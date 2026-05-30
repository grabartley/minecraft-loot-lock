package com.grahambartley.config;

import java.util.Collections;
import java.util.List;

public record ConfigValidationResult(boolean valid, List<String> errors) {
  private static final ConfigValidationResult VALID =
      new ConfigValidationResult(true, Collections.emptyList());

  public static ConfigValidationResult success() {
    return VALID;
  }

  public static ConfigValidationResult failure(String error) {
    return new ConfigValidationResult(false, Collections.singletonList(error));
  }

  public static ConfigValidationResult failure(List<String> errors) {
    return new ConfigValidationResult(false, errors);
  }

  public ConfigValidationResult {
    errors = errors == null ? Collections.emptyList() : Collections.unmodifiableList(errors);
  }
}
