package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
  void withRuleRemovedRemovesMatchingItem() {
    List<RuleEntry> rules =
        RuleListController.withRuleRemoved(
            List.of(new RuleEntry("minecraft:stone"), new RuleEntry("minecraft:dirt")),
            "minecraft:stone");

    assertEquals(List.of("minecraft:dirt"), rules.stream().map(RuleEntry::itemId).toList());
  }
}
