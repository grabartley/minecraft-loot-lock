package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

record CommandStateContext(
    UUID targetUuid,
    String displayName,
    ServerPlayerEntity onlineTarget,
    boolean isSelfTargeted,
    ServerPlayerDataManager dataManager,
    LootLockPlayerData data,
    LootLockProfile profile) {

  CommandStateContext withProfile(LootLockProfile newProfile) {
    return new CommandStateContext(
        targetUuid, displayName, onlineTarget, isSelfTargeted, dataManager, data, newProfile);
  }

  void markDirty(ServerCommandSource source) {
    if (onlineTarget != null) {
      dataManager.markDirty(onlineTarget);
    } else {
      MinecraftServer server = source.getServer();
      long tick = server != null ? server.getTicks() : 0L;
      dataManager.markDirty(targetUuid, tick);
    }
  }

  void syncIfOnline() {
    if (onlineTarget != null) {
      ServerToClientPackets.sendAuthoritativeSync(onlineTarget);
    }
  }
}
