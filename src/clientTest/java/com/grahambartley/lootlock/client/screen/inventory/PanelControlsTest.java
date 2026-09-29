package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PanelControlsTest {

  @ParameterizedTest(name = "innerRight={0}, integrated={1} -> {2}")
  @CsvSource({
    "262, true,  220",
    "262, false, 138",
    "100, true,  58",
  })
  void clientSwitchMovesLeftToMakeRoomForServerSwitch(
      int innerRight, boolean integrated, int expectedX) {
    assertEquals(expectedX, PanelControls.clientSwitchX(innerRight, integrated));
  }
}
