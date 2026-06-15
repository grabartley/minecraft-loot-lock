package com.grahambartley.config;

import com.grahambartley.config.ConfigSerializer.ConfigDeserializationException;
import com.grahambartley.data.LootLockPlayerData;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ConfigManager {
  private static final Logger LOGGER = LoggerFactory.getLogger(ConfigManager.class);

  private final ConfigPaths paths;

  public ConfigManager(Path worldDir) {
    this.paths = new ConfigPaths(worldDir);
    ensureDirectories();
  }

  private void ensureDirectories() {
    try {
      Files.createDirectories(paths.getPlayersDir());
    } catch (IOException e) {
      LOGGER.error("Failed to create lootlock player data directory: {}", paths.getPlayersDir(), e);
    }
  }

  public LoadResult loadPlayerData(UUID playerUuid) {
    Path dataFile = paths.getPlayerDataPath(playerUuid);

    if (!Files.exists(dataFile)) {
      return new LoadResult(LootLockPlayerData.createDefault(playerUuid), true);
    }

    try {
      String json = Files.readString(dataFile);
      return new LoadResult(deserializeAndMigrate(json, playerUuid), false);
    } catch (ConfigDeserializationException e) {
      LOGGER.warn("Failed to deserialize player data for {}: {}", playerUuid, e.getMessage());
      backupCorruptFile(dataFile, playerUuid);
      return new LoadResult(LootLockPlayerData.createDefault(playerUuid), true);
    } catch (IOException e) {
      LOGGER.warn("Failed to read player data file for {}: {}", playerUuid, e.getMessage());
      backupCorruptFile(dataFile, playerUuid);
      return new LoadResult(LootLockPlayerData.createDefault(playerUuid), true);
    }
  }

  public record LoadResult(LootLockPlayerData data, boolean createdDefault) {}

  public void savePlayerData(LootLockPlayerData data) {
    Path dataFile = paths.getPlayerDataPath(data.getPlayerUuid());
    savePlayerDataToFile(data, dataFile);
  }

  public void savePlayerData(LootLockPlayerData data, Path targetPath) {
    savePlayerDataToFile(data, targetPath);
  }

  private void savePlayerDataToFile(LootLockPlayerData data, Path dataFile) {
    try {
      Files.createDirectories(dataFile.getParent());
      String json = ConfigSerializer.serialize(data);
      Path tempFile = dataFile.resolveSibling(dataFile.getFileName().toString() + ".tmp");
      Files.writeString(tempFile, json);
      Files.move(
          tempFile, dataFile, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      LOGGER.error("Failed to save player data for {}: {}", data.getPlayerUuid(), e.getMessage());
    }
  }

  public Optional<String> loadRawJson(UUID playerUuid) {
    Path dataFile = paths.getPlayerDataPath(playerUuid);
    if (!Files.exists(dataFile)) {
      return Optional.empty();
    }
    try {
      return Optional.of(Files.readString(dataFile));
    } catch (IOException e) {
      LOGGER.warn("Failed to read raw player data file for {}: {}", playerUuid, e.getMessage());
      return Optional.empty();
    }
  }

  public boolean deletePlayerData(UUID playerUuid) {
    Path dataFile = paths.getPlayerDataPath(playerUuid);
    try {
      return Files.deleteIfExists(dataFile);
    } catch (IOException e) {
      LOGGER.warn("Failed to delete player data for {}: {}", playerUuid, e.getMessage());
      return false;
    }
  }

  private LootLockPlayerData deserializeAndMigrate(String json, UUID expectedPlayerUuid)
      throws ConfigDeserializationException {
    LootLockPlayerData data = ConfigSerializer.deserialize(json, expectedPlayerUuid);

    ConfigValidationResult migrationResult = ConfigMigration.run(data);
    if (!migrationResult.valid() && !migrationResult.errors().isEmpty()) {
      String firstError = migrationResult.errors().get(0);
      if (firstError.startsWith("Unknown schema version")) {
        throw new ConfigDeserializationException(firstError);
      }
    }

    return data;
  }

  private void backupCorruptFile(Path dataFile, UUID playerUuid) {
    try {
      Path backupPath = paths.getBrokenBackupPath(dataFile);
      Files.move(dataFile, backupPath, StandardCopyOption.REPLACE_EXISTING);
      LOGGER.info("Backed up corrupt player data file for {} to {}", playerUuid, backupPath);
    } catch (IOException e) {
      LOGGER.error("Failed to back up corrupt file for {}: {}", playerUuid, e.getMessage());
    }
  }

  public ConfigPaths getPaths() {
    return paths;
  }
}
