package com.grahambartley.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class FilterModeTest {

  @ParameterizedTest(name = "{0} -> {1}")
  @CsvSource({
    "DENYLIST,  Denylist",
    "ALLOWLIST, Allowlist",
  })
  void displayNameMatchesEnumLabel(FilterMode mode, String expected) {
    assertEquals(expected, mode.displayName());
  }
}
