package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RuleRowButtonTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<Arguments> washCases() {
    return Stream.of(
        Arguments.of("selected wins over hover", true, true, Palette.SELECTED_WASH),
        Arguments.of("selected", true, false, Palette.SELECTED_WASH),
        Arguments.of("hovered", false, true, Palette.HOVER_WASH),
        Arguments.of("idle has no wash", false, false, 0));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("washCases")
  void rowWashDistinguishesSelectionFromHover(
      String label, boolean selected, boolean hovered, int expected) {
    assertEquals(expected, RuleRowButton.rowWash(selected, hovered));
  }

  @Test
  void onPressRunsRowAction() {
    AtomicInteger presses = new AtomicInteger();
    RuleRowButton row =
        new RuleRowButton(
            0,
            0,
            200,
            null,
            "Stone",
            "minecraft:stone",
            false,
            () -> false,
            presses::incrementAndGet);

    row.onPress();

    assertEquals(1, presses.get());
  }
}
