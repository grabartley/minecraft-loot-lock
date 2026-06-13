package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

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

  @Test
  void setEnabledForAllFlipsEveryProfile() {
    LootLockPlayerData data = newDataWithProfiles(true, false, true);

    data.setEnabledForAll(false);

    for (LootLockProfile profile : data.getProfiles()) {
      assertFalse(profile.isEnabled());
    }

    data.setEnabledForAll(true);

    for (LootLockProfile profile : data.getProfiles()) {
      assertTrue(profile.isEnabled());
    }
  }

  @Test
  void setEnabledForAllTolerantOfNullEntries() {
    LootLockPlayerData data = newDataWithProfiles(true, true);
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.add(null);
    // setProfiles rejects an empty list but does not strip nulls. Inject directly.
    data.setProfiles(profiles);

    data.setEnabledForAll(false);

    for (LootLockProfile profile : data.getProfiles()) {
      if (profile != null) {
        assertFalse(profile.isEnabled());
      }
    }
  }

  @Test
  void isGloballyEnabledTrueWhenEveryProfileEnabled() {
    LootLockPlayerData data = newDataWithProfiles(true, true, true);

    assertTrue(data.isGloballyEnabled());
  }

  @Test
  void isGloballyEnabledFalseWhenAnyProfileDisabled() {
    LootLockPlayerData data = newDataWithProfiles(true, false, true);

    assertFalse(data.isGloballyEnabled());
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
      LootLockProfile profile =
          new LootLockProfile(
              UUID.randomUUID(),
              "Profile " + index++,
              FilterMode.DENYLIST,
              RejectedItemAction.LEAVE_ON_GROUND,
              enabled,
              Collections.emptyList());
      profiles.add(profile);
    }
    data.setProfiles(profiles);
    return data;
  }
}
