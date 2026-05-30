package com.grahambartley.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PickupSourceTest {

  @Test
  void enumHasExpectedValues() {
    assertNotNull(PickupSource.valueOf("ITEM_ENTITY_COLLISION"));
    assertNotNull(PickupSource.valueOf("INVENTORY_INSERT_GUARD"));
    assertNotNull(PickupSource.valueOf("COMMAND_TEST"));
    assertNotNull(PickupSource.valueOf("FUTURE_UNKNOWN"));
    assertEquals(4, PickupSource.values().length);
  }
}
