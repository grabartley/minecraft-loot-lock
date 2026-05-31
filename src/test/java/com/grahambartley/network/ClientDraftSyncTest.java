package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grahambartley.data.LootLockProfile;
import org.junit.jupiter.api.Test;

class ClientDraftSyncTest {
  @Test
  void sendSaveRequestReturnsFalseForNullRequest() {
    assertFalse(ClientDraftSync.sendSaveRequest(null));
  }

  @Test
  void sendActivateRequestReturnsFalseForNullProfileId() {
    assertFalse(ClientDraftSync.sendActivateRequest(1L, null));
  }

  @Test
  void sendCreateRequestReturnsFalseForNullName() {
    assertFalse(ClientDraftSync.sendCreateRequest(1L, null, LootLockProfile.createDefault()));
  }

  @Test
  void sendDeleteRequestReturnsFalseForNullProfileId() {
    assertFalse(ClientDraftSync.sendDeleteRequest(1L, null));
  }
}
