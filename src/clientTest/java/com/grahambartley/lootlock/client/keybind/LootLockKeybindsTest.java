package com.grahambartley.lootlock.client.keybind;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.grahambartley.lootlock.client.LootLockClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.lwjgl.glfw.GLFW;

class LootLockKeybindsTest {
  @BeforeEach
  void resetSharedState() {
    LootLockClient.getState().clear();
  }

  @Test
  void toggleEnabledNowIsCallableAndNoOpsWhenSnapshotEmpty() {
    assertDoesNotThrow(() -> LootLockKeybinds.toggleEnabledNow(null));
  }

  @ParameterizedTest(name = "key {0} does not match before registration")
  @ValueSource(ints = {GLFW.GLFW_KEY_UNKNOWN, GLFW.GLFW_KEY_A, GLFW.GLFW_KEY_P})
  void matchersRejectKeysBeforeRegistration(int keyCode) {
    assertFalse(LootLockKeybinds.matchesAddHovered(keyCode, 0));
    assertFalse(LootLockKeybinds.matchesCycleProfile(keyCode, 0));
    assertFalse(LootLockKeybinds.matchesToggleEnabled(keyCode, 0));
  }
}
