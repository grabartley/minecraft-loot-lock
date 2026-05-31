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

  static Optional<UUID> nextProfileId(List<LootLockProfile> profiles, UUID activeProfileId) {
    if (profiles == null || profiles.isEmpty()) {
      return Optional.empty();
    }

    int currentIndex = -1;
    for (int i = 0; i < profiles.size(); i++) {
      LootLockProfile profile = profiles.get(i);
      if (profile != null && profile.getId().equals(activeProfileId)) {
        currentIndex = i;
        break;
      }
    }

    if (currentIndex == -1) {
      LootLockProfile first = profiles.get(0);
      return first == null ? Optional.empty() : Optional.of(first.getId());
    }

    LootLockProfile next = profiles.get((currentIndex + 1) % profiles.size());
    return next == null ? Optional.empty() : Optional.of(next.getId());
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
