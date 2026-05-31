package com.grahambartley.server;

import com.grahambartley.LootLock;
import com.grahambartley.config.ConfigPaths;
import com.grahambartley.config.LootLockConfig;
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
