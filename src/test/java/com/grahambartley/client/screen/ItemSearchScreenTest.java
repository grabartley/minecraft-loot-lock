package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

class ItemSearchScreenTest {
  @Test
  void subtitleShowsAddingToWithProfileName() {
    Text result = ItemSearchScreen.subtitle("Farming");

    assertEquals("Adding to Farming", result.getString());
  }

  @Test
  void subtitleHighlightsProfileNameInYellow() {
    Text result = ItemSearchScreen.subtitle("Mining");

    assertEquals(1, result.getSiblings().size());
    assertEquals(
        Formatting.YELLOW.getColorValue(),
        result.getSiblings().get(0).getStyle().getColor().getRgb());
  }

  @Test
  void subtitleShowsLabelInGray() {
    Text result = ItemSearchScreen.subtitle("Default");

    assertEquals(Formatting.GRAY.getColorValue(), result.getStyle().getColor().getRgb());
  }
}
