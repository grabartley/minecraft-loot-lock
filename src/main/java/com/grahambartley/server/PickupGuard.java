package com.grahambartley.server;

import com.grahambartley.api.PickupDecision;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PickupGuard {
  private static final Logger LOGGER = LoggerFactory.getLogger(PickupGuard.class);
  private static final long NOTIFICATION_COOLDOWN_TICKS = 40;

  private final ServerPlayerDataManager playerDataManager;
  private final Map<UUID, Long> lastNotificationTick = new HashMap<>();

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

    return activeProfile.evaluate(itemId);
  }

  public boolean tryNotify(
      ServerPlayerEntity player, ItemStack stack, boolean deleted, long currentTick) {
    return tryNotify(player.getUuid(), stack, deleted, currentTick, player);
  }

  public boolean tryNotify(UUID playerUuid, ItemStack stack, boolean deleted, long currentTick) {
    return tryNotify(playerUuid, stack, deleted, currentTick, null);
  }

  private boolean tryNotify(
      UUID playerUuid,
      ItemStack stack,
      boolean deleted,
      long currentTick,
      @Nullable ServerPlayerEntity player) {
    Long lastTick = lastNotificationTick.get(playerUuid);

    if (lastTick != null && (currentTick - lastTick) < NOTIFICATION_COOLDOWN_TICKS) {
      return false;
    }

    if (player != null) {
      String message =
          deleted
              ? String.format(
                  "§7[LootLock] §cDeleted %dx %s",
                  stack.getCount(), stack.getItem().getName().getString())
              : String.format(
                  "§7[LootLock] §eBlocked %dx %s",
                  stack.getCount(), stack.getItem().getName().getString());

      player.sendMessage(Text.literal(message), true);
      LOGGER.debug("{} for player {}", message, playerUuid);
      lastNotificationTick.put(playerUuid, currentTick);
    }

    return true;
  }

  void clearNotificationCooldown(UUID playerUuid) {
    lastNotificationTick.remove(playerUuid);
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
}
