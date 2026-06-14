package com.grahambartley.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Optional;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class BlockedNoticePresenterTest {

  @ParameterizedTest(name = "deleted={0}, item={1} -> \"{2}\"")
  @CsvSource({
    "false, Cobblestone, Blocked Cobblestone",
    "true,  Diamond,     Deleted Diamond",
  })
  void formatMessageRendersExpectedText(boolean deleted, String itemName, String expected) {
    assertEquals(expected, BlockedNoticePresenter.formatMessage(itemName, deleted).getString());
  }

  @Test
  void resolveItemLabelFallsBackForNullIdentifier() {
    assertEquals("unknown item", BlockedNoticePresenter.resolveItemLabel(null));
  }

  @Test
  void resolveItemLabelFallsBackToIdentifierStringWhenLookupEmpty() {
    Identifier unknown = new Identifier("lootlock", "definitely_missing_item");

    assertEquals(
        "lootlock:definitely_missing_item",
        BlockedNoticePresenter.resolveItemLabel(unknown, ignored -> Optional.empty()));
  }
}
