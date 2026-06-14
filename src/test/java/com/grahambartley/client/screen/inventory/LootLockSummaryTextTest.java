package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class LootLockSummaryTextTest {

  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<Arguments> summaryCases() {
    return Stream.of(
        Arguments.of(
            "globally off",
            false,
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            3,
            "Loot Lock is off, every item is picked up normally for all profiles."),
        Arguments.of(
            "denylist empty",
            true,
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            0,
            "Denylist, nothing is filtered yet, add items below to skip them."),
        Arguments.of(
            "denylist populated with leave action",
            true,
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            2,
            "Denylist, 2 items are skipped and left on the ground."),
        Arguments.of(
            "denylist populated with delete action (singular)",
            true,
            FilterMode.DENYLIST,
            RejectedItemAction.DELETE,
            1,
            "Denylist, 1 item is skipped and deleted."),
        Arguments.of(
            "allowlist empty",
            true,
            FilterMode.ALLOWLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            0,
            "Allowlist, no items allowed yet, you will keep nothing."),
        Arguments.of(
            "allowlist populated with delete action",
            true,
            FilterMode.ALLOWLIST,
            RejectedItemAction.DELETE,
            4,
            "Allowlist, only these 4 items are picked up, everything else is deleted."));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("summaryCases")
  void buildRendersExpectedText(
      String label,
      boolean enabled,
      FilterMode mode,
      RejectedItemAction action,
      int ruleCount,
      String expected) {
    LootLockProfile profile = newProfile(mode, action, ruleCount);

    assertEquals(expected, LootLockSummaryText.build(enabled, profile).getString());
  }

  @Test
  void nullProfileWhenEnabledReturnsNoActiveProfileMessage() {
    assertEquals("No active profile.", LootLockSummaryText.build(true, null).getString());
  }

  private static LootLockProfile newProfile(
      FilterMode mode, RejectedItemAction action, int ruleCount) {
    RuleEntry[] rules = new RuleEntry[ruleCount];
    for (int i = 0; i < ruleCount; i++) {
      rules[i] = new RuleEntry("minecraft:item_" + i);
    }
    return new LootLockProfile(UUID.randomUUID(), "Test", mode, action, true, List.of(rules));
  }
}
