package com.grahambartley;

import com.grahambartley.command.LootLockCommand;
import com.grahambartley.config.ConfigManager;
import com.grahambartley.network.LootLockNetworking;
import com.grahambartley.server.PickupGuard;
import com.grahambartley.server.ServerLifecycleHooks;
import com.grahambartley.server.ServerPlayerDataManager;
import java.nio.file.Path;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LootLock implements ModInitializer {
  public static final Logger LOGGER = LoggerFactory.getLogger(LootLockConstants.MOD_ID);
  public static ServerPlayerDataManager PLAYER_DATA_MANAGER;
  public static PickupGuard PICKUP_GUARD;

  @Override
  public void onInitialize() {
    CommandRegistrationCallback.EVENT.register(
        (dispatcher, registryAccess, environment) -> LootLockCommand.register(dispatcher));
    LootLockNetworking.initializeServer();

    ServerLifecycleEvents.SERVER_STARTED.register(
        server -> {
          Path worldDir = server.getSavePath(WorldSavePath.ROOT).normalize();
          ConfigManager configManager = new ConfigManager(worldDir);
          PLAYER_DATA_MANAGER = new ServerPlayerDataManager(configManager);
          PICKUP_GUARD = new PickupGuard(PLAYER_DATA_MANAGER);
          ServerLifecycleHooks.initialize(PLAYER_DATA_MANAGER, PICKUP_GUARD);
          LOGGER.info("{} initialized (world: {})", LootLockConstants.MOD_NAME, worldDir);
        });
  }
}
