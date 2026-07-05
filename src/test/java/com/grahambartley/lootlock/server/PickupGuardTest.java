package com.grahambartley.lootlock.server;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.api.PickupDecision;
import com.grahambartley.lootlock.config.LootLockConfig;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PickupGuardTest {

  private static final Identifier COBBLESTONE = Identifier.tryParse("minecraft:cobblestone");
  private static final Identifier DIAMOND = Identifier.tryParse("minecraft:diamond");
  private static final Identifier UNKNOWN = Identifier.tryParse("oldmod:removed_item");

  @Mock private ServerPlayerDataManager playerDataManager;

  private PickupGuard guard;
  private UUID playerUuid;

  @BeforeEach
  void setUp() {
    LootLock.SERVER_CONFIG = LootLockConfig.defaults();
    guard = new PickupGuard(playerDataManager);
    playerUuid = UUID.randomUUID();
  }

  static Stream<Arguments> evaluateCases() {
    return Stream.of(
        Arguments.of(
            "disabled profile -> allow",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            false,
            List.of(),
            DIAMOND,
            true,
            PickupDecision.ALLOW),
        Arguments.of(
            "denylist match -> reject_leave",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:cobblestone")),
            COBBLESTONE,
            true,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "denylist non-match -> allow",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:cobblestone")),
            DIAMOND,
            true,
            PickupDecision.ALLOW),
        Arguments.of(
            "denylist delete-action match -> reject_delete",
            FilterMode.DENYLIST,
            RejectedItemAction.DELETE,
            true,
            List.of(new RuleEntry("minecraft:cobblestone")),
            COBBLESTONE,
            true,
            PickupDecision.REJECT_DELETE),
        Arguments.of(
            "delete downgraded when policy disables delete",
            FilterMode.DENYLIST,
            RejectedItemAction.DELETE,
            true,
            List.of(new RuleEntry("minecraft:cobblestone")),
            COBBLESTONE,
            false,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "allowlist match -> allow",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:diamond")),
            DIAMOND,
            true,
            PickupDecision.ALLOW),
        Arguments.of(
            "allowlist non-match -> reject_leave",
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("minecraft:diamond")),
            COBBLESTONE,
            true,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "disabled profile still allows even with unknown rule item",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            false,
            List.of(new RuleEntry("oldmod:removed_item")),
            UNKNOWN,
            true,
            PickupDecision.ALLOW),
        Arguments.of(
            "unknown item id matched via denylist -> reject_leave",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("oldmod:removed_item")),
            UNKNOWN,
            true,
            PickupDecision.REJECT_LEAVE),
        Arguments.of(
            "unknown item id unmatched via denylist -> allow",
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of(new RuleEntry("oldmod:removed_item")),
            DIAMOND,
            true,
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
      boolean allowDeletePolicy,
      PickupDecision expected) {
    LootLock.SERVER_CONFIG = new LootLockConfig(allowDeletePolicy);
    LootLockPlayerData data = createPlayerData(mode, action, enabled, rules);
    when(playerDataManager.getOrLoad(playerUuid)).thenReturn(data);

    assertEquals(expected, guard.evaluate(playerUuid, itemId));
  }

  @Test
  void evaluateReturnsAllowWhenNoActiveProfile() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    data.setActiveProfileId(null);
    when(playerDataManager.getOrLoad(playerUuid)).thenReturn(data);

    assertEquals(PickupDecision.ALLOW, guard.evaluate(playerUuid, DIAMOND));
  }

  @Test
  void tryNotifyReturnsFalseForNullStack() {
    assertFalse(guard.tryNotify(playerUuid, null, false, 100));
    assertFalse(guard.hasNotificationCooldown(playerUuid));
  }

  @Test
  void tryNotifyReturnsFalseAfterCooldownStamped() {
    guard.stampNotificationCooldown(playerUuid, 100);

    assertFalse(guard.tryNotify(playerUuid, null, false, 100));
    assertFalse(guard.tryNotify(playerUuid, null, false, 140));
  }

  @Test
  void recordBlockedCollisionAggregatesDuringCooldown() {
    List<PickupGuard.BlockedNotice> first =
        guard.recordBlockedCollision(playerUuid, COBBLESTONE, 1, false, 100);
    assertEquals(1, first.size());
    assertEquals(1, first.get(0).count());

    List<PickupGuard.BlockedNotice> second =
        guard.recordBlockedCollision(playerUuid, COBBLESTONE, 2, false, 110);
    assertTrue(second.isEmpty());

    List<PickupGuard.BlockedNotice> third =
        guard.recordBlockedCollision(playerUuid, COBBLESTONE, 3, false, 140);
    assertEquals(1, third.size());
    assertEquals(5, third.get(0).count());
  }

  @Test
  void clearNotificationCooldownRemovesStampedTracking() {
    guard.stampNotificationCooldown(playerUuid, 100);
    assertTrue(guard.hasNotificationCooldown(playerUuid));

    guard.clearNotificationCooldown(playerUuid);

    assertFalse(guard.hasNotificationCooldown(playerUuid));
  }

  @Test
  void clearNotificationCooldownOnUnknownPlayerDoesNotThrow() {
    guard.clearNotificationCooldown(UUID.randomUUID());

    assertFalse(guard.hasNotificationCooldown(playerUuid));
  }

  @Test
  void stampNotificationCooldownTracksTick() {
    guard.stampNotificationCooldown(playerUuid, 42);

    assertTrue(guard.hasNotificationCooldown(playerUuid));
    assertEquals(42, guard.getNotificationCooldownTick(playerUuid));
  }

  @Test
  void notificationCooldownsArePerPlayer() {
    UUID otherPlayer = UUID.randomUUID();
    guard.stampNotificationCooldown(playerUuid, 100);

    assertTrue(guard.hasNotificationCooldown(playerUuid));
    assertFalse(guard.hasNotificationCooldown(otherPlayer));
  }

  private LootLockPlayerData createPlayerData(
      FilterMode mode, RejectedItemAction action, boolean enabled, List<RuleEntry> rules) {
    LootLockPlayerData data = LootLockPlayerData.createDefault(playerUuid);
    LootLockProfile profile = data.getActiveProfile().orElseThrow();
    profile.setMode(mode);
    profile.setRejectedItemAction(action);
    profile.setEnabled(enabled);
    profile.setRules(rules);
    profile.compileRules();
    return data;
  }
}
