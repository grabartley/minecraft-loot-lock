package com.grahambartley.lootlock.client.keybind;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.grahambartley.lootlock.client.LootLockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LootLockKeybindsTest {
  @BeforeEach
  void resetSharedState() {
    LootLockClient.getState().clear();
  }

  @Test
  void toggleEnabledNowIsCallableAndNoOpsWhenSnapshotEmpty() {
    assertDoesNotThrow(() -> LootLockKeybinds.toggleEnabledNow(null));
  }
}
