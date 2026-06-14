package com.grahambartley.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class PickupDecisionTest {

  @ParameterizedTest(name = "{0} resolves")
  @ValueSource(strings = {"ALLOW", "REJECT_LEAVE", "REJECT_DELETE"})
  void valueOfResolvesExpectedConstant(String name) {
    assertNotNull(PickupDecision.valueOf(name));
  }

  @ParameterizedTest
  @EnumSource(PickupDecision.class)
  void everyConstantRoundTripsThroughValueOf(PickupDecision decision) {
    assertEquals(decision, PickupDecision.valueOf(decision.name()));
  }

  @Test
  void enumHasExactlyThreeConstants() {
    assertEquals(3, PickupDecision.values().length);
  }
}
