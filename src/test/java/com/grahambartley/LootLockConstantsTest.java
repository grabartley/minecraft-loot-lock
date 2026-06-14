package com.grahambartley;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class LootLockConstantsTest {

  @Test
  void modIdRemainsLootLockKebabId() {
    assertEquals("loot-lock", LootLockConstants.MOD_ID);
  }

  @Test
  void modNameUsesSpacedDisplayForm() {
    assertEquals("Loot Lock", LootLockConstants.MOD_NAME);
  }
}
