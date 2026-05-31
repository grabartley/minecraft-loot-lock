package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class ItemSearchControllerTest {
  @Test
  void filterMatchesDisplayNameAndNamespace() {
    List<ItemSearchController.ItemCandidate> source =
        List.of(
            new ItemSearchController.ItemCandidate("minecraft:stone", "Stone", "minecraft"),
            new ItemSearchController.ItemCandidate("create:zinc_ore", "Zinc Ore", "create"));

    List<ItemSearchController.ItemCandidate> byName = ItemSearchController.filter(source, "zinc");
    List<ItemSearchController.ItemCandidate> byNamespace =
        ItemSearchController.filter(source, "create");

    assertEquals(1, byName.size());
    assertEquals("create:zinc_ore", byName.get(0).itemId());
    assertEquals(1, byNamespace.size());
  }

  @Test
  void filterSortsByItemId() {
    List<ItemSearchController.ItemCandidate> source =
        List.of(
            new ItemSearchController.ItemCandidate("minecraft:stone", "Stone", "minecraft"),
            new ItemSearchController.ItemCandidate("minecraft:apple", "Apple", "minecraft"));

    List<ItemSearchController.ItemCandidate> filtered =
        ItemSearchController.filter(source, "minecraft");

    assertEquals("minecraft:apple", filtered.get(0).itemId());
    assertTrue(filtered.size() == 2);
  }
}
