package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.glfw.GLFW;

class ItemSearchScreenTest {
  @ParameterizedTest
  @MethodSource("profileNames")
  void subtitleShowsAddingToWithProfileName(String profileName, String expected) {
    Text result = ItemSearchScreen.subtitle(profileName);

    assertEquals(expected, result.getString());
  }

  @ParameterizedTest
  @MethodSource("profileNames")
  void subtitleHighlightsProfileNameInYellow(String profileName, String expected) {
    Text result = ItemSearchScreen.subtitle(profileName);

    assertEquals(1, result.getSiblings().size());
    assertEquals(
        Formatting.YELLOW.getColorValue(),
        result.getSiblings().get(0).getStyle().getColor().getRgb());
  }

  @ParameterizedTest
  @MethodSource("profileNames")
  void subtitleShowsLabelInGray(String profileName, String expected) {
    Text result = ItemSearchScreen.subtitle(profileName);

    assertEquals(Formatting.GRAY.getColorValue(), result.getStyle().getColor().getRgb());
  }

  @Test
  void additiveSelectionClickTreatsMacSecondaryClickAsControl() {
    assertTrue(ItemSearchScreen.isAdditiveSelectionClick(1, false, false, true));
    assertFalse(ItemSearchScreen.isAdditiveSelectionClick(1, false, false, false));
    assertTrue(ItemSearchScreen.isAdditiveSelectionClick(0, true, false, false));
    assertTrue(ItemSearchScreen.isAdditiveSelectionClick(0, false, true, false));
  }

  @Test
  void primaryDoubleClickRequiresPlainPrimaryClick() {
    assertTrue(ItemSearchScreen.isPrimaryDoubleClick(0, false, false, 4, 4, 100L, 400L));
    assertFalse(ItemSearchScreen.isPrimaryDoubleClick(1, false, false, 4, 4, 100L, 400L));
    assertFalse(ItemSearchScreen.isPrimaryDoubleClick(0, true, false, 4, 4, 100L, 400L));
    assertFalse(ItemSearchScreen.isPrimaryDoubleClick(0, false, true, 4, 4, 100L, 400L));
    assertFalse(
        ItemSearchScreen.isPrimaryDoubleClick(
            GLFW.GLFW_MOUSE_BUTTON_LEFT, false, false, 4, 5, 100L, 400L));
  }

  static Stream<Arguments> profileNames() {
    return Stream.of(
        Arguments.of("Farming", "Adding to Farming"),
        Arguments.of("Mining", "Adding to Mining"),
        Arguments.of("Default", "Adding to Default"),
        Arguments.of("", "Adding to "));
  }
}
