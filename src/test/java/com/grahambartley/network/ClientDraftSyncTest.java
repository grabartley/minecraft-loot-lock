package com.grahambartley.network;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

class ClientDraftSyncTest {
  @Test
  void sendSaveRequestReturnsFalseForNullRequest() {
    assertFalse(ClientDraftSync.sendSaveRequest(null));
  }
}
