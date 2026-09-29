package com.grahambartley.lootlock.client.screen.inventory;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class DragToAddRouter {
  private DragToAddRouter() {}

  public static String route(ItemStack stack) {
    String itemId = itemIdOf(stack);
    if (itemId == null) {
      return null;
    }
    RuleMutations.addToActiveProfile(List.of(itemId));
    return itemId;
  }

  public static String itemIdOf(ItemStack stack) {
    if (stack == null || stack.isEmpty()) {
      return null;
    }
    Identifier id = Registries.ITEM.getId(stack.getItem());
    if (id == null) {
      return null;
    }
    return id.toString();
  }
}
