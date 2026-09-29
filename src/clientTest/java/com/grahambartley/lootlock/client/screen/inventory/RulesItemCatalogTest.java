package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import com.grahambartley.lootlock.client.screen.UnobtainableItems;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RulesItemCatalogTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void catalogCoversExactlyTheObtainableRegistryItems() {
    List<String> expectedIds =
        Registries.ITEM.stream()
            .filter(item -> !UnobtainableItems.isUnobtainable(item))
            .map(item -> Registries.ITEM.getId(item).toString())
            .toList();

    List<String> actualIds = RulesItemCatalog.all().stream().map(ItemCandidate::itemId).toList();

    assertEquals(expectedIds, actualIds);
  }

  static Stream<Arguments> knownItems() {
    return Stream.of(
        Arguments.of(Items.STONE, true),
        Arguments.of(Items.DIAMOND, true),
        Arguments.of(Items.AIR, false),
        Arguments.of(Items.BEDROCK, false),
        Arguments.of(Items.KNOWLEDGE_BOOK, false));
  }

  @ParameterizedTest(name = "{0} listed={1}")
  @MethodSource("knownItems")
  void candidatesDescribeTheirItem(Item item, boolean expectedListed) {
    String itemId = Registries.ITEM.getId(item).toString();
    Map<String, ItemCandidate> byId =
        RulesItemCatalog.all().stream()
            .collect(Collectors.toMap(ItemCandidate::itemId, Function.identity()));

    assertEquals(expectedListed, byId.containsKey(itemId));
    if (expectedListed) {
      ItemCandidate candidate = byId.get(itemId);
      assertEquals(item.getName().getString(), candidate.displayName());
      assertEquals("minecraft", candidate.namespace());
      assertSame(item, candidate.item());
    }
  }

  @Test
  void catalogIsCachedAndReadOnly() {
    List<ItemCandidate> first = RulesItemCatalog.all();

    assertSame(first, RulesItemCatalog.all());
    assertFalse(first.isEmpty());
    assertThrows(UnsupportedOperationException.class, () -> first.add(first.get(0)));
  }
}
