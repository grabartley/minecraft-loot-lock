package com.grahambartley;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LootLock implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger(LootLockConstants.MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("{} initialized", LootLockConstants.MOD_NAME);
	}
}
