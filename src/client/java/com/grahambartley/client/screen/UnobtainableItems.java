package com.grahambartley.client.screen;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.resource.featuretoggle.FeatureFlags;

public final class UnobtainableItems {
  private UnobtainableItems() {}

  // Items that appear in non-OPERATOR creative tabs but aren't realistically
  // useful as LootLock rule targets:
  //   BEDROCK — only obtainable via /give in survival
  //   END_PORTAL_FRAME — same
  //   KNOWLEDGE_BOOK — only via /give, vanishes on use
  //   DRAGON_EGG — extreme edge case (piston pushing)
  //   SPAWNER — only via silk touch (impossible in vanilla, but mods may)
  private static final Set<Item> EXPLICIT_BLOCKLIST =
      Set.of(
          Items.BEDROCK,
          Items.END_PORTAL_FRAME,
          Items.KNOWLEDGE_BOOK,
          Items.DRAGON_EGG,
          Items.SPAWNER);

  private static volatile Set<Item> operatorItems;

  public static boolean isUnobtainable(Item item) {
    return item == Items.AIR
        || !item.isEnabled(FeatureFlags.DEFAULT_ENABLED_FEATURES)
        || operatorItems().contains(item)
        || EXPLICIT_BLOCKLIST.contains(item);
  }

  private static Set<Item> operatorItems() {
    Set<Item> local = operatorItems;
    if (local == null) {
      local = computeOperatorItems();
      operatorItems = local;
    }
    return local;
  }

  private static Set<Item> computeOperatorItems() {
    Set<Item> ops = new HashSet<>();
    ItemGroup group = Registries.ITEM_GROUP.get(ItemGroups.OPERATOR);
    if (group != null) {
      for (ItemStack stack : group.getDisplayStacks()) {
        ops.add(stack.getItem());
      }
    }
    return Set.copyOf(ops);
  }
}
