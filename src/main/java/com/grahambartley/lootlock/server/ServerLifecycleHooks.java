package com.grahambartley.lootlock.server;

import com.grahambartley.lootlock.network.ServerToClientPackets;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerLifecycleHooks {
  private static final Logger LOGGER = LoggerFactory.getLogger(ServerLifecycleHooks.class);

  private ServerLifecycleHooks() {}

  public static void initialize(
      ServerPlayerDataManager playerDataManager, PickupGuard pickupGuard) {
    ServerPlayConnectionEvents.JOIN.register(
        (handler, sender, server) -> {
          UUID playerUuid = handler.player.getUuid();
          playerDataManager.get(handler.player);
          ServerToClientPackets.sendServerCapabilities(handler.player);
          LOGGER.debug("Player data initialized for {} on join", playerUuid);
        });

    ServerPlayConnectionEvents.DISCONNECT.register(
        (handler, server) -> {
          UUID playerUuid = handler.player.getUuid();
          playerDataManager.saveOnDisconnect(playerUuid);
          pickupGuard.clearNotificationCooldown(playerUuid);
          LOGGER.debug("Player data saved and cache cleared for {} on disconnect", playerUuid);
        });

    ServerLifecycleEvents.SERVER_STOPPING.register(
        server -> {
          int flushed = playerDataManager.flushAll();
          LOGGER.info("Flushed player data for {} player(s) on server shutdown", flushed);
        });

    ServerTickEvents.END_SERVER_TICK.register(
        server -> {
          playerDataManager.tick(server);
        });
  }
}
