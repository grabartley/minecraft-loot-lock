package com.grahambartley.lootlock.server;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.config.ConfigManager;
import com.grahambartley.lootlock.config.LootLockConfig;
import java.nio.file.Path;

public final class WorldSession {
  private static Path openWorldDir;

  private WorldSession() {}

  public static synchronized void open(Path worldDir) {
    Path normalized = worldDir.normalize();
    ConfigManager configManager = new ConfigManager(normalized);
    LootLock.SERVER_CONFIG = LootLockConfig.load(configManager.getPaths().getServerPolicyPath());
    LootLock.PLAYER_DATA_MANAGER = new ServerPlayerDataManager(configManager);
    LootLock.PICKUP_GUARD = new PickupGuard(LootLock.PLAYER_DATA_MANAGER);
    openWorldDir = normalized;
  }

  public static synchronized void ensureOpen(Path worldDir) {
    if (!isOpenFor(worldDir)) {
      open(worldDir);
    }
  }

  public static synchronized void close() {
    LootLock.PLAYER_DATA_MANAGER = null;
    LootLock.PICKUP_GUARD = null;
    LootLock.SERVER_CONFIG = LootLockConfig.defaults();
    openWorldDir = null;
  }

  public static synchronized boolean isOpenFor(Path worldDir) {
    return openWorldDir != null && openWorldDir.equals(worldDir.normalize());
  }
}
