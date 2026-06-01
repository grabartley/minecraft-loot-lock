package com.grahambartley.client.hud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BlockedNoticePresenterTest {
  @Test
  void formatMessageShowsBlockedWithoutCount() {
    String text = BlockedNoticePresenter.formatMessage("Cobblestone", false).getString();

    assertEquals("Blocked Cobblestone", text);
  }

  @Test
  void formatMessageShowsDeletedWithoutCount() {
    String text = BlockedNoticePresenter.formatMessage("Diamond", true).getString();

    assertEquals("Deleted Diamond", text);
  }

  @Test
  void resolveItemLabelFallsBackForNullIdentifier() {
    String label = BlockedNoticePresenter.resolveItemLabel(null);

    assertEquals("unknown item", label);
  }
}
