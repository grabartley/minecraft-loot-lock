package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.RuleEntry;
import java.util.List;
import org.junit.jupiter.api.Test;

class RuleListControllerTest {
  @Test
  void dedupeRulesKeepsFirstOrder() {
    List<RuleEntry> deduped =
        RuleListController.dedupeRules(
            List.of(
                new RuleEntry("minecraft:stone"),
                new RuleEntry("minecraft:dirt"),
                new RuleEntry("minecraft:stone")));

    assertEquals(
        List.of("minecraft:stone", "minecraft:dirt"),
        deduped.stream().map(RuleEntry::itemId).toList());
  }

  @Test
  void unresolvedRulesReturnsOnlyMissing() {
    List<RuleEntry> unresolved =
        RuleListController.unresolvedRules(
            List.of(new RuleEntry("minecraft:stone"), new RuleEntry("mod:ghost")),
            id -> id.equals("minecraft:stone"));

    assertEquals(1, unresolved.size());
    assertEquals("mod:ghost", unresolved.get(0).itemId());
  }

  @Test
  void withRuleAddedPreventsDuplicates() {
    List<RuleEntry> rules =
        RuleListController.withRuleAdded(
            List.of(new RuleEntry("minecraft:stone")), "minecraft:stone");

    assertEquals(1, rules.size());
  }

  @Test
  void withRulesAddedAppendsNewRulesOnceInSelectionOrder() {
    List<RuleEntry> rules =
        RuleListController.withRulesAdded(
            List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")),
            List.of("minecraft:dirt", "minecraft:diamond", "minecraft:stone", "minecraft:apple"));

    assertEquals(
        List.of("minecraft:stone", "minecraft:dirt", "minecraft:diamond", "minecraft:apple"),
        rules.stream().map(RuleEntry::itemId).toList());
  }

  @Test
  void filterRulesMatchesBySubstring() {
    List<RuleEntry> filtered =
        RuleListController.filterRules(
            List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:diamond")), "dia");

    assertEquals(1, filtered.size());
    assertTrue(filtered.get(0).itemId().contains("diamond"));
  }

  @Test
  void toggleRuleAddsWhenMissingAndRemovesWhenPresent() {
    List<RuleEntry> added = RuleListController.toggleRule(List.of(), "minecraft:stone");
    List<RuleEntry> removed = RuleListController.toggleRule(added, "minecraft:stone");

    assertEquals(1, added.size());
    assertTrue(removed.isEmpty());
  }
}
