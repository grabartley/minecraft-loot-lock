package com.grahambartley.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.LootLock;
import com.grahambartley.api.PickupDecision;
import com.grahambartley.config.ConfigManager;
import com.grahambartley.config.LootLockConfig;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PickupGuardTest {

  @TempDir Path tempDir;

  @BeforeEach
  void resetServerPolicy() {
    LootLock.SERVER_CONFIG = LootLockConfig.defaults();
  }

  @Test
  void constructorCreatesGuard() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);
    assertNotNull(guard);
  }

  @Test
  void evaluateReturnsAllowWhenProfileIsDisabled() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setEnabled(false);

    assertEquals(
        PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsAllowWhenPlayerDataHasNoActiveProfile() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    playerData.setActiveProfileId(null);

    assertEquals(
        PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsRejectLeaveForDeniedItemInDenylist() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.DENYLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
    profile.compileRules();

    assertEquals(
        PickupDecision.REJECT_LEAVE,
        guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
  }

  @Test
  void evaluateReturnsAllowForUnlistedItemInDenylist() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.DENYLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
    profile.compileRules();

    assertEquals(
        PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsRejectDeleteForDeniedItemWithDeleteAction() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.DENYLIST);
    profile.setRejectedItemAction(RejectedItemAction.DELETE);
    profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
    profile.compileRules();

    assertEquals(
        PickupDecision.REJECT_DELETE,
        guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
  }

  @Test
  void evaluateDowngradesDeleteToLeaveWhenPolicyDisablesDelete() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    LootLock.SERVER_CONFIG = new LootLockConfig(false);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.DENYLIST);
    profile.setRejectedItemAction(RejectedItemAction.DELETE);
    profile.setRules(List.of(new RuleEntry("minecraft:cobblestone")));
    profile.compileRules();

    assertEquals(
        PickupDecision.REJECT_LEAVE,
        guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
  }

  @Test
  void evaluateReturnsAllowForListedItemInAllowlist() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.ALLOWLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:diamond")));
    profile.compileRules();

    assertEquals(
        PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
  }

  @Test
  void evaluateReturnsRejectLeaveForUnlistedItemInAllowlist() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setMode(FilterMode.ALLOWLIST);
    profile.setRules(List.of(new RuleEntry("minecraft:diamond")));
    profile.compileRules();

    assertEquals(
        PickupDecision.REJECT_LEAVE,
        guard.evaluate(playerUuid, Identifier.tryParse("minecraft:cobblestone")));
  }

  @Test
  void evaluateWithDisabledProfileReturnsAllowEvenForUnknownItemId() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setEnabled(false);
    profile.setRules(List.of(new RuleEntry("oldmod:removed_item")));
    profile.compileRules();

    assertEquals(
        PickupDecision.ALLOW,
        guard.evaluate(playerUuid, Identifier.tryParse("oldmod:removed_item")));
  }

  @Test
  void evaluateWithUnknownItemIdDoesNotMatch() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    LootLockPlayerData playerData = dataManager.getOrLoad(playerUuid);
    LootLockProfile profile = playerData.getActiveProfile().orElseThrow();
    profile.setRules(List.of(new RuleEntry("oldmod:removed_item")));
    profile.compileRules();

    assertEquals(
        PickupDecision.ALLOW, guard.evaluate(playerUuid, Identifier.tryParse("minecraft:diamond")));
    assertEquals(
        PickupDecision.REJECT_LEAVE,
        guard.evaluate(playerUuid, Identifier.tryParse("oldmod:removed_item")));
  }

  @Test
  void tryNotifyWithNullStackReturnsFalseAndDoesNotStampCooldown() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();

    assertFalse(guard.tryNotify(playerUuid, null, false, 100));
    assertFalse(guard.hasNotificationCooldown(playerUuid));
  }

  @Test
  void tryNotifyRespectsCooldownWhenStamped() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    guard.stampNotificationCooldown(playerUuid, 100);

    assertFalse(guard.tryNotify(playerUuid, null, false, 100));
  }

  @Test
  void recordBlockedCollisionAggregatesDuringCooldown() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    Identifier cobblestone = Identifier.of("minecraft", "cobblestone");

    List<PickupGuard.BlockedNotice> first =
        guard.recordBlockedCollision(playerUuid, cobblestone, 1, false, 100);
    assertEquals(1, first.size());
    assertEquals(1, first.get(0).count());

    List<PickupGuard.BlockedNotice> second =
        guard.recordBlockedCollision(playerUuid, cobblestone, 2, false, 110);
    assertTrue(second.isEmpty());

    List<PickupGuard.BlockedNotice> third =
        guard.recordBlockedCollision(playerUuid, cobblestone, 3, false, 140);
    assertEquals(1, third.size());
    assertEquals(5, third.get(0).count());
  }

  @Test
  void tryNotifyWithNullStackStaysFalseAfterCooldownExpires() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    guard.stampNotificationCooldown(playerUuid, 100);

    assertFalse(guard.tryNotify(playerUuid, null, false, 140));
  }

  @Test
  void clearNotificationCooldownRemovesStampedTracking() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    guard.stampNotificationCooldown(playerUuid, 100);

    assertTrue(guard.hasNotificationCooldown(playerUuid));
    guard.clearNotificationCooldown(playerUuid);
    assertFalse(guard.hasNotificationCooldown(playerUuid));
  }

  @Test
  void clearNotificationCooldownOnUnknownPlayerDoesNotThrow() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    guard.clearNotificationCooldown(UUID.randomUUID());
  }

  @Test
  void stampNotificationCooldownTracksTick() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerUuid = UUID.randomUUID();
    guard.stampNotificationCooldown(playerUuid, 42);

    assertTrue(guard.hasNotificationCooldown(playerUuid));
    assertEquals(42, guard.getNotificationCooldownTick(playerUuid));
  }

  @Test
  void notificationCooldownsArePerPlayer() {
    ConfigManager configManager = new ConfigManager(tempDir);
    ServerPlayerDataManager dataManager = new ServerPlayerDataManager(configManager);
    PickupGuard guard = new PickupGuard(dataManager);

    UUID playerA = UUID.randomUUID();
    UUID playerB = UUID.randomUUID();
    guard.stampNotificationCooldown(playerA, 100);

    assertTrue(guard.hasNotificationCooldown(playerA));
    assertFalse(guard.hasNotificationCooldown(playerB));
  }
}
