package com.grahambartley.lootlock.gametest;

import com.grahambartley.lootlock.config.ConfigPaths;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.WorldSavePath;

public final class ServerLifecycleHooksGameTest implements FabricGameTest {
  private static final String BATCH = "join-sync";
  private static final int TICK_LIMIT = 100;

  @GameTest(templateName = FabricGameTest.EMPTY_STRUCTURE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void aJoiningPlayerHasDefaultDataSavedIntoThisWorld(TestContext context) {
    ServerPlayerEntity player = GameTestPlayers.joinInSurvival(context);
    Path worldDir = context.getWorld().getServer().getSavePath(WorldSavePath.ROOT).normalize();
    Path dataFile = new ConfigPaths(worldDir).getPlayerDataPath(player.getUuid());

    context
        .createTimedTaskRunner()
        .createAndAdd(
            () ->
                context.assertTrue(
                    Files.isRegularFile(dataFile),
                    "Joining should create and save the player's data at " + dataFile))
        .createAndAdd(() -> context.getWorld().getServer().getPlayerManager().remove(player))
        .completeIfSuccessful();
  }
}
