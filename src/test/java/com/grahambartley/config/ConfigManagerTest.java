package com.grahambartley.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigManagerTest {

  private static final String BACKUP_PREFIX = ".broken.";

  @TempDir Path tempDir;

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
    Files.writeString(dataPath, "garbage not json");

    LootLockPlayerData loaded = manager.loadPlayerData(playerUuid);

    assertEquals(playerUuid, loaded.getPlayerUuid());
    assertEquals(1, loaded.getProfiles().size());

    Path playersDir = manager.getPaths().getPlayersDir();
    boolean hasBrokenBackup;
    try (Stream<Path> listing = Files.list(playersDir)) {
      hasBrokenBackup = listing.anyMatch(p -> p.getFileName().toString().contains(BACKUP_PREFIX));
    }
    assertTrue(hasBrokenBackup, "Expected a " + BACKUP_PREFIX + " backup file");
  }

  @Test
  void saveAndReloadWithMultipleProfiles() throws IOException {
    ConfigManager manager = new ConfigManager(tempDir);
    UUID playerUuid = UUID.randomUUID();

    LootLockProfile mining =
        new LootLockProfile(
            UUID.randomUUID(),
            "Mining",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:cobblestone")));
    LootLockProfile farming =
        new LootLockProfile(
            UUID.randomUUID(),
            "Farming",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:wheat_seeds")));

    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setProfiles(List.of(mining, farming));
    manager.savePlayerData(original);

    LootLockPlayerData loaded = manager.loadPlayerData(playerUuid);

    assertEquals(2, loaded.getProfiles().size());
    assertEquals("Mining", loaded.getProfiles().get(0).getName());
    assertEquals("Farming", loaded.getProfiles().get(1).getName());
  }

  @Test
  void saveDoesNotLeakTempFiles() throws IOException {
    ConfigManager manager = new ConfigManager(tempDir);
    UUID playerUuid = UUID.randomUUID();

    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    manager.savePlayerData(data);

    Path playersDir = manager.getPaths().getPlayersDir();
    try (Stream<Path> listing = Files.list(playersDir)) {
      long tmpCount = listing.filter(p -> p.getFileName().toString().endsWith(".tmp")).count();
      assertEquals(0, tmpCount, "No .tmp files should leak after save");
    }
  }

  @Test
  void saveProducesValidJson() throws IOException {
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
