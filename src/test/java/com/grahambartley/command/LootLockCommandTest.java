package com.grahambartley.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockCommandTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "DENYLIST,  denylist",
    "ALLOWLIST, allowlist",
  })
  void modeTokenMapsEnumToCommandToken(FilterMode mode, String expected) {
    assertEquals(expected, LootLockCommand.modeToken(mode));
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "LEAVE_ON_GROUND, leave",
    "DELETE,          delete",
  })
  void actionTokenMapsEnumToCommandToken(RejectedItemAction action, String expected) {
    assertEquals(expected, LootLockCommand.actionToken(action));
  }

  @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
  @CsvSource({
    "'  Mining  ', Mining",
  })
  void normalizeProfileNameTrimsValidName(String raw, String expected) {
    assertEquals(expected, LootLockCommand.normalizeProfileName(raw));
  }

  @ParameterizedTest(name = "rejects blank or oversize: \"{0}\"")
  @ValueSource(strings = {"   ", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
  void normalizeProfileNameRejectsBlankOrOversize(String raw) {
    assertNull(LootLockCommand.normalizeProfileName(raw));
  }

  @Test
  void findProfileByNameMatchesIgnoringCase() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setName("Farming");
    profiles.add(profile);
    data.setProfiles(profiles);

    assertTrue(LootLockCommand.findProfileByName(data, "farming").isPresent());
    assertFalse(LootLockCommand.findProfileByName(data, "nether").isPresent());
  }

  @ParameterizedTest(name = "containsRule({1}) -> {2}")
  @CsvSource({
    "ignored, minecraft:stone,   true",
    "ignored, minecraft:dirt,    true",
    "ignored, minecraft:diamond, false",
  })
  void containsRuleChecksMembership(String label, String itemId, boolean expected) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    assertEquals(expected, LootLockCommand.containsRule(profile, itemId));
  }

  @ParameterizedTest(name = "normalize({0}, allowDelete={1}) -> {2}")
  @CsvSource({
    "DELETE,          false, LEAVE_ON_GROUND",
    "DELETE,          true,  DELETE",
    "LEAVE_ON_GROUND, false, LEAVE_ON_GROUND",
    "LEAVE_ON_GROUND, true,  LEAVE_ON_GROUND",
  })
  void normalizeRejectedItemActionRespectsPolicy(
      RejectedItemAction action, boolean allowDelete, RejectedItemAction expected) {
    assertEquals(expected, LootLockCommand.normalizeRejectedItemAction(action, allowDelete));
  }

  @Test
  void normalizeRejectedItemActionTreatsNullAsLeave() {
    assertEquals(
        RejectedItemAction.LEAVE_ON_GROUND,
        LootLockCommand.normalizeRejectedItemAction(null, true));
  }

  @ParameterizedTest(name = "applyGlobalEnable({0}) flips every profile")
  @CsvSource({"true", "false"})
  void applyGlobalEnableFlipsEveryProfileFromMixedState(boolean targetState) {
    LootLockPlayerData data = newMixedEnabledData();

    LootLockCommand.applyGlobalEnable(data, targetState);

    for (LootLockProfile profile : data.getProfiles()) {
      assertEquals(targetState, profile.isEnabled());
    }
    assertEquals(targetState, data.isGloballyEnabled());
  }

  private static LootLockPlayerData newMixedEnabledData() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    List<LootLockProfile> profiles =
        new ArrayList<>(
            List.of(
                profile("Farming", FilterMode.DENYLIST, true),
                profile("Mining", FilterMode.ALLOWLIST, false),
                profile("Nether", FilterMode.DENYLIST, true)));
    data.setProfiles(profiles);
    return data;
  }

  private static LootLockProfile profile(String name, FilterMode mode, boolean enabled) {
    return new LootLockProfile(
        UUID.randomUUID(), name, mode, RejectedItemAction.LEAVE_ON_GROUND, enabled, List.of());
  }
}
