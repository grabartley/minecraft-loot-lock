package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class SegmentedButtonTest {
  private static final int SELECTED_LABEL = 0xFF6EC85D;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest(name = "selected={0}, active={1}, hovered={2} -> {3}")
  @CsvSource({
    "true,  true,  true,  widget/button_disabled",
    "true,  false, false, widget/button_disabled",
    "false, true,  false, widget/button",
    "false, true,  true,  widget/button_highlighted",
    "false, false, true,  widget/button_disabled",
  })
  void selectedSegmentReadsAsPressed(
      boolean selected, boolean active, boolean hovered, String expectedPath) {
    assertEquals(
        Identifier.ofVanilla(expectedPath), SegmentedButton.sprite(selected, active, hovered));
  }

  @ParameterizedTest(name = "selected={0}, active={1} -> 0x{2}")
  @CsvSource({
    "true,  true,  FF6EC85D",
    "true,  false, FF6EC85D",
    "false, true,  FFFFFFFF",
    "false, false, FFA0A0A0",
  })
  void selectedSegmentKeepsItsModeColour(boolean selected, boolean active, String expectedHex) {
    assertEquals(
        Integer.parseUnsignedInt(expectedHex, 16),
        SegmentedButton.labelColor(selected, active, SELECTED_LABEL));
  }

  @Test
  void onPressRunsSegmentAction() {
    AtomicInteger presses = new AtomicInteger();
    SegmentedButton segment =
        new SegmentedButton(
            0,
            0,
            60,
            18,
            Text.literal("Allow"),
            Palette.ALLOW,
            SELECTED_LABEL,
            () -> false,
            presses::incrementAndGet);

    segment.onPress();

    assertEquals(1, presses.get());
  }
}
