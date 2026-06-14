package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockPlayerDataTest {

  @Test
  void setRevisionRejectsNegativeValues() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());

    assertThrows(IllegalArgumentException.class, () -> data.setRevision(-1));
  }

  @Test
  void setRevisionRejectsDecreasingValues() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setRevision(5);

    assertThrows(IllegalArgumentException.class, () -> data.setRevision(4));
    assertEquals(5, data.getRevision());
  }

  @ParameterizedTest(name = "setEnabledForAll({0}) flips every profile")
  @ValueSource(booleans = {true, false})
  void setEnabledForAllFlipsEveryProfile(boolean targetState) {
    LootLockPlayerData data = newDataWithProfiles(true, false, true);

    data.setEnabledForAll(targetState);

    for (LootLockProfile profile : data.getProfiles()) {
      assertEquals(targetState, profile.isEnabled());
    }
  }

  @Test
  void setEnabledForAllTolerantOfNullEntries() {
    LootLockPlayerData data = newDataWithProfiles(true, true);
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.add(null);
    data.setProfiles(profiles);

    data.setEnabledForAll(false);

    for (LootLockProfile profile : data.getProfiles()) {
      if (profile != null) {
        assertFalse(profile.isEnabled());
      }
    }
  }

  static Stream<Arguments> globallyEnabledCases() {
    return Stream.of(
        Arguments.of("all enabled", new boolean[] {true, true, true}, true),
        Arguments.of("single disabled", new boolean[] {true, false, true}, false),
        Arguments.of("all disabled", new boolean[] {false, false, false}, false),
        Arguments.of("single enabled", new boolean[] {true}, true));
  }

  @ParameterizedTest(name = "isGloballyEnabled {0} -> {2}")
  @MethodSource("globallyEnabledCases")
  void isGloballyEnabledReflectsEveryProfile(
      String label, boolean[] enabledStates, boolean expected) {
    LootLockPlayerData data = newDataWithProfiles(enabledStates);

    assertEquals(expected, data.isGloballyEnabled());
  }

  @Test
  void isGloballyEnabledIgnoresNullEntries() {
    LootLockPlayerData data = newDataWithProfiles(true, true);
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.add(null);
    data.setProfiles(profiles);

    assertTrue(data.isGloballyEnabled());
  }

  @Test
  void setEnabledForAllCollapsesMixedStateIntoSingleValue() {
    LootLockPlayerData data = newDataWithProfiles(true, false, true, false);

    data.setEnabledForAll(true);

    assertTrue(data.isGloballyEnabled());
  }

  private static LootLockPlayerData newDataWithProfiles(boolean... enabledStates) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    List<LootLockProfile> profiles = new ArrayList<>();
    int index = 0;
    for (boolean enabled : enabledStates) {
      profiles.add(
          new LootLockProfile(
              UUID.randomUUID(),
              "Profile " + index++,
              FilterMode.DENYLIST,
              RejectedItemAction.LEAVE_ON_GROUND,
              enabled,
              Collections.emptyList()));
    }
    data.setProfiles(profiles);
    return data;
  }
}
