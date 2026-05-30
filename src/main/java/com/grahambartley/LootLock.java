package com.grahambartley;

import com.grahambartley.config.ConfigManager;
import com.grahambartley.server.ServerLifecycleHooks;
import com.grahambartley.server.ServerPlayerDataManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.WorldSavePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;

public class LootLock implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(LootLockConstants.MOD_ID);
	public static ServerPlayerDataManager PLAYER_DATA_MANAGER;

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(server -> {
			Path worldDir = server.getSavePath(WorldSavePath.ROOT).normalize();
			ConfigManager configManager = new ConfigManager(worldDir);
			PLAYER_DATA_MANAGER = new ServerPlayerDataManager(configManager);
			ServerLifecycleHooks.initialize(PLAYER_DATA_MANAGER);
			LOGGER.info("{} initialized (world: {})", LootLockConstants.MOD_NAME, worldDir);
		});
	}
}
