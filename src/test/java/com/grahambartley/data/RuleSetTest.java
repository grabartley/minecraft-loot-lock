package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RuleSetTest {

  static Stream<Arguments> emptyResultCases() {
    Supplier<List<RuleEntry>> emptyList = () -> List.of();
    Supplier<List<RuleEntry>> nullList = () -> null;
    Supplier<List<RuleEntry>> entriesWithBlankItemIds =
        () -> List.of(new RuleEntry(""), new RuleEntry("   "));
    Supplier<List<RuleEntry>> singleNull =
        () -> {
          List<RuleEntry> list = new ArrayList<>();
          list.add(null);
          return list;
        };
    return Stream.of(
        Arguments.of("empty list", emptyList),
        Arguments.of("null list", nullList),
        Arguments.of("blank item ids", entriesWithBlankItemIds),
        Arguments.of("single null entry", singleNull));
  }

  @ParameterizedTest(name = "{0} compiles to empty rule set")
  @MethodSource("emptyResultCases")
  void emptyOrInvalidEntriesProduceEmptySet(String label, Supplier<List<RuleEntry>> entries) {
    assertTrue(RuleSet.fromRuleEntries(entries.get()).isEmpty());
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
            Arrays.asList(
                new RuleEntry("minecraft:dirt"),
                new RuleEntry("minecraft:dirt"),
                new RuleEntry("minecraft:dirt")));

    assertEquals(1, ruleSet.itemIds().size());
  }

  @Test
  void nullRuleEntryIsSkippedWhenMixedWithValid() {
    List<RuleEntry> entries = new ArrayList<>();
    entries.add(new RuleEntry("minecraft:dirt"));
    entries.add(null);

    RuleSet ruleSet = RuleSet.fromRuleEntries(entries);

    assertEquals(1, ruleSet.itemIds().size());
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:dirt")));
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

  @Test
  void tagRuleResolvesToUnionViaInjectedResolver() {
    List<RuleEntry> rules =
        List.of(new RuleEntry("#minecraft:flowers"), new RuleEntry("minecraft:stone"));
    java.util.function.Function<Identifier, java.util.Collection<Identifier>> resolver =
        tagId -> {
          if (Identifier.tryParse("minecraft:flowers").equals(tagId)) {
            return List.of(
                Identifier.tryParse("minecraft:poppy"), Identifier.tryParse("minecraft:dandelion"));
          }
          return List.of();
        };

    RuleSet ruleSet = RuleSet.fromRuleEntries(rules, resolver);

    assertEquals(3, ruleSet.itemIds().size());
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:poppy")));
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:dandelion")));
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:stone")));
  }

  @Test
  void unknownTagIsSkippedWithoutThrowing() {
    List<RuleEntry> rules =
        List.of(new RuleEntry("#unknown:tag"), new RuleEntry("minecraft:stone"));
    java.util.function.Function<Identifier, java.util.Collection<Identifier>> resolver =
        tagId -> List.of();

    RuleSet ruleSet = RuleSet.fromRuleEntries(rules, resolver);

    assertEquals(1, ruleSet.itemIds().size());
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:stone")));
  }

  @Test
  void overlappingTagRulesDoNotProduceDuplicates() {
    List<RuleEntry> rules =
        List.of(new RuleEntry("#minecraft:flowers"), new RuleEntry("#minecraft:wool"));
    java.util.function.Function<Identifier, java.util.Collection<Identifier>> resolver =
        tagId -> List.of(Identifier.tryParse("minecraft:poppy"));

    RuleSet ruleSet = RuleSet.fromRuleEntries(rules, resolver);

    assertEquals(1, ruleSet.itemIds().size());
    assertTrue(ruleSet.contains(Identifier.tryParse("minecraft:poppy")));
  }

  @Test
  void tagEntryWithInvalidTagPathIsSkipped() {
    List<RuleEntry> rules = List.of(new RuleEntry("#not a valid id"));
    java.util.function.Function<Identifier, java.util.Collection<Identifier>> resolver =
        tagId -> List.of(Identifier.tryParse("minecraft:stone"));

    RuleSet ruleSet = RuleSet.fromRuleEntries(rules, resolver);

    assertTrue(ruleSet.isEmpty());
  }

  @ParameterizedTest(name = "isTag(\"{0}\") -> {1}")
  @org.junit.jupiter.params.provider.CsvSource({
    "#minecraft:flowers, true",
    "minecraft:stone,    false",
    "'',                 false",
  })
  void ruleEntryClassifiesByPrefix(String token, boolean expected) {
    assertEquals(expected, new RuleEntry(token).isTag());
  }
}
