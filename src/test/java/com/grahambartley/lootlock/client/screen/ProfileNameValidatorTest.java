package com.grahambartley.lootlock.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ProfileNameValidatorTest {
  @Test
  void sanitizeTrimsAndTruncatesToUiLimit() {
    String sanitized = ProfileNameValidator.sanitize("  abcdefghijklmnopqrstuvwxyz1234567890  ");
    assertEquals("abcdefghijklmnopqrstuvwxyz123456", sanitized);
  }
}
