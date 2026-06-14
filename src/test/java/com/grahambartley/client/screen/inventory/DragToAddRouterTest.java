package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class DragToAddRouterTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void itemIdOfReturnsRegistryIdentifierString() {
    assertEquals("minecraft:diamond", DragToAddRouter.itemIdOf(new ItemStack(Items.DIAMOND)));
  }

  @Test
  void itemIdOfReturnsNullForEmptyStack() {
    assertNull(DragToAddRouter.itemIdOf(ItemStack.EMPTY));
  }

  @Test
  void itemIdOfTolerantOfNullStack() {
    assertNull(DragToAddRouter.itemIdOf(null));
  }
}
