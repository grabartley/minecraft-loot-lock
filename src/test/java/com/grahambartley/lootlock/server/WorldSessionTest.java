package com.grahambartley.lootlock.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.config.ConfigPaths;
import com.grahambartley.lootlock.config.LootLockConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WorldSessionTest {
  @TempDir Path saves;

  @AfterEach
  void closeSession() {
    WorldSession.close();
  }

  @Test
  void openingAWorldCreatesItsManagerAndGuard() {
    WorldSession.open(saves.resolve("first"));

    assertNotNull(LootLock.PLAYER_DATA_MANAGER);
    assertNotNull(LootLock.PICKUP_GUARD);
    assertTrue(WorldSession.isOpenFor(saves.resolve("first")));
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void openingAWorldLoadsItsServerPolicy(boolean allowDelete) {
    Path worldDir = saves.resolve("world");
    LootLockConfig.save(
        new ConfigPaths(worldDir).getServerPolicyPath(), new LootLockConfig(allowDelete));

    WorldSession.open(worldDir);

    assertEquals(allowDelete, LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
  }

  @Test
  void closingClearsEveryPerWorldStatic() {
    Path worldDir = saves.resolve("world");
    LootLockConfig.save(new ConfigPaths(worldDir).getServerPolicyPath(), new LootLockConfig(false));
    WorldSession.open(worldDir);

    WorldSession.close();

    assertNull(LootLock.PLAYER_DATA_MANAGER);
    assertNull(LootLock.PICKUP_GUARD);
    assertTrue(LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
    assertFalse(WorldSession.isOpenFor(worldDir));
  }

  @Test
  void aSecondWorldOnlyTouchesItsOwnFolder() {
    Path firstWorld = saves.resolve("first");
    Path secondWorld = saves.resolve("second");
    WorldSession.open(firstWorld);
    ServerPlayerDataManager firstManager = LootLock.PLAYER_DATA_MANAGER;
    WorldSession.close();
    WorldSession.open(secondWorld);
    UUID playerUuid = UUID.randomUUID();

    LootLock.PLAYER_DATA_MANAGER.getOrLoad(playerUuid);
    LootLock.PLAYER_DATA_MANAGER.flushAll();

    assertNotSame(firstManager, LootLock.PLAYER_DATA_MANAGER);
    assertTrue(Files.exists(new ConfigPaths(secondWorld).getPlayerDataPath(playerUuid)));
    assertFalse(Files.exists(new ConfigPaths(firstWorld).getPlayerDataPath(playerUuid)));
    assertFalse(WorldSession.isOpenFor(firstWorld));
  }

  @Test
  void isOpenForComparesNormalizedPaths() {
    WorldSession.open(saves.resolve("world"));

    assertTrue(WorldSession.isOpenFor(saves.resolve("other/../world")));
  }
}
