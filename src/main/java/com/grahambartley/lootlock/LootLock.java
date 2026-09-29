package com.grahambartley.lootlock;

import com.grahambartley.lootlock.command.LootLockCommand;
import com.grahambartley.lootlock.config.LootLockConfig;
import com.grahambartley.lootlock.network.LootLockNetworking;
import com.grahambartley.lootlock.server.PickupGuard;
import com.grahambartley.lootlock.server.ServerLifecycleHooks;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.server.WorldSession;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.CommonLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LootLock implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger(LootLockConstants.MOD_ID);
  public static volatile ServerPlayerDataManager PLAYER_DATA_MANAGER;
  public static volatile PickupGuard PICKUP_GUARD;
  public static volatile LootLockConfig SERVER_CONFIG = LootLockConfig.defaults();

  @Override
  public void onInitialize() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> LootLockCommand.register(dispatcher));
    LootLockNetworking.initializeNetworking();

    ServerLifecycleHooks.register();
    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          Path worldDir = server.getSavePath(WorldSavePath.ROOT).normalize();
          WorldSession.ensureOpen(worldDir);
          LOGGER.info("{} initialized (world: {})", LootLockConstants.MOD_NAME, worldDir);
        });
    ServerLifecycleEvents.SERVER_STOPPED.register(server -> WorldSession.close());

    CommonLifecycleEvents.TAGS_LOADED.register(
        (registries, client) -> {
          if (client) {
            return;
          }
          if (PLAYER_DATA_MANAGER != null) {
            PLAYER_DATA_MANAGER.recompileAllProfiles();
          }
        });
  }
}
