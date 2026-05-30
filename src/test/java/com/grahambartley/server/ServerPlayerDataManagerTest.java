package com.grahambartley.server;

import com.grahambartley.config.ConfigManager;
import com.grahambartley.data.LootLockPlayerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerPlayerDataManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void getOrLoadReturnsDataForUuid() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData data = manager.getOrLoad(playerUuid);

		assertNotNull(data);
		assertEquals(playerUuid, data.getPlayerUuid());
		assertEquals(1, data.getProfiles().size());
	}

	@Test
	void getOrLoadCachesSubsequentCalls() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData first = manager.getOrLoad(playerUuid);
		LootLockPlayerData second = manager.getOrLoad(playerUuid);

		assertSame(first, second);
		assertEquals(1, manager.getCacheSize());
	}

	@Test
	void getOrLoadLoadsFromDiskOnCacheMiss() {
		ConfigManager configManager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
		original.setRevision(42);
		configManager.savePlayerData(original);

		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		LootLockPlayerData loaded = manager.getOrLoad(playerUuid);

		assertEquals(42, loaded.getRevision());
	}

	@Test
	void markDirtySetsDirtyFlag() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		manager.getOrLoad(playerUuid);
		assertFalse(manager.isDirty(playerUuid));

		manager.markDirty(playerUuid, 100);

		assertTrue(manager.isDirty(playerUuid));
	}

	@Test
	void markDirtyRecordsCurrentTick() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		manager.getOrLoad(playerUuid);
		manager.markDirty(playerUuid, 50);

		assertEquals(50, manager.getDirtyTick(playerUuid));
	}

	@Test
	void markDirtyIncrementsRevision() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData data = manager.getOrLoad(playerUuid);
		long initialRevision = data.getRevision();

		manager.markDirty(playerUuid, 50);

		assertEquals(initialRevision + 1, data.getRevision());
	}

	@Test
	void saveOnDisconnectSavesDirtyDataAndRemovesFromCache() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		manager.getOrLoad(playerUuid);
		manager.markDirty(playerUuid, 10);
		assertEquals(1, manager.getCacheSize());

		manager.saveOnDisconnect(playerUuid);

		assertEquals(0, manager.getCacheSize());
		LootLockPlayerData reloaded = configManager.loadPlayerData(playerUuid);
		assertEquals(1, reloaded.getRevision());
	}

	@Test
	void saveOnDisconnectDoesNotSaveNonDirtyData() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		manager.getOrLoad(playerUuid);
		manager.saveOnDisconnect(playerUuid);

		assertEquals(0, manager.getCacheSize());
	}

	@Test
	void tickDoesNotSaveBeforeDebounceThreshold() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		manager.getOrLoad(playerUuid);
		manager.markDirty(playerUuid, 0);

		manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS - 1);

		assertTrue(manager.isDirty(playerUuid));
	}

	@Test
	void tickSavesExactlyAtDebounceThreshold() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		manager.getOrLoad(playerUuid);
		manager.markDirty(playerUuid, 0);

		manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS);

		assertFalse(manager.isDirty(playerUuid));
	}

	@Test
	void tickOnlySavesExactlyDebouncedEntries() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid1 = UUID.randomUUID();
		UUID playerUuid2 = UUID.randomUUID();

		manager.getOrLoad(playerUuid1);
		manager.getOrLoad(playerUuid2);

		manager.markDirty(playerUuid1, 0);
		manager.markDirty(playerUuid2, 39);

		manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS);

		assertFalse(manager.isDirty(playerUuid1));
		assertTrue(manager.isDirty(playerUuid2));
	}

	@Test
	void flushAllSavesAllDirtyEntries() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid1 = UUID.randomUUID();
		UUID playerUuid2 = UUID.randomUUID();

		manager.getOrLoad(playerUuid1);
		manager.getOrLoad(playerUuid2);

		manager.markDirty(playerUuid1, 0);
		manager.markDirty(playerUuid2, 0);

		manager.flushAll();

		assertFalse(manager.isDirty(playerUuid1));
		assertFalse(manager.isDirty(playerUuid2));
	}

	@Test
	void flushAllOnlySavesDirtyEntries() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID dirtyUuid = UUID.randomUUID();
		UUID cleanUuid = UUID.randomUUID();

		manager.getOrLoad(dirtyUuid);
		manager.getOrLoad(cleanUuid);
		manager.markDirty(dirtyUuid, 0);

		int saved = manager.flushAll();

		assertFalse(manager.isDirty(dirtyUuid));
		assertFalse(manager.isDirty(cleanUuid));
		assertEquals(1, saved);
	}

	@Test
	void markDirtyOnCacheMissIsNoOp() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid = UUID.randomUUID();

		assertEquals(0, manager.getCacheSize());
		manager.markDirty(playerUuid, 100);

		assertEquals(0, manager.getCacheSize());
		assertFalse(manager.isDirty(playerUuid));
	}

	@Test
	void getCacheSizeReflectsLoadedPlayers() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);

		assertEquals(0, manager.getCacheSize());

		manager.getOrLoad(UUID.randomUUID());
		assertEquals(1, manager.getCacheSize());

		manager.getOrLoad(UUID.randomUUID());
		assertEquals(2, manager.getCacheSize());
	}

	@Test
	void multiplePlayersAreIsolated() {
		ConfigManager configManager = new ConfigManager(tempDir);
		ServerPlayerDataManager manager = new ServerPlayerDataManager(configManager);
		UUID playerUuid1 = UUID.randomUUID();
		UUID playerUuid2 = UUID.randomUUID();

		LootLockPlayerData data1 = manager.getOrLoad(playerUuid1);
		LootLockPlayerData data2 = manager.getOrLoad(playerUuid2);

		assertEquals(playerUuid1, data1.getPlayerUuid());
		assertEquals(playerUuid2, data2.getPlayerUuid());
		assertEquals(2, manager.getCacheSize());
	}
}
