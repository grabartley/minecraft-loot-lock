package com.grahambartley.lootlock.config;

import java.nio.file.Path;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class ConfigPaths {
  private static final String LOOTLOCK_DIR = "lootlock";
  private static final String PLAYERS_DIR = "players";
  private static final String JSON_EXT = ".json";
  private static final String BROKEN_PREFIX = ".broken.";

  private final Path lootLockDir;

  public ConfigPaths(Path worldDir) {
    this.lootLockDir = worldDir.resolve(LOOTLOCK_DIR);
  }

  public Path getLootLockDir() {
    return lootLockDir;
  }

  public Path getPlayersDir() {
    return lootLockDir.resolve(PLAYERS_DIR);
  }

  public Path getServerPolicyPath() {
    return lootLockDir.resolve("server-policy.json");
  }

  public Path getPlayerDataPath(UUID playerUuid) {
    return getPlayersDir().resolve(playerUuid.toString() + JSON_EXT);
  }

  public Path getBrokenBackupPath(Path originalFile) {
    String fileName = originalFile.getFileName().toString();
    String baseName =
        fileName.endsWith(JSON_EXT)
            ? fileName.substring(0, fileName.length() - JSON_EXT.length())
            : fileName;
    String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
    return originalFile.resolveSibling(baseName + BROKEN_PREFIX + timestamp + JSON_EXT);
  }
}
