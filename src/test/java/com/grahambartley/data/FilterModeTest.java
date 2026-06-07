package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FilterModeTest {
  @Test
  void denylistDisplayNameIsDenylist() {
    assertEquals("Denylist", FilterMode.DENYLIST.displayName());
  }

  @Test
  void allowlistDisplayNameIsAllowlist() {
    assertEquals("Allowlist", FilterMode.ALLOWLIST.displayName());
  }
}
