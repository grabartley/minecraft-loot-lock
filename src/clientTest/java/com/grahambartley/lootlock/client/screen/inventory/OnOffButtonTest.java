package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class OnOffButtonTest {
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
    OnOffButton widget =
        new OnOffButton(0, 0, 42, 16, () -> true, () -> fired.set(true), initialReadOnly, false);

    if (setReadOnly != null) {
      widget.setReadOnly(setReadOnly);
    }
    widget.onPress();

    assertEquals(expectedFired, fired.get());
    assertEquals(expectedReadOnly, widget.isReadOnly());
  }

  @ParameterizedTest(name = "active={0}, readOnly={1}, hovered={2} -> {3}")
  @CsvSource({
    "true,  false, false, widget/button",
    "true,  false, true,  widget/button_highlighted",
    "true,  true,  true,  widget/button_disabled",
    "false, false, true,  widget/button_disabled",
  })
  void spriteShowsDisabledFaceWhenNotInteractive(
      boolean active, boolean readOnly, boolean hovered, String expectedPath) {
    assertEquals(Identifier.ofVanilla(expectedPath), OnOffButton.sprite(active, readOnly, hovered));
  }

  static Stream<Arguments> labelColorCases() {
    return Stream.of(
        Arguments.of(
            "unsupported server stays red", false, true, true, true, Palette.DENY_ON_PRESSED),
        Arguments.of(
            "unsupported server locked stays red",
            false,
            false,
            true,
            true,
            Palette.DENY_ON_PRESSED),
        Arguments.of("read-only on reads green", true, true, true, true, Palette.ALLOW_ON_PRESSED),
        Arguments.of(
            "read-only off reads grey", false, true, true, false, Palette.BUTTON_TEXT_DISABLED),
        Arguments.of("locked reads grey", true, false, false, false, Palette.BUTTON_TEXT_DISABLED),
        Arguments.of("interactive on reads white", true, true, false, false, Palette.BUTTON_TEXT),
        Arguments.of(
            "interactive off reads white", false, true, false, false, Palette.BUTTON_TEXT));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("labelColorCases")
  void labelColorKeepsUnsupportedStateDistinct(
      String label,
      boolean on,
      boolean active,
      boolean readOnly,
      boolean badWhenOff,
      int expected) {
    assertEquals(expected, OnOffButton.labelColor(on, active, readOnly, badWhenOff));
  }
}
