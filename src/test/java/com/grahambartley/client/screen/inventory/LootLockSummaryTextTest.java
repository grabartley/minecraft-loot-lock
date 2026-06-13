package com.grahambartley.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.List;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.text.MutableText;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LootLockSummaryTextTest {
  @BeforeAll
  static void bootstrapMinecraft() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void describesOffStateWhenGloballyDisabled() {
    LootLockProfile profile =
        newProfile(FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, 3);

    MutableText text = LootLockSummaryText.build(false, profile);

    assertNotNull(text);
    String rendered = text.getString();
    assertTrue(rendered.contains("off"), rendered);
    assertTrue(rendered.toLowerCase().contains("picked up normally"), rendered);
  }

  @Test
  void describesDenylistEmpty() {
    LootLockProfile profile =
        newProfile(FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, 0);

    String rendered = LootLockSummaryText.build(true, profile).getString();

    assertTrue(rendered.startsWith("Denylist"), rendered);
    assertTrue(rendered.contains("nothing is filtered yet"), rendered);
  }

  @Test
  void describesDenylistPopulatedWithLeaveAction() {
    LootLockProfile profile =
        newProfile(FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, 2);

    String rendered = LootLockSummaryText.build(true, profile).getString();

    assertTrue(rendered.contains("2 items"), rendered);
    assertTrue(rendered.contains("skipped"), rendered);
    assertTrue(rendered.contains("left on the ground"), rendered);
  }

  @Test
  void describesDenylistPopulatedWithDeleteAction() {
    LootLockProfile profile = newProfile(FilterMode.DENYLIST, RejectedItemAction.DELETE, 1);

    String rendered = LootLockSummaryText.build(true, profile).getString();

    assertTrue(rendered.contains("1 item "), rendered);
    assertTrue(rendered.contains("is skipped"), rendered);
    assertTrue(rendered.contains("deleted"), rendered);
  }

  @Test
  void describesAllowlistEmpty() {
    LootLockProfile profile =
        newProfile(FilterMode.ALLOWLIST, RejectedItemAction.LEAVE_ON_GROUND, 0);

    String rendered = LootLockSummaryText.build(true, profile).getString();

    assertTrue(rendered.startsWith("Allowlist"), rendered);
    assertTrue(rendered.contains("no items allowed yet"), rendered);
  }

  @Test
  void describesAllowlistPopulatedWithDeleteAction() {
    LootLockProfile profile = newProfile(FilterMode.ALLOWLIST, RejectedItemAction.DELETE, 4);

    String rendered = LootLockSummaryText.build(true, profile).getString();

    assertTrue(rendered.startsWith("Allowlist"), rendered);
    assertTrue(rendered.contains("only these 4 items"), rendered);
    assertTrue(rendered.contains("deleted"), rendered);
  }

  @Test
  void nullProfileWhenEnabledRendersGracefully() {
    String rendered = LootLockSummaryText.build(true, null).getString();

    assertTrue(rendered.toLowerCase().contains("no active profile"), rendered);
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
