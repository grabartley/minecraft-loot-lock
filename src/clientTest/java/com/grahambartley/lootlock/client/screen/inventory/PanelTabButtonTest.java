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

class PanelTabButtonTest {
  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest(name = "selected={0}, active={1}, hovered={2} -> {3}")
  @CsvSource({
    "true,  true,  false, widget/tab_selected",
    "true,  true,  true,  widget/tab_selected_highlighted",
    "false, true,  false, widget/tab",
    "false, true,  true,  widget/tab_highlighted",
    "false, false, true,  widget/tab",
    "true,  false, true,  widget/tab_selected",
  })
  void spriteOnlyHighlightsLiveTabs(
      boolean selected, boolean active, boolean hovered, String expectedPath) {
    assertEquals(
        Identifier.ofVanilla(expectedPath), PanelTabButton.sprite(selected, active, hovered));
  }

  @ParameterizedTest(name = "selected={0}, active={1}, hovered={2} -> 0x{3}")
  @CsvSource({
    "true,  true,  false, FF404040",
    "true,  false, true,  FF404040",
    "false, true,  false, FFA0A0A0",
    "false, true,  true,  FFFFFFFF",
    "false, false, true,  FFA0A0A0",
  })
  void labelColorReadsOnEachTabFace(
      boolean selected, boolean active, boolean hovered, String expectedHex) {
    assertEquals(
        Integer.parseUnsignedInt(expectedHex, 16),
        PanelTabButton.labelColor(selected, active, hovered));
  }

  @Test
  void onPressRunsTabAction() {
    AtomicInteger presses = new AtomicInteger();
    PanelTabButton tab =
        new PanelTabButton(
            0, 0, 60, 20, Text.literal("Rules"), () -> false, presses::incrementAndGet);

    tab.onPress();

    assertEquals(1, presses.get());
  }
}
