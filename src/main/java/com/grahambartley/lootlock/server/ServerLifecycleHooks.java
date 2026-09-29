package com.grahambartley.lootlock.server;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerLifecycleHooks {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerLifecycleHooks.class);
  private static boolean registered;

  private ServerLifecycleHooks() {}

  public static synchronized void register() {
    if (registered) {
      return;
    }
    registered = true;
    ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> onJoin(handler.player));
    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> onDisconnect(handler.player.getUuid()));
    ServerLifecycleEvents.SERVER_STOPPING.register(server -> onStopping());
    ServerTickEvents.END_SERVER_TICK.register(ServerLifecycleHooks::onTick);
  }

  private static void onJoin(ServerPlayerEntity player) {
    ServerPlayerDataManager playerDataManager = LootLock.PLAYER_DATA_MANAGER;
    if (playerDataManager == null) {
      return;
    }
    playerDataManager.get(player);
    ServerToClientPackets.sendServerCapabilities(player);
    LOGGER.debug("Player data initialized for {} on join", player.getUuid());
  }

  private static void onDisconnect(UUID playerUuid) {
    ServerPlayerDataManager playerDataManager = LootLock.PLAYER_DATA_MANAGER;
    if (playerDataManager != null) {
      playerDataManager.saveOnDisconnect(playerUuid);
    }
    PickupGuard pickupGuard = LootLock.PICKUP_GUARD;
    if (pickupGuard != null) {
      pickupGuard.clearNotificationCooldown(playerUuid);
    }
    LOGGER.debug("Player data saved and cache cleared for {} on disconnect", playerUuid);
  }

  private static void onStopping() {
    ServerPlayerDataManager playerDataManager = LootLock.PLAYER_DATA_MANAGER;
    if (playerDataManager == null) {
      return;
    }
    int flushed = playerDataManager.flushAll();
    LOGGER.info("Flushed player data for {} player(s) on server shutdown", flushed);
  }

  private static void onTick(MinecraftServer server) {
    ServerPlayerDataManager playerDataManager = LootLock.PLAYER_DATA_MANAGER;
    if (playerDataManager != null) {
      playerDataManager.tick(server);
    }
  }
}
