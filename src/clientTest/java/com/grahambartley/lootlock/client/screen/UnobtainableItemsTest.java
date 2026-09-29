package com.grahambartley.lootlock.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class UnobtainableItemsTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<Arguments> items() {
    return Stream.of(
        Arguments.of("air", Items.AIR, true),
        Arguments.of("bedrock", Items.BEDROCK, true),
        Arguments.of("end portal frame", Items.END_PORTAL_FRAME, true),
        Arguments.of("knowledge book", Items.KNOWLEDGE_BOOK, true),
        Arguments.of("dragon egg", Items.DRAGON_EGG, true),
        Arguments.of("spawner", Items.SPAWNER, true),
        Arguments.of("stone", Items.STONE, false),
        Arguments.of("diamond", Items.DIAMOND, false),
        Arguments.of("wheat seeds", Items.WHEAT_SEEDS, false));
  }

  @ParameterizedTest(name = "{0} -> unobtainable={2}")
  @MethodSource("items")
  void classifiesItems(String label, Item item, boolean expected) {
    assertEquals(expected, UnobtainableItems.isUnobtainable(item));
  }
}
