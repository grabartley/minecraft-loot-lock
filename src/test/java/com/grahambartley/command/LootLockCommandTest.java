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

class LootLockCommandTest {

  @Test
  void modeTokenMapsEnumToCommandToken() {
    assertEquals("denylist", LootLockCommand.modeToken(FilterMode.DENYLIST));
    assertEquals("allowlist", LootLockCommand.modeToken(FilterMode.ALLOWLIST));
  }

  @Test
  void actionTokenMapsEnumToCommandToken() {
    assertEquals("leave", LootLockCommand.actionToken(RejectedItemAction.LEAVE_ON_GROUND));
    assertEquals("delete", LootLockCommand.actionToken(RejectedItemAction.DELETE));
  }

  @Test
  void normalizeProfileNameTrimsAndValidatesLength() {
    assertEquals("Mining", LootLockCommand.normalizeProfileName("  Mining  "));
    assertNull(LootLockCommand.normalizeProfileName("   "));
    assertNull(LootLockCommand.normalizeProfileName("x".repeat(33)));
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

  @Test
  void containsRuleReturnsTrueWhenItemExists() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")));

    assertTrue(LootLockCommand.containsRule(profile, "minecraft:stone"));
    assertFalse(LootLockCommand.containsRule(profile, "minecraft:diamond"));
  }

  @Test
  void normalizeRejectedItemActionRespectsPolicyAndNullSafety() {
    assertEquals(
        RejectedItemAction.LEAVE_ON_GROUND,
        LootLockCommand.normalizeRejectedItemAction(RejectedItemAction.DELETE, false));
    assertEquals(
        RejectedItemAction.DELETE,
        LootLockCommand.normalizeRejectedItemAction(RejectedItemAction.DELETE, true));
    assertEquals(
        RejectedItemAction.LEAVE_ON_GROUND,
        LootLockCommand.normalizeRejectedItemAction(null, true));
  }
}
