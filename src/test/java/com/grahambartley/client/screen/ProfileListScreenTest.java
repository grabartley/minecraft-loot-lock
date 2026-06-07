package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

class ProfileListScreenTest {
  @Test
  void activeSubtitleOmitsEdgeCaseWhenNoActiveProfileExists() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setActiveProfileId(null);

    assertTrue(ProfileListScreen.activeSubtitle(data).isEmpty());
  }

  @Test
  void activeSubtitleHighlightsActiveProfileNameInYellow() {
    Text subtitle = ProfileListScreen.activeSubtitle("Farming");

    assertEquals("Active: Farming", subtitle.getString());
    assertEquals("Farming", subtitle.getSiblings().get(0).getString());
    assertEquals(
        Formatting.YELLOW.getColorValue(),
        subtitle.getSiblings().get(0).getStyle().getColor().getRgb());
  }

  @Test
  void profileNameTextUsesYellowForActiveRowsOnly() {
    Text active = ProfileListScreen.profileNameText("Mining", true);
    Text inactive = ProfileListScreen.profileNameText("Mining", false);

    assertEquals(Formatting.YELLOW.getColorValue(), active.getStyle().getColor().getRgb());
    assertTrue(inactive.getStyle().getColor() == null);
  }

  @Test
  void listStatusTextDescribesVisibleTruncationWithoutFakePagination() {
    assertEquals("4 profiles", ProfileListScreen.listStatusText(4, 5).getString());
    assertEquals(
        "11 profiles (showing first 5)", ProfileListScreen.listStatusText(11, 5).getString());
  }

  @Test
  void activeSubtitleUsesCurrentActiveProfileFromPlayerData() {
    LootLockProfile defaultProfile = LootLockProfile.createDefault();
    LootLockProfile farmingProfile = LootLockProfile.createDefault();
    farmingProfile.setName("Farming");

    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setProfiles(List.of(defaultProfile, farmingProfile));
    data.setActiveProfileId(farmingProfile.getId());

    assertEquals(
        "Active: Farming", ProfileListScreen.activeSubtitle(data).orElseThrow().getString());
  }
}
