package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

class RuleListScreenTest {
  @Test
  void subtitleShowsProfileNameAndModeWithCorrectValues() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setName("Farming");
    profile.setMode(FilterMode.ALLOWLIST);

    Text result = RuleListScreen.subtitle(profile);

    assertEquals("Profile: Farming · Mode: Allowlist", result.getString());
  }

  @Test
  void subtitleHighlightsProfileNameInYellow() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setName("Mining");

    Text result = RuleListScreen.subtitle(profile);
    Text profileName = result.getSiblings().get(0);

    assertEquals(Formatting.YELLOW.getColorValue(), profileName.getStyle().getColor().getRgb());
  }

  @Test
  void subtitleHighlightsModeInYellow() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setMode(FilterMode.ALLOWLIST);

    Text result = RuleListScreen.subtitle(profile);
    Text modeValue = result.getSiblings().get(2);

    assertEquals(Formatting.YELLOW.getColorValue(), modeValue.getStyle().getColor().getRgb());
  }

  @Test
  void subtitleShowsLabelsInGray() {
    LootLockProfile profile = LootLockProfile.createDefault();

    Text result = RuleListScreen.subtitle(profile);

    assertEquals(Formatting.GRAY.getColorValue(), result.getStyle().getColor().getRgb());
    assertEquals(
        Formatting.GRAY.getColorValue(),
        result.getSiblings().get(1).getStyle().getColor().getRgb());
  }

  @Test
  void subtitleHasExactlyThreeSiblingsInCorrectOrder() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setName("Mining");
    profile.setMode(FilterMode.ALLOWLIST);

    Text result = RuleListScreen.subtitle(profile);

    assertEquals(3, result.getSiblings().size());
    assertEquals("Mining", result.getSiblings().get(0).getString());
    assertEquals(" · Mode: ", result.getSiblings().get(1).getString());
    assertEquals("Allowlist", result.getSiblings().get(2).getString());
  }
}
