package com.grahambartley.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class PickupDecisionTest {

  @Test
  void enumHasExpectedValues() {
    assertNotNull(PickupDecision.valueOf("ALLOW"));
    assertNotNull(PickupDecision.valueOf("REJECT_LEAVE"));
    assertNotNull(PickupDecision.valueOf("REJECT_DELETE"));
    assertEquals(3, PickupDecision.values().length);
  }
}
