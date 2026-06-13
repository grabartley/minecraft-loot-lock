package com.grahambartley.client.screen;

public final class ProfileNameValidator {
  public static final int MAX_UI_PROFILE_NAME_LENGTH = 32;

  private ProfileNameValidator() {}

  public static String sanitize(String name) {
    if (name == null) {
      return "";
    }
    String trimmed = name.trim();
    if (trimmed.length() <= MAX_UI_PROFILE_NAME_LENGTH) {
      return trimmed;
    }
    return trimmed.substring(0, MAX_UI_PROFILE_NAME_LENGTH);
  }

  public static boolean isValid(String name) {
    String sanitized = sanitize(name);
    return !sanitized.isBlank() && sanitized.length() <= MAX_UI_PROFILE_NAME_LENGTH;
  }
}
