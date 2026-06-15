package com.grahambartley.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockCommandTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

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

    assertEquals(expected, LootLockCommand.canCreateProfile(data));
  }

  @Test
  void canCreateProfileFalseForNullData() {
    assertFalse(LootLockCommand.canCreateProfile(null));
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

  @ParameterizedTest(name = "tryParseUuid(\"{0}\") -> present={1}")
  @CsvSource({
    "069a79f4-44e9-4726-a5be-fca90e38aaf5, true",
    "00000000-0000-0000-0000-000000000000, true",
    "069A79F4-44E9-4726-A5BE-FCA90E38AAF5, true",
    "Steve,                                false",
    "069a79f4-44e9-4726-a5be,              false",
    "'',                                   false",
  })
  void tryParseUuidParsesOnlyValidLiterals(String input, boolean expectedPresent) {
    assertEquals(expectedPresent, LootLockCommand.tryParseUuid(input).isPresent());
  }

  @Test
  void tryParseUuidReturnsEmptyForNull() {
    assertFalse(LootLockCommand.tryParseUuid(null).isPresent());
  }

  @Test
  void appendProfileAddsToTargetWithoutTouchingOthers() {
    LootLockPlayerData targetData = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockPlayerData otherData = LootLockPlayerData.createDefault(UUID.randomUUID());
    int otherSizeBefore = otherData.getProfiles().size();
    int targetSizeBefore = targetData.getProfiles().size();

    LootLockProfile created = LootLockCommand.createProfileWithDefaults("Mining");
    LootLockCommand.appendProfile(targetData, created);

    assertEquals(targetSizeBefore + 1, targetData.getProfiles().size());
    assertEquals(otherSizeBefore, otherData.getProfiles().size());
    assertTrue(LootLockCommand.findProfileByName(targetData, "Mining").isPresent());
    assertFalse(LootLockCommand.findProfileByName(otherData, "Mining").isPresent());
  }

  @Test
  void removeProfileByIdRebindsActiveWhenActiveDeleted() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockProfile second = LootLockCommand.createProfileWithDefaults("Second");
    LootLockCommand.appendProfile(data, second);
    data.setActiveProfileId(second.getId());

    LootLockCommand.removeProfileById(data, second.getId());

    assertFalse(LootLockCommand.findProfileByName(data, "Second").isPresent());
    assertNotEquals(second.getId(), data.getActiveProfileId());
    assertEquals(data.getProfiles().get(0).getId(), data.getActiveProfileId());
  }

  @Test
  void removeProfileByIdLeavesActiveAloneWhenAnotherDeleted() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    UUID originallyActive = data.getActiveProfileId();
    LootLockProfile other = LootLockCommand.createProfileWithDefaults("Other");
    LootLockCommand.appendProfile(data, other);

    LootLockCommand.removeProfileById(data, other.getId());

    assertEquals(originallyActive, data.getActiveProfileId());
  }

  @Test
  void appendProfileDoesNotTouchOtherPlayerData() {
    LootLockPlayerData target = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockPlayerData other = LootLockPlayerData.createDefault(UUID.randomUUID());
    UUID otherActiveBefore = other.getActiveProfileId();

    LootLockProfile created = LootLockCommand.createProfileWithDefaults("Mining");
    LootLockCommand.appendProfile(target, created);

    assertEquals(otherActiveBefore, other.getActiveProfileId());
    assertFalse(LootLockCommand.findProfileByName(other, "Mining").isPresent());
  }

  @ParameterizedTest(name = "addRuleToProfile(\"{0}\") -> added={1}")
  @CsvSource({
    "minecraft:cobblestone, true",
    "minecraft:stone,       false",
  })
  void addRuleToProfileSkipsDuplicates(String itemId, boolean expectedAdded) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone")));

    assertEquals(expectedAdded, LootLockCommand.addRuleToProfile(profile, itemId));
    assertTrue(LootLockCommand.containsRule(profile, itemId));
  }

  @ParameterizedTest(name = "addRuleToProfile(\"{0}\") -> added={1}")
  @CsvSource({
    "#minecraft:flowers, true",
    "#minecraft:wool,    true",
  })
  void addRuleToProfileAcceptsTagTokens(String token, boolean expectedAdded) {
    LootLockProfile profile = LootLockProfile.createDefault();

    assertEquals(expectedAdded, LootLockCommand.addRuleToProfile(profile, token));
    assertTrue(LootLockCommand.containsRule(profile, token));
  }

  @Test
  void tagExistsReturnsFalseForNullOrUnknownTag() {
    assertFalse(LootLockCommand.tagExists(null));
    assertFalse(
        LootLockCommand.tagExists(net.minecraft.util.Identifier.tryParse("lootlock:bogus_tag")));
  }

  @ParameterizedTest(name = "removeRuleFromProfile(\"{0}\") -> removed={1}")
  @CsvSource({
    "minecraft:stone,   true",
    "minecraft:diamond, false",
  })
  void removeRuleFromProfileTouchesOnlyMatchingEntries(String itemId, boolean expectedRemoved) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    boolean removed = LootLockCommand.removeRuleFromProfile(profile, itemId);

    assertEquals(expectedRemoved, removed);
    assertTrue(LootLockCommand.containsRule(profile, "minecraft:dirt"));
  }

  @Test
  void clearRulesOnProfileEmptiesList() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    LootLockCommand.clearRulesOnProfile(profile);

    assertTrue(profile.getRules().isEmpty());
  }

  @Test
  void mutationsOnTargetDoNotLeakIntoUnrelatedPlayerData() {
    LootLockPlayerData target = LootLockPlayerData.createDefault(UUID.randomUUID());
    LootLockPlayerData unrelated = LootLockPlayerData.createDefault(UUID.randomUUID());

    LootLockProfile targetActive = target.getActiveProfile().orElseThrow();
    LootLockProfile unrelatedActive = unrelated.getActiveProfile().orElseThrow();
    Optional<LootLockProfile> unrelatedSnapshot =
        Optional.of(unrelatedActive).map(p -> copyProfile(p));

    LootLockCommand.addRuleToProfile(targetActive, "minecraft:stone");
    LootLockCommand.applyGlobalEnable(target, false);

    assertEquals(0, unrelatedActive.getRules().size());
    assertEquals(unrelatedSnapshot.orElseThrow().isEnabled(), unrelatedActive.isEnabled());
    assertTrue(unrelated.isGloballyEnabled());
  }

  private static LootLockProfile copyProfile(LootLockProfile profile) {
    return new LootLockProfile(
        profile.getId(),
        profile.getName(),
        profile.getMode(),
        profile.getRejectedItemAction(),
        profile.isEnabled(),
        new ArrayList<>(profile.getRules()));
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
