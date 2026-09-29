package com.grahambartley.lootlock.gametest;

import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.TestContext;
import net.minecraft.world.GameMode;

final class GameTestPlayers {
  private GameTestPlayers() {}

  static ServerPlayerEntity joinInSurvival(TestContext context) {
    ServerPlayerEntity player = context.createMockCreativeServerPlayerInWorld();
    player.changeGameMode(GameMode.SURVIVAL);
    return player;
  }

  static void removeAndComplete(TestContext context, ServerPlayerEntity... players) {
    for (ServerPlayerEntity player : players) {
      context.getWorld().getServer().getPlayerManager().remove(player);
    }
    context.complete();
  }
}
