package com.grahambartley.lootlock.gametest;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import java.util.List;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.test.GameTest;
import net.minecraft.test.TestContext;
import net.minecraft.util.math.BlockPos;

public final class ItemEntityMixinGameTest implements FabricGameTest {
  private static final String TEMPLATE = "loot-lock:pickup_pad";
  private static final String BATCH = "pickup-filter";
  private static final int TICK_LIMIT = 20;
  private static final BlockPos DROP_POS = new BlockPos(2, 1, 2);

  @GameTest(templateName = TEMPLATE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void anItemMissingFromTheDenylistIsPickedUp(TestContext context) {
    ServerPlayerEntity player = GameTestPlayers.joinInSurvival(context);
    setActiveProfile(player, FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, Items.STONE);
    ItemEntity drop = context.spawnItem(Items.DIRT, DROP_POS);

    drop.onPlayerCollision(player);

    context.assertTrue(drop.isRemoved(), "An allowed drop should be taken off the ground");
    context.assertTrue(
        player.getInventory().count(Items.DIRT) == 1,
        "An allowed drop should land in the player's inventory");
    GameTestPlayers.disconnectAndComplete(context, player);
  }

  @GameTest(templateName = TEMPLATE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void aDenylistedItemIsLeftOnTheGround(TestContext context) {
    ServerPlayerEntity player = GameTestPlayers.joinInSurvival(context);
    setActiveProfile(player, FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, Items.STONE);
    ItemEntity drop = context.spawnItem(Items.STONE, DROP_POS);

    drop.onPlayerCollision(player);

    context.assertFalse(drop.isRemoved(), "A denied drop set to leave should stay on the ground");
    context.assertTrue(
        player.getInventory().count(Items.STONE) == 0,
        "A denied drop should never reach the player's inventory");
    GameTestPlayers.disconnectAndComplete(context, player);
  }

  @GameTest(templateName = TEMPLATE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void aDenylistedItemSetToDeleteIsDiscarded(TestContext context) {
    ServerPlayerEntity player = GameTestPlayers.joinInSurvival(context);
    setActiveProfile(player, FilterMode.DENYLIST, RejectedItemAction.DELETE, Items.STONE);
    ItemEntity drop = context.spawnItem(Items.STONE, DROP_POS);

    drop.onPlayerCollision(player);

    context.assertTrue(drop.isRemoved(), "A denied drop set to delete should be discarded");
    context.assertTrue(
        player.getInventory().count(Items.STONE) == 0,
        "A deleted drop should never reach the player's inventory");
    GameTestPlayers.disconnectAndComplete(context, player);
  }

  @GameTest(templateName = TEMPLATE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void anAllowlistLeavesEverythingElseOnTheGround(TestContext context) {
    ServerPlayerEntity player = GameTestPlayers.joinInSurvival(context);
    setActiveProfile(
        player, FilterMode.ALLOWLIST, RejectedItemAction.LEAVE_ON_GROUND, Items.DIAMOND);
    ItemEntity allowed = context.spawnItem(Items.DIAMOND, DROP_POS);
    ItemEntity other = context.spawnItem(Items.DIRT, DROP_POS);

    allowed.onPlayerCollision(player);
    other.onPlayerCollision(player);

    context.assertTrue(allowed.isRemoved(), "An allowlisted drop should be picked up");
    context.assertFalse(other.isRemoved(), "A drop missing from the allowlist should stay put");
    GameTestPlayers.disconnectAndComplete(context, player);
  }

  @GameTest(templateName = TEMPLATE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void rulesOnlyApplyToThePlayerWhoSetThem(TestContext context) {
    ServerPlayerEntity filtering = GameTestPlayers.joinInSurvival(context);
    ServerPlayerEntity unfiltered = GameTestPlayers.joinInSurvival(context);
    setActiveProfile(
        filtering, FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, Items.STONE);
    ItemEntity blockedDrop = context.spawnItem(Items.STONE, DROP_POS);
    ItemEntity openDrop = context.spawnItem(Items.STONE, DROP_POS);

    blockedDrop.onPlayerCollision(filtering);
    openDrop.onPlayerCollision(unfiltered);

    context.assertFalse(blockedDrop.isRemoved(), "The filtering player should not pick up stone");
    context.assertTrue(
        unfiltered.getInventory().count(Items.STONE) == 1,
        "Another player's rules should not stop this player picking up stone");
    GameTestPlayers.disconnectAndComplete(context, filtering, unfiltered);
  }

  @GameTest(templateName = TEMPLATE, batchId = BATCH, tickLimit = TICK_LIMIT)
  public void aDisabledProfilePicksUpDenylistedItems(TestContext context) {
    ServerPlayerEntity player = GameTestPlayers.joinInSurvival(context);
    LootLockProfile profile =
        setActiveProfile(
            player, FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, Items.STONE);
    profile.setEnabled(false);
    ItemEntity drop = context.spawnItem(Items.STONE, DROP_POS);

    drop.onPlayerCollision(player);

    context.assertTrue(drop.isRemoved(), "A disabled profile should not filter anything");
    GameTestPlayers.disconnectAndComplete(context, player);
  }

  private static LootLockProfile setActiveProfile(
      ServerPlayerEntity player, FilterMode mode, RejectedItemAction action, Item ruleItem) {
    LootLockProfile profile =
        LootLock.PLAYER_DATA_MANAGER.get(player).getActiveProfile().orElseThrow();
    profile.setMode(mode);
    profile.setRejectedItemAction(action);
    profile.setRules(List.of(new RuleEntry(Registries.ITEM.getId(ruleItem).toString())));
    return profile;
  }
}
