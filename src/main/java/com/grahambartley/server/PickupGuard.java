package com.grahambartley.server;

import com.grahambartley.LootLock;
import com.grahambartley.api.PickupDecision;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ServerToClientPackets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PickupGuard {
  private static final Logger LOGGER = LoggerFactory.getLogger(PickupGuard.class);
  private static final long NOTIFICATION_COOLDOWN_TICKS = 40;

  private final ServerPlayerDataManager playerDataManager;
  private final Map<UUID, Long> lastNotificationTick = new HashMap<>();
  private final Map<UUID, BlockedNotificationAccumulator> blockedAccumulators = new HashMap<>();

  public PickupGuard(ServerPlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
  }

  public PickupDecision evaluate(ServerPlayerEntity player, ItemStack stack) {
    Identifier itemId = Registries.ITEM.getId(stack.getItem());
    return evaluate(player.getUuid(), itemId);
  }

  public PickupDecision evaluate(UUID playerUuid, Identifier itemId) {
    LootLockPlayerData playerData = playerDataManager.getOrLoad(playerUuid);
    LootLockProfile activeProfile = playerData.getActiveProfile().orElse(null);

    if (activeProfile == null || !activeProfile.isEnabled()) {
      return PickupDecision.ALLOW;
    }

    PickupDecision decision = activeProfile.evaluate(itemId);
    if (!LootLock.SERVER_CONFIG.allowDeleteRejectedItems()
        && decision == PickupDecision.REJECT_DELETE) {
      return PickupDecision.REJECT_LEAVE;
    }
    return decision;
  }

  public boolean tryNotify(
      ServerPlayerEntity player, ItemStack stack, boolean deleted, long currentTick) {
    if (stack == null) {
      return false;
    }

    List<BlockedNotice> notices =
        recordBlockedCollision(
            player.getUuid(),
            Registries.ITEM.getId(stack.getItem()),
            stack.getCount(),
            deleted,
            currentTick);

    if (notices.isEmpty()) {
      return false;
    }

    for (BlockedNotice notice : notices) {
      if (!ServerToClientPackets.sendBlockedNotice(
          player, notice.itemId(), notice.count(), notice.deleted())) {
        String verb = notice.deleted() ? "Deleted" : "Blocked";
        String itemName = stack.getItem().getName().getString();
        player.sendMessage(Text.literal(String.format("[LootLock] %s %s", verb, itemName)), true);
      }
      LOGGER.debug(
          "{} {}x{} for player {}",
          notice.deleted() ? "Deleted" : "Blocked",
          notice.count(),
          notice.itemId(),
          player.getUuid());
    }

    return true;
  }

  public boolean tryNotify(UUID playerUuid, ItemStack stack, boolean deleted, long currentTick) {
    if (stack == null) {
      return false;
    }

    Identifier itemId = Registries.ITEM.getId(stack.getItem());
    return !recordBlockedCollision(playerUuid, itemId, stack.getCount(), deleted, currentTick)
        .isEmpty();
  }

  List<BlockedNotice> recordBlockedCollision(
      UUID playerUuid, Identifier itemId, int count, boolean deleted, long currentTick) {
    Long lastTick = lastNotificationTick.get(playerUuid);
    BlockedNotificationAccumulator accumulator =
        blockedAccumulators.computeIfAbsent(
            playerUuid, ignored -> new BlockedNotificationAccumulator());
    accumulator.accumulate(itemId, Math.max(1, count), deleted);

    if (lastTick != null && (currentTick - lastTick) < NOTIFICATION_COOLDOWN_TICKS) {
      return List.of();
    }

    lastNotificationTick.put(playerUuid, currentTick);
    return accumulator.drain();
  }

  void clearNotificationCooldown(UUID playerUuid) {
    lastNotificationTick.remove(playerUuid);
    blockedAccumulators.remove(playerUuid);
  }

  boolean hasNotificationCooldown(UUID playerUuid) {
    return lastNotificationTick.containsKey(playerUuid);
  }

  Long getNotificationCooldownTick(UUID playerUuid) {
    return lastNotificationTick.get(playerUuid);
  }

  void stampNotificationCooldown(UUID playerUuid, long currentTick) {
    lastNotificationTick.put(playerUuid, currentTick);
  }

  record BlockedNotice(Identifier itemId, int count, boolean deleted) {}

  record BlockedNoticeKey(Identifier itemId, boolean deleted) {}

  private static final class BlockedNotificationAccumulator {
    private final Map<BlockedNoticeKey, BlockedNotice> pending = new LinkedHashMap<>();

    void accumulate(Identifier itemId, int count, boolean deleted) {
      BlockedNoticeKey key = new BlockedNoticeKey(itemId, deleted);
      BlockedNotice existing = pending.get(key);
      if (existing == null) {
        pending.put(key, new BlockedNotice(itemId, count, deleted));
        return;
      }
      pending.put(key, new BlockedNotice(itemId, existing.count() + count, deleted));
    }

    List<BlockedNotice> drain() {
      if (pending.isEmpty()) {
        return List.of();
      }
      List<BlockedNotice> drained = new ArrayList<>(pending.values());
      pending.clear();
      return drained;
    }
  }
}
