package com.grahambartley.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ConfigManagerTest {

  private static final String BACKUP_PREFIX = ".broken.";

  @TempDir Path tempDir;
  private ConfigManager manager;

  @BeforeEach
  void setUp() {
    manager = new ConfigManager(tempDir);
  }

  @Test
  void missingFileReturnsDefault() {
    UUID playerUuid = UUID.randomUUID();

    ConfigManager.LoadResult result = manager.loadPlayerData(playerUuid);

    assertTrue(result.createdDefault());
    LootLockPlayerData data = result.data();
    assertEquals(playerUuid, data.getPlayerUuid());
    assertEquals(1, data.getProfiles().size());
    assertEquals(0, data.getRevision());
  }

  @Test
  void savedDataReloadsWithIdenticalRevision() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setRevision(7);
    manager.savePlayerData(original);

    ConfigManager.LoadResult result = manager.loadPlayerData(playerUuid);

    assertFalse(result.createdDefault());
    LootLockPlayerData loaded = result.data();
    assertEquals(playerUuid, loaded.getPlayerUuid());
    assertEquals(7, loaded.getRevision());
    assertEquals(1, loaded.getProfiles().size());
  }

  @Test
  void corruptFileCreatesDefaultAndBackup() throws IOException {
    UUID playerUuid = UUID.randomUUID();
    Path dataPath = manager.getPaths().getPlayerDataPath(playerUuid);
    Files.createDirectories(dataPath.getParent());
    Files.writeString(dataPath, "garbage not json");

    ConfigManager.LoadResult result = manager.loadPlayerData(playerUuid);

    assertTrue(result.createdDefault());
    LootLockPlayerData loaded = result.data();
    assertEquals(playerUuid, loaded.getPlayerUuid());
    assertEquals(1, loaded.getProfiles().size());

    try (Stream<Path> listing = Files.list(manager.getPaths().getPlayersDir())) {
      assertTrue(
          listing.anyMatch(p -> p.getFileName().toString().contains(BACKUP_PREFIX)),
          "Expected a " + BACKUP_PREFIX + " backup file");
    }
  }

  @Test
  void saveAndReloadWithMultipleProfiles() {
    UUID playerUuid = UUID.randomUUID();
    LootLockProfile mining = newProfile("Mining", FilterMode.DENYLIST, "minecraft:cobblestone");
    LootLockProfile farming = newProfile("Farming", FilterMode.DENYLIST, "minecraft:wheat_seeds");

    LootLockPlayerData original = LootLockPlayerData.createDefault(playerUuid);
    original.setProfiles(List.of(mining, farming));
    manager.savePlayerData(original);

    LootLockPlayerData loaded = manager.loadPlayerData(playerUuid).data();

    assertEquals(2, loaded.getProfiles().size());
    assertEquals("Mining", loaded.getProfiles().get(0).getName());
    assertEquals("Farming", loaded.getProfiles().get(1).getName());
  }

  @Test
  void saveDoesNotLeakTempFiles() throws IOException {
    UUID playerUuid = UUID.randomUUID();
    manager.savePlayerData(LootLockPlayerData.createDefault(playerUuid));

    try (Stream<Path> listing = Files.list(manager.getPaths().getPlayersDir())) {
      assertEquals(
          0,
          listing.filter(p -> p.getFileName().toString().endsWith(".tmp")).count(),
          "No .tmp files should leak after save");
    }
  }

  @Test
  void loadRawJsonReturnsContentsAfterSave() {
    UUID playerUuid = UUID.randomUUID();
    manager.savePlayerData(LootLockPlayerData.createDefault(playerUuid));

    String rawJson = manager.loadRawJson(playerUuid).orElseThrow();
    assertFalse(rawJson.isBlank());
    assertTrue(rawJson.contains("schemaVersion"));
    assertTrue(rawJson.contains(playerUuid.toString()));
  }

  @Test
  void deletePlayerDataRemovesFile() {
    UUID playerUuid = UUID.randomUUID();
    manager.savePlayerData(LootLockPlayerData.createDefault(playerUuid));
    assertTrue(Files.exists(manager.getPaths().getPlayerDataPath(playerUuid)));

    assertTrue(manager.deletePlayerData(playerUuid));

    assertFalse(Files.exists(manager.getPaths().getPlayerDataPath(playerUuid)));
  }

  @Test
  void deleteNonExistentDataReturnsFalse() {
    assertFalse(manager.deletePlayerData(UUID.randomUUID()));
  }

  private static LootLockProfile newProfile(String name, FilterMode mode, String... ruleItemIds) {
    RuleEntry[] rules = new RuleEntry[ruleItemIds.length];
    for (int i = 0; i < ruleItemIds.length; i++) {
      rules[i] = new RuleEntry(ruleItemIds[i]);
    }
    return new LootLockProfile(
        UUID.randomUUID(), name, mode, RejectedItemAction.LEAVE_ON_GROUND, true, List.of(rules));
  }
}
