package com.grahambartley.client.screen;

import com.grahambartley.data.LootLockProfile;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class ProfileUiController {
  private ProfileUiController() {}

  static boolean canDelete(List<LootLockProfile> profiles) {
    return profiles != null && profiles.size() > 1;
  }

  static Optional<LootLockProfile> findById(List<LootLockProfile> profiles, UUID id) {
    if (profiles == null || id == null) {
      return Optional.empty();
    }
    return profiles.stream()
        .filter(profile -> profile != null && id.equals(profile.getId()))
        .findFirst();
  }

  static String nextDuplicateName(List<LootLockProfile> profiles, String sourceName) {
    String baseName = ProfileNameValidator.sanitize(sourceName);
    if (baseName.isBlank()) {
      baseName = "Profile";
    }

    if (!containsIgnoreCase(profiles, baseName)) {
      return baseName;
    }

    int suffix = 2;
    while (suffix <= 999) {
      String candidate = baseName + " (" + suffix + ")";
      candidate = ProfileNameValidator.sanitize(candidate);
      if (!containsIgnoreCase(profiles, candidate)) {
        return candidate;
      }
      suffix++;
    }

    return ProfileNameValidator.sanitize(baseName + " copy");
  }

  private static boolean containsIgnoreCase(List<LootLockProfile> profiles, String name) {
    if (profiles == null || name == null) {
      return false;
    }
    return profiles.stream()
        .filter(profile -> profile != null)
        .anyMatch(
            profile -> name.equalsIgnoreCase(ProfileNameValidator.sanitize(profile.getName())));
  }
}
