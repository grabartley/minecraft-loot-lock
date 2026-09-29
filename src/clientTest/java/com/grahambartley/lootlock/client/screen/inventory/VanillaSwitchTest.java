package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class VanillaSwitchTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest(name = "initialReadOnly={0}, setReadOnly={1} -> fired={2}, isReadOnly={3}")
  @CsvSource({
    "true,  ,      false, true",
    "true,  false, true,  false",
    "false, true,  false, true",
    "false, ,      true,  false",
  })
  void onPressFiresOnlyWhenSwitchIsInteractive(
      boolean initialReadOnly,
      Boolean setReadOnly,
      boolean expectedFired,
      boolean expectedReadOnly) {
    AtomicBoolean fired = new AtomicBoolean();
    VanillaSwitch widget =
        new VanillaSwitch(0, 0, 42, 16, () -> true, () -> fired.set(true), initialReadOnly, false);

    if (setReadOnly != null) {
      widget.setReadOnly(setReadOnly);
    }
    widget.onPress();

    assertEquals(expectedFired, fired.get());
    assertEquals(expectedReadOnly, widget.isReadOnly());
  }
}
