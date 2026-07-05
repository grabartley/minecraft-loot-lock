package com.grahambartley.lootlock.client.screen.inventory;

import java.util.List;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Translates a player's Alt + click on an inventory slot into a rule add against the active
 * profile. Keeps the slot-to-rule mapping centralised so the InventoryScreen mixin can stay tiny
 * and the routing logic is verifiable in tests.
 */
public final class DragToAddRouter {
  private DragToAddRouter() {}

  /**
   * @return the item id that was added, or {@code null} when the stack is empty / unknown and
   *     nothing was routed.
   */
  public static String route(ItemStack stack) {
    String itemId = itemIdOf(stack);
    if (itemId == null) {
      return null;
    }
    RuleMutations.addToActiveProfile(List.of(itemId));
    return itemId;
  }

  /** Pure decision used in tests; returns the item id for a non-empty stack with a known item. */
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
