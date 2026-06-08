package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Set;
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

    assertEquals("minecraft:apple", filtered.get(0).itemId());
    assertTrue(filtered.size() == 2);
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

  @Test
  void filterExcludesItemsAlreadyInActiveProfileRules() {
    List<ItemSearchController.ItemCandidate> filtered =
        ItemSearchController.filter(ITEMS, "", Set.of("minecraft:apple", "create:zinc_ore"));

    assertEquals(3, filtered.size());
    assertEquals(
        List.of("create:andesite", "minecraft:diamond", "minecraft:stone"),
        filtered.stream().map(ItemSearchController.ItemCandidate::itemId).toList());
  }

  @Test
  void filterPreservesSearchBehaviorAfterExcludingExistingRules() {
    List<ItemSearchController.ItemCandidate> filtered =
        ItemSearchController.filter(ITEMS, "minecraft", Set.of("minecraft:apple"));

    assertEquals(2, filtered.size());
    assertEquals(
        List.of("minecraft:diamond", "minecraft:stone"),
        filtered.stream().map(ItemSearchController.ItemCandidate::itemId).toList());
  }

  @Test
  void selectControlClickAddsAndRemovesWithoutClearingOtherSelections() {
    ItemSearchController.SelectionState added =
        ItemSearchController.select(ITEMS, List.of("minecraft:apple"), 1, 3, true, false);

    assertEquals(List.of("minecraft:apple", "create:andesite"), added.selectedItemIds());
    assertEquals(3, added.lastClickedIndex());

    ItemSearchController.SelectionState removed =
        ItemSearchController.select(ITEMS, added.selectedItemIds(), 3, 1, true, false);

    assertEquals(List.of("create:andesite"), removed.selectedItemIds());
    assertEquals(1, removed.lastClickedIndex());
  }

  @Test
  void selectShiftClickBuildsRangeFromLastClickedIndex() {
    ItemSearchController.SelectionState range =
        ItemSearchController.select(ITEMS, List.of("minecraft:apple"), 1, 3, false, true);

    assertEquals(
        List.of("minecraft:apple", "create:zinc_ore", "create:andesite"), range.selectedItemIds());
    assertEquals(3, range.lastClickedIndex());
  }

  @Test
  void retainVisibleSelectionPreservesSelectionsAcrossPagesOfSameResultSet() {
    List<String> retained =
        ItemSearchController.retainVisibleSelection(
            ITEMS, List.of("minecraft:apple", "minecraft:diamond"));

    assertEquals(List.of("minecraft:apple", "minecraft:diamond"), retained);
  }

  @Test
  void selectedItemIdsInVisibleOrderIncludesSelectionsFromMultiplePages() {
    List<String> ordered =
        ItemSearchController.selectedItemIdsInVisibleOrder(
            ITEMS, List.of("minecraft:diamond", "minecraft:apple"));

    assertEquals(List.of("minecraft:apple", "minecraft:diamond"), ordered);
  }
}
