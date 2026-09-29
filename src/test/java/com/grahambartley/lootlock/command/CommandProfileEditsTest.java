package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class CommandProfileEditsTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
  @CsvSource({
    "'  Mining  ', Mining",
  })
  void normalizeProfileNameTrimsValidName(String raw, String expected) {
    assertEquals(expected, CommandProfileEdits.normalizeProfileName(raw));
  }

  @ParameterizedTest(name = "rejects blank or oversize: \"{0}\"")
  @ValueSource(strings = {"   ", "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"})
  void normalizeProfileNameRejectsBlankOrOversize(String raw) {
    assertNull(CommandProfileEdits.normalizeProfileName(raw));
  }

  @Test
  void findProfileByNameMatchesIgnoringCase() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setName("Farming");
    profiles.add(profile);
    data.setProfiles(profiles);

    assertTrue(CommandProfileEdits.findProfileByName(data, "farming").isPresent());
    assertFalse(CommandProfileEdits.findProfileByName(data, "nether").isPresent());
  }

  @ParameterizedTest(name = "containsRule({0}) -> {1}")
  @CsvSource({
    "minecraft:stone,   true",
    "minecraft:dirt,    true",
    "minecraft:diamond, false",
  })
  void containsRuleChecksMembership(String itemId, boolean expected) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    assertEquals(expected, CommandProfileEdits.containsRule(profile, itemId));
  }

  @ParameterizedTest(name = "normalize({0}, allowDelete={1}) -> {2}")
  @CsvSource({
    "DELETE,          false, LEAVE_ON_GROUND",
    "DELETE,          true,  DELETE",
    "LEAVE_ON_GROUND, false, LEAVE_ON_GROUND",
    "LEAVE_ON_GROUND, true,  LEAVE_ON_GROUND",
    ",                true,  LEAVE_ON_GROUND",
  })
  void normalizeRejectedItemActionRespectsPolicy(
      RejectedItemAction action, boolean allowDelete, RejectedItemAction expected) {
    assertEquals(expected, CommandProfileEdits.normalizeRejectedItemAction(action, allowDelete));
  }

  @ParameterizedTest(name = "{0} profiles -> canCreate={1}")
  @CsvSource({
    "1, true",
    "8, true",
    "9, false",
  })
  void canCreateProfileRespectsCap(int existingProfiles, boolean expected) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    List<LootLockProfile> profiles = new ArrayList<>(existingProfiles);
    for (int i = 0; i < existingProfiles; i++) {
      profiles.add(profile("Profile " + i, FilterMode.DENYLIST, true));
    }
    data.setProfiles(profiles);

    assertEquals(expected, CommandProfileEdits.canCreateProfile(data));
  }

  @Test
  void canCreateProfileFalseForNullData() {
    assertFalse(CommandProfileEdits.canCreateProfile(null));
  }

  @ParameterizedTest(name = "applyGlobalEnable({0}) flips every profile")
  @CsvSource({"true", "false"})
  void applyGlobalEnableFlipsEveryProfileFromMixedState(boolean targetState) {
    LootLockPlayerData data = newMixedEnabledData();

    CommandProfileEdits.applyGlobalEnable(data, targetState);

    for (LootLockProfile profile : data.getProfiles()) {
      assertEquals(targetState, profile.isEnabled());
    }
    assertEquals(targetState, data.isGloballyEnabled());
  }

  @Test
  void createProfileWithDefaultsUsesDenylistLeaveEnabledAndNoRules() {
    LootLockProfile created = CommandProfileEdits.createProfileWithDefaults("Mining");

    assertEquals("Mining", created.getName());
    assertEquals(FilterMode.DENYLIST, created.getMode());
    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, created.getRejectedItemAction());
    assertTrue(created.isEnabled());
    assertTrue(created.getRules().isEmpty());
  }

  @Test
  void appendProfileAddsToTargetWithoutTouchingOthers() {
    LootLockPlayerData targetData = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockPlayerData otherData = LootLockPlayerData.createDefault(UUID.randomUUID());
    int otherSizeBefore = otherData.getProfiles().size();
    int targetSizeBefore = targetData.getProfiles().size();
    UUID otherActiveBefore = otherData.getActiveProfileId();

    LootLockProfile created = CommandProfileEdits.createProfileWithDefaults("Mining");
    CommandProfileEdits.appendProfile(targetData, created);

    assertEquals(targetSizeBefore + 1, targetData.getProfiles().size());
    assertEquals(otherSizeBefore, otherData.getProfiles().size());
    assertEquals(otherActiveBefore, otherData.getActiveProfileId());
    assertTrue(CommandProfileEdits.findProfileByName(targetData, "Mining").isPresent());
    assertFalse(CommandProfileEdits.findProfileByName(otherData, "Mining").isPresent());
  }

  @Test
  void removeProfileByIdRebindsActiveWhenActiveDeleted() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockProfile second = CommandProfileEdits.createProfileWithDefaults("Second");
    CommandProfileEdits.appendProfile(data, second);
    data.setActiveProfileId(second.getId());

    CommandProfileEdits.removeProfileById(data, second.getId());

    assertFalse(CommandProfileEdits.findProfileByName(data, "Second").isPresent());
    assertNotEquals(second.getId(), data.getActiveProfileId());
    assertEquals(data.getProfiles().get(0).getId(), data.getActiveProfileId());
  }

  @Test
  void removeProfileByIdLeavesActiveAloneWhenAnotherDeleted() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    UUID originallyActive = data.getActiveProfileId();
    LootLockProfile other = CommandProfileEdits.createProfileWithDefaults("Other");
    CommandProfileEdits.appendProfile(data, other);

    CommandProfileEdits.removeProfileById(data, other.getId());

    assertEquals(originallyActive, data.getActiveProfileId());
  }

  @ParameterizedTest(name = "addRuleToProfile(\"{0}\") -> added={1}")
  @CsvSource({
    "minecraft:cobblestone, true",
    "minecraft:stone,       false",
    "#minecraft:flowers,    true",
    "#minecraft:wool,       true",
  })
  void addRuleToProfileSkipsDuplicates(String itemId, boolean expectedAdded) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone")));

    assertEquals(expectedAdded, CommandProfileEdits.addRuleToProfile(profile, itemId));
    assertTrue(CommandProfileEdits.containsRule(profile, itemId));
  }

  @ParameterizedTest(name = "removeRuleFromProfile(\"{0}\") -> removed={1}")
  @CsvSource({
    "minecraft:stone,   true",
    "minecraft:diamond, false",
  })
  void removeRuleFromProfileTouchesOnlyMatchingEntries(String itemId, boolean expectedRemoved) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    boolean removed = CommandProfileEdits.removeRuleFromProfile(profile, itemId);

    assertEquals(expectedRemoved, removed);
    assertFalse(CommandProfileEdits.containsRule(profile, itemId));
    assertTrue(CommandProfileEdits.containsRule(profile, "minecraft:dirt"));
  }

  @Test
  void clearRulesOnProfileEmptiesList() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    CommandProfileEdits.clearRulesOnProfile(profile);

    assertTrue(profile.getRules().isEmpty());
  }

  @Test
  void mutationsOnTargetDoNotLeakIntoUnrelatedPlayerData() {
    LootLockPlayerData target = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockPlayerData unrelated = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockProfile targetActive = target.getActiveProfile().orElseThrow();
    LootLockProfile unrelatedActive = unrelated.getActiveProfile().orElseThrow();
    boolean unrelatedEnabledBefore = unrelatedActive.isEnabled();

    CommandProfileEdits.addRuleToProfile(targetActive, "minecraft:stone");
    CommandProfileEdits.applyGlobalEnable(target, false);

    assertEquals(0, unrelatedActive.getRules().size());
    assertEquals(unrelatedEnabledBefore, unrelatedActive.isEnabled());
    assertTrue(unrelated.isGloballyEnabled());
  }

  @ParameterizedTest(name = "taken {1} -> next for \"{0}\" is \"{2}\"")
  @CsvSource(
      delimiter = '|',
      value = {
        "Mining | ''                  | Mining",
        "Mining | Mining              | Mining (2)",
        "Mining | Mining;Mining (2)   | Mining (3)",
        "''     | ''                  | Profile",
        "''     | Profile             | Profile (2)",
      })
  void nextAvailableNameSkipsTakenNames(String sourceName, String taken, String expected) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    for (String name : taken.split(";")) {
      if (!name.isEmpty()) {
        CommandProfileEdits.appendProfile(
            data, CommandProfileEdits.createProfileWithDefaults(name));
      }
    }

    assertEquals(expected, CommandProfileEdits.nextAvailableName(data, sourceName));
  }

  @Test
  void nextAvailableNameTrimsBaseToFitMaxLength() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    String thirtyTwo = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef";
    CommandProfileEdits.appendProfile(
        data, CommandProfileEdits.createProfileWithDefaults(thirtyTwo));

    String result = CommandProfileEdits.nextAvailableName(data, thirtyTwo);

    assertEquals("ABCDEFGHIJKLMNOPQRSTUVWXYZab (2)", result);
  }

  private static LootLockPlayerData newMixedEnabledData() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setProfiles(
        new ArrayList<>(
            List.of(
                profile("Farming", FilterMode.DENYLIST, true),
                profile("Mining", FilterMode.ALLOWLIST, false),
                profile("Nether", FilterMode.DENYLIST, true))));
    return data;
  }

  private static LootLockProfile profile(String name, FilterMode mode, boolean enabled) {
    return new LootLockProfile(
        UUID.randomUUID(), name, mode, RejectedItemAction.LEAVE_ON_GROUND, enabled, List.of());
  }
}
