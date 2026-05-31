package com.grahambartley.client.screen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProfileNameValidatorTest {
  @Test
  void sanitizeTrimsAndTruncatesToUiLimit() {
    String sanitized = ProfileNameValidator.sanitize("  abcdefghijklmnopqrstuvwxyz1234567890  ");
    assertEquals("abcdefghijklmnopqrstuvwxyz123456", sanitized);
  }

  @Test
  void validNameRequiresNonBlankValue() {
    assertTrue(ProfileNameValidator.isValid("Builder"));
    assertFalse(ProfileNameValidator.isValid("   "));
  }
}
