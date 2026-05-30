package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class RuleSetTest {

  @Test
  void emptyRulesProducesEmptySet() {
    RuleSet ruleSet = RuleSet.fromRuleEntries(List.of());
    assertTrue(ruleSet.isEmpty());
  }

  @Test
  void nullRulesProducesEmptySet() {
    RuleSet ruleSet = RuleSet.fromRuleEntries(null);
    assertTrue(ruleSet.isEmpty());
  }

  @Test
  void compilesValidIdentifiers() {
    RuleSet ruleSet =
        RuleSet.fromRuleEntries(
            List.of(new RuleEntry("minecraft:dirt"), new RuleEntry("minecraft:stone")));

    assertFalse(ruleSet.isEmpty());
    assertEquals(2, ruleSet.itemIds().size());
  }

  @Test
  void containsByIdentifier() {
    RuleSet ruleSet = RuleSet.fromRuleEntries(List.of(new RuleEntry("minecraft:diamond")));

    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:diamond")));
    assertFalse(ruleSet.contains(Identifier.tryParse("minecraft:dirt")));
  }

  @Test
  void invalidIdentifierIsSkipped() {
    RuleSet ruleSet =
        RuleSet.fromRuleEntries(
            List.of(new RuleEntry("has space"), new RuleEntry("minecraft:stone")));

    assertEquals(1, ruleSet.itemIds().size());
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:stone")));
  }

  @Test
  void duplicateEntriesAreDeduplicated() {
    RuleSet ruleSet =
        RuleSet.fromRuleEntries(
            List.of(
                new RuleEntry("minecraft:dirt"),
                new RuleEntry("minecraft:dirt"),
                new RuleEntry("minecraft:dirt")));

    assertEquals(1, ruleSet.itemIds().size());
  }

  @Test
  void nullRuleEntryIsSkipped() {
    List<RuleEntry> entries = new ArrayList<>();
    entries.add(new RuleEntry("minecraft:dirt"));
    entries.add(null);
    RuleSet ruleSet = RuleSet.fromRuleEntries(entries);

    assertEquals(1, ruleSet.itemIds().size());
  }

  @Test
  void ruleEntryWithBlankItemIdIsSkipped() {
    RuleSet ruleSet =
        RuleSet.fromRuleEntries(
            List.of(new RuleEntry(""), new RuleEntry("   "), new RuleEntry("minecraft:dirt")));

    assertEquals(1, ruleSet.itemIds().size());
  }

  @Test
  void unknownItemIdIsRetainedAsIdentifier() {
    RuleSet ruleSet = RuleSet.fromRuleEntries(List.of(new RuleEntry("oldmod:removed_item")));

    assertFalse(ruleSet.isEmpty());
    assertTrue(ruleSet.contains(Identifier.tryParse("oldmod:removed_item")));
  }

  @Test
  void emptyConstantIsShared() {
    assertTrue(RuleSet.empty().isEmpty());
  }
}
