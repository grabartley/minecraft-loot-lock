package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RulesTagCatalogTest {
  private static final Identifier SHINY_TAG = Identifier.of("lootlock", "test/shiny_things");
  private static final Identifier EMPTY_TAG = Identifier.of("lootlock", "empty_tag");

  @BeforeAll
  static void bootstrapWithTags() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
    Registries.ITEM.populateTags(
        Map.of(
            TagKey.of(RegistryKeys.ITEM, SHINY_TAG),
            List.of(
                Registries.ITEM.getEntry(Items.DIAMOND), Registries.ITEM.getEntry(Items.EMERALD)),
            TagKey.of(RegistryKeys.ITEM, EMPTY_TAG),
            List.of()));
  }

  @AfterAll
  static void clearTags() {
    Registries.ITEM.populateTags(Map.of());
    RulesTagCatalog.invalidate();
  }

  @BeforeEach
  void resetCache() {
    RulesTagCatalog.invalidate();
  }

  @ParameterizedTest(name = "{0} -> \"{1}\" in {2}")
  @CsvSource({
    "#lootlock:test/shiny_things, test shiny things, lootlock",
    "#lootlock:empty_tag,         empty tag,         lootlock",
  })
  void tagsBecomePrefixedCandidatesWithReadableNames(
      String expectedId, String expectedName, String expectedNamespace) {
    Map<String, ItemCandidate> byId =
        RulesTagCatalog.all().stream()
            .collect(Collectors.toMap(ItemCandidate::itemId, Function.identity()));

    ItemCandidate candidate = byId.get(expectedId);
    assertEquals(expectedName, candidate.displayName());
    assertEquals(expectedNamespace, candidate.namespace());
    assertNull(candidate.item());
  }

  @Test
  void catalogIsCachedUntilInvalidated() {
    List<ItemCandidate> first = RulesTagCatalog.all();

    assertSame(first, RulesTagCatalog.all());
    RulesTagCatalog.invalidate();
    List<ItemCandidate> rebuilt = RulesTagCatalog.all();
    assertNotSame(first, rebuilt);
    assertEquals(first, rebuilt);
  }

  @ParameterizedTest(name = "resolvedCount({0}) -> {1}")
  @CsvSource({
    "lootlock:test/shiny_things, 2",
    "lootlock:empty_tag,         0",
    "lootlock:missing_tag,       -1",
    ",                           -1",
  })
  void resolvedCountReportsTagSizeOrMissing(String tagId, int expected) {
    assertEquals(
        expected, RulesTagCatalog.resolvedCount(tagId == null ? null : Identifier.of(tagId)));
  }
}
