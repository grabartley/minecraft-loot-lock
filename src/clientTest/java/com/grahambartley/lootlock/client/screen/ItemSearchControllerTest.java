package com.grahambartley.lootlock.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ItemSearchControllerTest {
  private static final List<ItemSearchController.ItemCandidate> ITEMS =
      List.of(
          new ItemSearchController.ItemCandidate("minecraft:stone", "Stone", "minecraft", null),
          new ItemSearchController.ItemCandidate("minecraft:apple", "Apple", "minecraft", null),
          new ItemSearchController.ItemCandidate("create:zinc_ore", "Zinc Ore", "create", null),
          new ItemSearchController.ItemCandidate("create:andesite", "Andesite", "create", null),
          new ItemSearchController.ItemCandidate(
              "minecraft:diamond", "Diamond", "minecraft", null));

  @ParameterizedTest
  @MethodSource("queriesWithExpectedResults")
  void filterMatchesExpectedItems(String query, int expectedCount, String expectedFirstId) {
    List<ItemSearchController.ItemCandidate> results = ItemSearchController.filter(ITEMS, query);

    assertEquals(expectedCount, results.size());
    if (!results.isEmpty()) {
      assertEquals(expectedFirstId, results.get(0).itemId());
    }
  }

  static Stream<Arguments> queriesWithExpectedResults() {
    return Stream.of(
        Arguments.of("", 5, "create:andesite"),
        Arguments.of("stone", 1, "minecraft:stone"),
        Arguments.of("minecraft", 3, "minecraft:apple"),
        Arguments.of("create", 2, "create:andesite"),
        Arguments.of("diAMOND", 1, "minecraft:diamond"),
        Arguments.of("nonexistent", 0, ""),
        Arguments.of("zinc ore", 1, "create:zinc_ore"),
        Arguments.of("create zinc", 1, "create:zinc_ore"),
        Arguments.of("zinc create", 1, "create:zinc_ore"));
  }

  @Test
  void filterSortsByItemId() {
    List<ItemSearchController.ItemCandidate> source =
        List.of(
            new ItemSearchController.ItemCandidate("minecraft:stone", "Stone", "minecraft", null),
            new ItemSearchController.ItemCandidate("minecraft:apple", "Apple", "minecraft", null));

    List<ItemSearchController.ItemCandidate> filtered =
        ItemSearchController.filter(source, "minecraft");

    assertEquals(2, filtered.size());
    assertEquals("minecraft:apple", filtered.get(0).itemId());
    assertEquals("minecraft:stone", filtered.get(1).itemId());
  }

  @Test
  void filterMatchesAcrossSeparatorsAndTokens() {
    List<ItemSearchController.ItemCandidate> source =
        List.of(
            new ItemSearchController.ItemCandidate("create:zinc_ore", "Zinc Ore", "create", null),
            new ItemSearchController.ItemCandidate("minecraft:stone", "Stone", "minecraft", null));

    List<ItemSearchController.ItemCandidate> filtered =
        ItemSearchController.filter(source, "create zinc ore");

    assertEquals(1, filtered.size());
    assertEquals("create:zinc_ore", filtered.get(0).itemId());
  }
}
