package com.grahambartley.server;

import com.grahambartley.data.LootLockPlayerData;
import net.minecraft.server.network.ServerPlayerEntity;

public final class ServerPlayerDataManager {
	public LootLockPlayerData get(ServerPlayerEntity player) {
		return LootLockPlayerData.createDefault(player.getUuid());
	}
}
