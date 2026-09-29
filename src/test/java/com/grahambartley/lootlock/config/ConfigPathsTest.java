package com.grahambartley.lootlock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ConfigPathsTest {
  private static final Path WORLD_DIR = Path.of("saves", "world");
  private static final UUID PLAYER_UUID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

  static Stream<Arguments> resolvedPaths() {
    Function<ConfigPaths, Path> lootLockDir = ConfigPaths::getLootLockDir;
    Function<ConfigPaths, Path> playersDir = ConfigPaths::getPlayersDir;
    Function<ConfigPaths, Path> serverPolicy = ConfigPaths::getServerPolicyPath;
    Function<ConfigPaths, Path> playerData = paths -> paths.getPlayerDataPath(PLAYER_UUID);
    return Stream.of(
        Arguments.of("loot lock dir", lootLockDir, WORLD_DIR.resolve("lootlock")),
        Arguments.of("players dir", playersDir, WORLD_DIR.resolve("lootlock/players")),
        Arguments.of(
            "server policy", serverPolicy, WORLD_DIR.resolve("lootlock/server-policy.json")),
        Arguments.of(
            "player data",
            playerData,
            WORLD_DIR.resolve("lootlock/players/069a79f4-44e9-4726-a5be-fca90e38aaf5.json")));
  }

  @ParameterizedTest(name = "{0} -> {2}")
  @MethodSource("resolvedPaths")
  void pathsResolveUnderWorldDirectory(
      String label, Function<ConfigPaths, Path> accessor, Path expected) {
    assertEquals(expected, accessor.apply(new ConfigPaths(WORLD_DIR)));
  }

  @ParameterizedTest(name = "{0} backs up as {1}.broken.<timestamp>.json")
  @CsvSource({
    "player.json,      player",
    "server-policy.json, server-policy",
    "notes.txt,        notes.txt",
  })
  void brokenBackupSitsBesideOriginalWithTimestamp(String fileName, String expectedBase) {
    Path original = WORLD_DIR.resolve("lootlock").resolve(fileName);
    Instant before = Instant.now().minusSeconds(1);

    Path backup = new ConfigPaths(WORLD_DIR).getBrokenBackupPath(original);

    assertEquals(original.getParent(), backup.getParent());
    String backupName = backup.getFileName().toString();
    String prefix = expectedBase + ".broken.";
    assertTrue(backupName.startsWith(prefix), backupName);
    assertTrue(backupName.endsWith(".json"), backupName);
    String timestamp =
        backupName.substring(prefix.length(), backupName.length() - ".json".length());
    assertFalse(timestamp.contains(":"), timestamp);
    Instant parsed = Instant.parse(timestamp.replaceFirst("T(\\d+)-(\\d+)-", "T$1:$2:"));
    assertFalse(parsed.isBefore(before), parsed + " before " + before);
  }
}
