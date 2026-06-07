package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.grahambartley.data.RejectedItemAction;
import net.minecraft.text.Text;
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
  void friendlyActionLabelsAreCompact() {
    assertEquals("Delete", LootLockMainScreen.friendlyAction(RejectedItemAction.DELETE));
    assertEquals("Leave", LootLockMainScreen.friendlyAction(RejectedItemAction.LEAVE_ON_GROUND));
  }

  @Test
  void serverStateCopyIsCollapsedToSingleLine() {
    Text supported = LootLockMainScreen.serverStateText(true);
    Text unsupported = LootLockMainScreen.serverStateText(false);

    assertEquals("Server: Supported", supported.getString());
    assertEquals("Server: Unsupported", unsupported.getString());
  }

  @Test
  void deletePolicyCopyUsesPassiveFooterText() {
    assertEquals("Delete policy: Allowed", LootLockMainScreen.deletePolicyText(true).getString());
    assertEquals("Delete policy: Blocked", LootLockMainScreen.deletePolicyText(false).getString());
  }
}
