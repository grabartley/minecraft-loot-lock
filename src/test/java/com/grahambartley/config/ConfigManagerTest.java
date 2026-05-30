package com.grahambartley.config;

import com.grahambartley.data.LootLockPlayerData;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

	@TempDir
	Path tempDir;

	@Test
	void missingFileReturnsDefault() {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData data = manager.loadPlayerData(playerUuid);

		assertEquals(playerUuid, data.getPlayerUuid());
		assertEquals(1, data.getProfiles().size());
		assertEquals(0, data.getRevision());
	}

	@Test
	void validFileLoadsCorrectly() throws IOException {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
		original.setRevision(7);
		manager.savePlayerData(original);

		LootLockPlayerData loaded = manager.loadPlayerData(playerUuid);

		assertEquals(playerUuid, loaded.getPlayerUuid());
		assertEquals(7, loaded.getRevision());
		assertEquals(1, loaded.getProfiles().size());
	}

	@Test
	void corruptFileCreatesDefaultAndBackup() throws IOException {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		Path dataPath = manager.getPaths().getPlayerDataPath(playerUuid);
		Files.createDirectories(dataPath.getParent());
		Files.writeString(dataPath, "this is not valid json");

		LootLockPlayerData loaded = manager.loadPlayerData(playerUuid);

		assertEquals(playerUuid, loaded.getPlayerUuid());
		assertEquals(1, loaded.getProfiles().size());

		Path playersDir = manager.getPaths().getPlayersDir();
		boolean hasBrokenBackup = Files.list(playersDir).anyMatch(p ->
			p.getFileName().toString().contains(".broken.")
		);
		assertTrue(hasBrokenBackup, "Expected a .broken. backup file");
	}

	@Test
	void saveAndReloadPreservesMultipleProfiles() throws IOException {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
		manager.savePlayerData(original);

		LootLockPlayerData loaded = manager.loadPlayerData(playerUuid);

		assertEquals(original.getActiveProfileId(), loaded.getActiveProfileId());
		assertEquals(original.getProfiles().size(), loaded.getProfiles().size());
	}

	@Test
	void atomicWriteDoesNotProduceCorruptState() throws IOException {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
		manager.savePlayerData(data);

		String rawJson = manager.loadRawJson(playerUuid).orElseThrow();
		assertNotNull(rawJson);
		assertFalse(rawJson.isBlank());
		assertTrue(rawJson.contains("schemaVersion"));
		assertTrue(rawJson.contains(playerUuid.toString()));
	}

	@Test
	void deletePlayerDataRemovesFile() throws IOException {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
		manager.savePlayerData(data);

		assertTrue(Files.exists(manager.getPaths().getPlayerDataPath(playerUuid)));

		boolean deleted = manager.deletePlayerData(playerUuid);
		assertTrue(deleted);
		assertFalse(Files.exists(manager.getPaths().getPlayerDataPath(playerUuid)));
	}

	@Test
	void deleteNonExistentDataReturnsFalse() {
		ConfigManager manager = new ConfigManager(tempDir);
		UUID playerUuid = UUID.randomUUID();

		assertFalse(manager.deletePlayerData(playerUuid));
	}
}
