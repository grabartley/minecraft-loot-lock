package com.grahambartley.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

class BlockedNoticePresenterTest {
  @Test
  void formatMessageShowsBlockedWithoutCount() {
    Identifier itemId = Identifier.of("minecraft", "cobblestone");

    String text = BlockedNoticePresenter.formatMessage(itemId, false).getString();

    assertEquals("Blocked minecraft:cobblestone", text);
  }

  @Test
  void formatMessageShowsDeletedWithoutCount() {
    Identifier itemId = Identifier.of("minecraft", "diamond");

    String text = BlockedNoticePresenter.formatMessage(itemId, true).getString();

    assertEquals("Deleted minecraft:diamond", text);
  }
}
