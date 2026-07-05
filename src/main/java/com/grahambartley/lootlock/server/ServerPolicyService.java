package com.grahambartley.lootlock.server;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.config.ConfigPaths;
import com.grahambartley.lootlock.config.LootLockConfig;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

public final class ServerPolicyService {
  private ServerPolicyService() {}

  public static boolean updateAllowDeleteRejectedItems(
      MinecraftServer server, boolean allowDeleteRejectedItems) {
    if (server == null) {
      return false;
    }
    LootLockConfig updated = new LootLockConfig(allowDeleteRejectedItems);
    ConfigPaths paths = new ConfigPaths(server.getSavePath(WorldSavePath.ROOT).normalize());
    if (!LootLockConfig.save(paths.getServerPolicyPath(), updated)) {
      return false;
    }
    LootLock.SERVER_CONFIG = updated;
    return true;
  }
}
