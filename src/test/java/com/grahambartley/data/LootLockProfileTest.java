package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.api.PickupDecision;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class LootLockProfileTest {

  private static boolean evaluate(LootLockPlayerData data, Identifier itemId) {
    LootLockProfile profile = data.getActiveProfile().orElse(null);
    if (profile == null || !profile.isEnabled()) {
      return false;
    }
    boolean matched = profile.getCompiledRuleSet().contains(itemId);
    return profile.shouldReject(matched);
  }

  private static RejectedItemAction resolveAction(LootLockPlayerData data) {
    LootLockProfile profile = data.getActiveProfile().orElse(null);
    if (profile == null || !profile.isEnabled()) {
      return RejectedItemAction.LEAVE_ON_GROUND;
    }
    return profile.getRejectedItemAction();
  }

  @Test
  void denylistRejectsListedItem() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertTrue(evaluate(data, Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void denylistAcceptsUnlistedItem() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertFalse(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void allowlistAcceptsListedItem() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.ALLOWLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:diamond")));

    assertFalse(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void allowlistRejectsUnlistedItem() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.ALLOWLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:diamond")));

    assertTrue(evaluate(data, Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void disabledProfileAcceptsAll() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setEnabled(false);
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertFalse(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertFalse(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void nullActiveProfileAcceptsAll() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    data.setActiveProfileId(UUID.randomUUID());

    assertTrue(data.getActiveProfile().isEmpty());
    assertFalse(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertFalse(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void emptyDenylistAcceptsAll() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);

    assertTrue(data.getActiveProfile().orElseThrow().getCompiledRuleSet().isEmpty());
    assertFalse(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertFalse(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void emptyAllowlistRejectsAll() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.ALLOWLIST);

    assertTrue(profile.getCompiledRuleSet().isEmpty());
    assertTrue(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertTrue(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void profileDefaultsToDenylistEnabled() {
    LootLockProfile profile = LootLockProfile.createDefault();

    assertEquals(FilterMode.DENYLIST, profile.getMode());
    assertTrue(profile.isEnabled());
    assertEquals("Default", profile.getName());
    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, profile.getRejectedItemAction());
    assertTrue(profile.getRules().isEmpty());
  }

  @Test
  void fullPipelineIdentifierThroughProfileDecision() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setRules(
        List.of(
            new RuleEntry("minecraft:cobblestone"),
            new RuleEntry("minecraft:dirt"),
            new RuleEntry("minecraft:gravel")));

    assertTrue(profile.getCompiledRuleSet().contains(Identifier.tryParse("minecraft:cobblestone")));
    assertTrue(profile.getCompiledRuleSet().contains(Identifier.tryParse("minecraft:dirt")));
    assertTrue(profile.getCompiledRuleSet().contains(Identifier.tryParse("minecraft:gravel")));
    assertFalse(profile.getCompiledRuleSet().contains(Identifier.tryParse("minecraft:diamond")));

    assertTrue(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertFalse(evaluate(data, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void deleteActionUsedOnRejectWhenSet() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setRejectedItemAction(RejectedItemAction.DELETE);
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertTrue(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertEquals(RejectedItemAction.DELETE, resolveAction(data));
  }

  @Test
  void leaveOnGroundActionUsedOnRejectByDefault() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertTrue(evaluate(data, Identifier.tryParse("minecraft:dirt")));
    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, resolveAction(data));
  }

  @Test
  void leaveOnGroundActionReturnedWhenProfileDisabled() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setRejectedItemAction(RejectedItemAction.DELETE);
    profile.setEnabled(false);

    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, resolveAction(data));
  }

  @Test
  void leaveOnGroundActionReturnedWhenNoActiveProfile() {
    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    data.setActiveProfileId(UUID.randomUUID());

    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, resolveAction(data));
  }

  @Test
  void evaluateReturnsRejectLeaveForDenylistMatch() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertEquals(
        PickupDecision.REJECT_LEAVE, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void evaluateReturnsAllowForDenylistNonMatch() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsAllowForAllowlistMatch() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setMode(FilterMode.ALLOWLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:diamond")));

    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsRejectLeaveForAllowlistNonMatch() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setMode(FilterMode.ALLOWLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:diamond")));

    assertEquals(
        PickupDecision.REJECT_LEAVE, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void evaluateReturnsAllWhenProfileDisabled() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setEnabled(false);
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsRejectDeleteWhenActionIsDelete() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRejectedItemAction(RejectedItemAction.DELETE);
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertEquals(
        PickupDecision.REJECT_DELETE, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void evaluateReturnsRejectLeaveWhenActionIsLeave() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRejectedItemAction(RejectedItemAction.LEAVE_ON_GROUND);
    profile.setRules(List.of(new RuleEntry("minecraft:dirt")));

    assertEquals(
        PickupDecision.REJECT_LEAVE, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void evaluateEmptyDenylistReturnsAllow() {
    LootLockProfile profile = LootLockProfile.createDefault();

    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateEmptyAllowlistReturnsRejectLeave() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setMode(FilterMode.ALLOWLIST);

    assertEquals(
        PickupDecision.REJECT_LEAVE, profile.evaluate(Identifier.tryParse("minecraft:dirt")));
    assertEquals(
        PickupDecision.REJECT_LEAVE, profile.evaluate(Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateUnknownItemIdIsHandled() {
    LootLockProfile profile = LootLockProfile.createDefault();
    profile.setRules(List.of(new RuleEntry("oldmod:removed_item")));

    assertEquals(
        PickupDecision.REJECT_LEAVE, profile.evaluate(Identifier.tryParse("oldmod:removed_item")));
    assertEquals(PickupDecision.ALLOW, profile.evaluate(Identifier.tryParse("minecraft:diamond")));
  }
}
