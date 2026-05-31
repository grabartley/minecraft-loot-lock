package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.data.RejectedItemAction;
import org.junit.jupiter.api.Test;

class LootLockMainScreenTest {

  @Test
  void deleteConfirmCopyWarnsAboutPermanence() {
    assertEquals("Enable delete mode?", LootLockMainScreen.deleteConfirmTitle());
    assertEquals(
        "Rejected dropped items are permanently deleted and cannot be recovered.",
        LootLockMainScreen.deleteConfirmMessage());
  }

  @Test
  void friendlyActionLabelsDeleteAsPermanent() {
    assertEquals(
        "Delete (permanent)", LootLockMainScreen.friendlyAction(RejectedItemAction.DELETE));
    assertEquals(
        "Leave on ground", LootLockMainScreen.friendlyAction(RejectedItemAction.LEAVE_ON_GROUND));
  }
}
