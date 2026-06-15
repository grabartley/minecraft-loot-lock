package com.grahambartley.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.config.ConfigManager;
import com.grahambartley.data.LootLockPlayerData;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ServerPlayerDataManagerTest {

  @Mock private ConfigManager configManager;

  private ServerPlayerDataManager manager;
  private UUID playerUuid;

  @BeforeEach
  void setUp() {
    manager = new ServerPlayerDataManager(configManager);
    playerUuid = UUID.randomUUID();
  }

  private ConfigManager.LoadResult loadedFromDisk(LootLockPlayerData data) {
    return new ConfigManager.LoadResult(data, false);
  }

  private ConfigManager.LoadResult createdDefault(LootLockPlayerData data) {
    return new ConfigManager.LoadResult(data, true);
  }

  @Test
  void getOrLoadDelegatesToConfigManagerOnCacheMiss() {
    LootLockPlayerData stored = LootLockPlayerData.createDefault(playerUuid);
    stored.setRevision(42);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(stored));

    LootLockPlayerData loaded = manager.getOrLoad(playerUuid);

    assertSame(stored, loaded);
    assertEquals(42, loaded.getRevision());
    verify(configManager, times(1)).loadPlayerData(playerUuid);
  }

  @Test
  void getOrLoadCachesSubsequentCalls() {
    when(configManager.loadPlayerData(playerUuid))
        .thenReturn(loadedFromDisk(LootLockPlayerData.createDefault(playerUuid)));

    LootLockPlayerData first = manager.getOrLoad(playerUuid);
    LootLockPlayerData second = manager.getOrLoad(playerUuid);

    assertSame(first, second);
    verify(configManager, times(1)).loadPlayerData(playerUuid);
  }

  @Test
  void getOrLoadLeavesExistingPlayerCleanOnLoad() {
    when(configManager.loadPlayerData(playerUuid))
        .thenReturn(loadedFromDisk(LootLockPlayerData.createDefault(playerUuid)));

    manager.getOrLoad(playerUuid);

    assertFalse(manager.isDirty(playerUuid));
  }

  @Test
  void getOrLoadMarksFirstTimePlayerDirtySoDefaultPersists() {
    when(configManager.loadPlayerData(playerUuid))
        .thenReturn(createdDefault(LootLockPlayerData.createDefault(playerUuid)));

    manager.getOrLoad(playerUuid);

    assertTrue(manager.isDirty(playerUuid));
  }

  @Test
  void existingPlayerTickDoesNotWriteWithinDebounceWindow() {
    when(configManager.loadPlayerData(playerUuid))
        .thenReturn(loadedFromDisk(LootLockPlayerData.createDefault(playerUuid)));
    manager.getOrLoad(playerUuid);

    manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS + 100);

    verify(configManager, never()).savePlayerData(any());
  }

  @Test
  void firstTimePlayerDebounceTickWritesDefault() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(createdDefault(data));
    manager.getOrLoad(playerUuid);

    manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS);

    assertFalse(manager.isDirty(playerUuid));
    verify(configManager).savePlayerData(data);
  }

  @Test
  void markDirtyOnCacheMissIsNoOp() {
    manager.markDirty(playerUuid, 100);

    assertFalse(manager.isDirty(playerUuid));
    verify(configManager, never()).savePlayerData(any());
  }

  @Test
  void markDirtyIncrementsRevisionAndRecordsTick() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    long initialRevision = data.getRevision();
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data));
    manager.getOrLoad(playerUuid);

    manager.markDirty(playerUuid, 50);

    assertTrue(manager.isDirty(playerUuid));
    assertEquals(initialRevision + 1, data.getRevision());
    assertEquals(50, manager.getDirtyTick(playerUuid));
  }

  @Test
  void saveOnDisconnectSavesDirtyDataAndEvictsCache() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data));
    manager.getOrLoad(playerUuid);
    manager.markDirty(playerUuid, 10);

    manager.saveOnDisconnect(playerUuid);

    assertFalse(manager.isDirty(playerUuid));
    verify(configManager).savePlayerData(data);
  }

  @Test
  void saveOnDisconnectDoesNotSaveCleanData() {
    // Returning-player loads start clean per the #142 contract: no save without an explicit
    // mutation.
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data));
    manager.getOrLoad(playerUuid);

    manager.saveOnDisconnect(playerUuid);

    assertFalse(manager.isDirty(playerUuid));
    verify(configManager, never()).savePlayerData(data);
  }

  @Test
  void tickSkipsSaveBeforeDebounceThreshold() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data));
    manager.getOrLoad(playerUuid);
    manager.markDirty(playerUuid, 0);

    manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS - 1);

    assertTrue(manager.isDirty(playerUuid));
    verify(configManager, never()).savePlayerData(any());
  }

  @Test
  void tickSavesExactlyAtDebounceThreshold() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data));
    manager.getOrLoad(playerUuid);
    manager.markDirty(playerUuid, 0);

    manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS);

    assertFalse(manager.isDirty(playerUuid));
    verify(configManager).savePlayerData(data);
  }

  @Test
  void tickOnlySavesDebouncedEntries() {
    UUID otherPlayer = UUID.randomUUID();
    LootLockPlayerData data1 = LootLockPlayerData.createDefault(playerUuid);
    LootLockPlayerData data2 = LootLockPlayerData.createDefault(otherPlayer);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data1));
    when(configManager.loadPlayerData(otherPlayer)).thenReturn(loadedFromDisk(data2));
    manager.getOrLoad(playerUuid);
    manager.getOrLoad(otherPlayer);
    manager.markDirty(playerUuid, 0);
    manager.markDirty(otherPlayer, 39);

    manager.tick(ServerPlayerDataManager.SAVE_DEBOUNCE_TICKS);

    assertFalse(manager.isDirty(playerUuid));
    assertTrue(manager.isDirty(otherPlayer));
    verify(configManager).savePlayerData(data1);
    verify(configManager, never()).savePlayerData(data2);
  }

  @Test
  void flushAllSavesAllDirtyEntries() {
    UUID otherPlayer = UUID.randomUUID();
    LootLockPlayerData data1 = LootLockPlayerData.createDefault(playerUuid);
    LootLockPlayerData data2 = LootLockPlayerData.createDefault(otherPlayer);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data1));
    when(configManager.loadPlayerData(otherPlayer)).thenReturn(loadedFromDisk(data2));
    manager.getOrLoad(playerUuid);
    manager.getOrLoad(otherPlayer);
    manager.markDirty(playerUuid, 0);
    manager.markDirty(otherPlayer, 0);

    int saved = manager.flushAll();

    assertEquals(2, saved);
    assertFalse(manager.isDirty(playerUuid));
    assertFalse(manager.isDirty(otherPlayer));
  }

  @Test
  void flushAllSkipsCleanEntries() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data));
    manager.getOrLoad(playerUuid);

    int saved = manager.flushAll();

    assertEquals(0, saved);
  }

  @Test
  void multiplePlayersAreIsolated() {
    UUID otherPlayer = UUID.randomUUID();
    LootLockPlayerData data1 = LootLockPlayerData.createDefault(playerUuid);
    LootLockPlayerData data2 = LootLockPlayerData.createDefault(otherPlayer);
    when(configManager.loadPlayerData(playerUuid)).thenReturn(loadedFromDisk(data1));
    when(configManager.loadPlayerData(otherPlayer)).thenReturn(loadedFromDisk(data2));

    LootLockPlayerData loaded1 = manager.getOrLoad(playerUuid);
    LootLockPlayerData loaded2 = manager.getOrLoad(otherPlayer);

    assertSame(data1, loaded1);
    assertSame(data2, loaded2);
  }
}
