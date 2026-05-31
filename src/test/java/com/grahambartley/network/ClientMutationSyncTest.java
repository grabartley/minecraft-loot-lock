package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grahambartley.data.LootLockProfile;
import org.junit.jupiter.api.Test;

class ClientMutationSyncTest {
  @Test
  void sendSaveRequestReturnsFalseForNullRequest() {
    assertFalse(ClientMutationSync.sendSaveRequest(null));
  }

  @Test
  void sendActivateRequestReturnsFalseForNullProfileId() {
    assertFalse(ClientMutationSync.sendActivateRequest(1L, null));
  }

  @Test
  void sendCreateRequestReturnsFalseForNullName() {
    assertFalse(ClientMutationSync.sendCreateRequest(1L, null, LootLockProfile.createDefault()));
  }

  @Test
  void sendDeleteRequestReturnsFalseForNullProfileId() {
    assertFalse(ClientMutationSync.sendDeleteRequest(1L, null));
  }
}
