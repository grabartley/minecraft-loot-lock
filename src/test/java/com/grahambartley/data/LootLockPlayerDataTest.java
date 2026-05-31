package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LootLockPlayerDataTest {
  @Test
  void setRevisionRejectsNegativeValues() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());

    assertThrows(IllegalArgumentException.class, () -> data.setRevision(-1));
  }

  @Test
  void setRevisionRejectsDecreasingValues() {
    LootLockPlayerData data = LootLockPlayerData.createDefault(UUID.randomUUID());
    data.setRevision(5);

    assertThrows(IllegalArgumentException.class, () -> data.setRevision(4));
    assertEquals(5, data.getRevision());
  }
}
