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

  @ParameterizedTest(name = "selected={0}, hovered={1} -> {2}")
  @CsvSource({
    "true,  false, widget/tab_selected",
    "true,  true,  widget/tab_selected_highlighted",
    "false, false, widget/tab",
    "false, true,  widget/tab_highlighted",
  })
  void spriteMatchesVanillaTabStates(boolean selected, boolean hovered, String expectedPath) {
    assertEquals(Identifier.ofVanilla(expectedPath), PanelTabButton.sprite(selected, hovered));
  }

  @ParameterizedTest(name = "selected={0}, hovered={1} -> 0x{2}")
  @CsvSource({
    "true,  false, FF404040",
    "true,  true,  FF404040",
    "false, false, FFA0A0A0",
    "false, true,  FFFFFFFF",
  })
  void labelColorReadsOnEachTabFace(boolean selected, boolean hovered, String expectedHex) {
    assertEquals(
        Integer.parseUnsignedInt(expectedHex, 16), PanelTabButton.labelColor(selected, hovered));
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
