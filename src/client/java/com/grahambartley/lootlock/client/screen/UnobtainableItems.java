package com.grahambartley.lootlock.client.screen;

import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public final class UnobtainableItems {
  private UnobtainableItems() {}

  private static final Set<Item> EXPLICIT_BLOCKLIST =
      Set.of(
          Items.BEDROCK,
          Items.END_PORTAL_FRAME,
          Items.KNOWLEDGE_BOOK,
          Items.DRAGON_EGG,
          Items.SPAWNER);

  private static final Set<Item> OPERATOR_ITEMS =
      Set.of(
          Items.COMMAND_BLOCK,
          Items.CHAIN_COMMAND_BLOCK,
          Items.REPEATING_COMMAND_BLOCK,
          Items.COMMAND_BLOCK_MINECART,
          Items.STRUCTURE_BLOCK,
          Items.STRUCTURE_VOID,
          Items.JIGSAW,
          Items.BARRIER,
          Items.LIGHT,
          Items.DEBUG_STICK);

  public static boolean isUnobtainable(Item item) {
    return item == Items.AIR
        || !item.isEnabled(FeatureFlags.DEFAULT_ENABLED_FEATURES)
        || OPERATOR_ITEMS.contains(item)
        || EXPLICIT_BLOCKLIST.contains(item);
  }
}
