package com.grahambartley.lootlock.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.api.PickupDecision;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LootLockProfileTest {

  private static final Identifier DIRT = Identifier.tryParse("minecraft:dirt");
  private static final Identifier DIAMOND = Identifier.tryParse("minecraft:diamond");
  private static final Identifier UNKNOWN = Identifier.tryParse("oldmod:removed_item");

  static Stream<Arguments> evaluateCases() {
    return Stream.of(
        Arguments.of(
            "denylist match -> REJECT_LEAVE",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:dirt")),
            DIRT,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "denylist non-match -> ALLOW",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:dirt")),
            DIAMOND,
            PickupDecision.ALLOW),
        Arguments.of(
            "allowlist match -> ALLOW",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:diamond")),
            DIAMOND,
            PickupDecision.ALLOW),
        Arguments.of(
            "allowlist non-match -> REJECT_LEAVE",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:diamond")),
            DIRT,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "disabled denylist -> ALLOW",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            false,
            List.of(new RuleEntry("minecraft:dirt")),
            DIRT,
            PickupDecision.ALLOW),
        Arguments.of(
            "disabled allowlist -> ALLOW",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            false,
            List.of(new RuleEntry("minecraft:diamond")),
            DIAMOND,
            PickupDecision.ALLOW),
        Arguments.of(
            "denylist delete action match -> REJECT_DELETE",
            FilterMode.DENYLIST,
            RejectedItemAction.DELETE,
            true,
            List.of(new RuleEntry("minecraft:dirt")),
            DIRT,
            PickupDecision.REJECT_DELETE),
        Arguments.of(
            "denylist leave action match -> REJECT_LEAVE",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:dirt")),
            DIRT,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "empty denylist -> ALLOW",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(),
            DIRT,
            PickupDecision.ALLOW),
        Arguments.of(
            "empty allowlist -> REJECT_LEAVE",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(),
            DIRT,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "unknown item id matches denylist entry -> REJECT_LEAVE",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("oldmod:removed_item")),
            UNKNOWN,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "unknown item id non-match denylist -> ALLOW",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("oldmod:removed_item")),
            DIAMOND,
            PickupDecision.ALLOW));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("evaluateCases")
  void evaluateReturnsExpectedDecision(
      String label,
      FilterMode mode,
      RejectedItemAction action,
      boolean enabled,
      List<RuleEntry> rules,
      Identifier itemId,
      PickupDecision expected) {
    LootLockProfile profile = newProfile(mode, action, enabled, rules);

    assertEquals(expected, profile.evaluate(itemId));
  }

  @Test
  void createDefaultBuildsDenylistEnabledLeaveProfile() {
    LootLockProfile profile = LootLockProfile.createDefault();

    assertEquals(FilterMode.DENYLIST, profile.getMode());
    assertTrue(profile.isEnabled());
    assertEquals("Default", profile.getName());
    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, profile.getRejectedItemAction());
    assertTrue(profile.getRules().isEmpty());
  }

  @Test
  void compiledRuleSetContainsConfiguredItemIds() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(
        List.of(
            new RuleEntry("minecraft:cobblestone"),
            new RuleEntry("minecraft:dirt"),
            new RuleEntry("minecraft:gravel")));

    RuleSet ruleSet = profile.getCompiledRuleSet();
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:cobblestone")));
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:dirt")));
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:gravel")));
    assertFalse(ruleSet.contains(Identifier.tryParse("minecraft:diamond")));
  }

  private static LootLockProfile newProfile(
      FilterMode mode, RejectedItemAction action, boolean enabled, List<RuleEntry> rules) {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setMode(mode);
    profile.setRejectedItemAction(action);
    profile.setEnabled(enabled);
    profile.setRules(rules);
    return profile;
  }
}
