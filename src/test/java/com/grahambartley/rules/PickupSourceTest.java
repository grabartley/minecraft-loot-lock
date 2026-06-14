package com.grahambartley.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class PickupSourceTest {

  @ParameterizedTest
  @EnumSource(PickupSource.class)
  void everyConstantRoundTripsThroughValueOf(PickupSource source) {
    assertEquals(source, PickupSource.valueOf(source.name()));
  }

  @Test
  void enumHasExactlyFourConstants() {
    assertEquals(4, PickupSource.values().length);
  }
}
