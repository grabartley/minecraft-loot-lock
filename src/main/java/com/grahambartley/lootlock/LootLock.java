package com.grahambartley.lootlock;

import com.grahambartley.lootlock.command.LootLockCommand;
import com.grahambartley.lootlock.config.ConfigManager;
import com.grahambartley.lootlock.config.LootLockConfig;
import com.grahambartley.lootlock.network.LootLockNetworking;
import com.grahambartley.lootlock.server.PickupGuard;
import com.grahambartley.lootlock.server.ServerLifecycleHooks;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
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
  public static ServerPlayerDataManager PLAYER_DATA_MANAGER;
  public static PickupGuard PICKUP_GUARD;
  // Updated at startup and mutated at runtime by server policy command and GUI paths.
  public static LootLockConfig SERVER_CONFIG = LootLockConfig.defaults();

  @Override
  public void onInitialize() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> LootLockCommand.register(dispatcher));
    LootLockNetworking.initializeNetworking();

    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          Path worldDir = server.getSavePath(WorldSavePath.ROOT).normalize();
          ConfigManager configManager = new ConfigManager(worldDir);
          SERVER_CONFIG = LootLockConfig.load(configManager.getPaths().getServerPolicyPath());
          PLAYER_DATA_MANAGER = new ServerPlayerDataManager(configManager);
          PICKUP_GUARD = new PickupGuard(PLAYER_DATA_MANAGER);
          ServerLifecycleHooks.initialize(PLAYER_DATA_MANAGER, PICKUP_GUARD);
          LOGGER.info("{} initialized (world: {})", LootLockConstants.MOD_NAME, worldDir);
        });

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
