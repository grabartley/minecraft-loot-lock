package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.PacketLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

final class CommandProfileEdits {
  private CommandProfileEdits() {}

  static boolean canCreateProfile(LootLockPlayerData data) {
    return data != null && data.getProfiles().size() < PacketLimits.MAX_PROFILES;
  }

  static String normalizeProfileName(String raw) {
    if (raw == null) {
      return null;
    }

    String normalized = raw.trim();
    if (normalized.isEmpty() || normalized.length() > 32) {
      return null;
    }

    return normalized;
  }

  static String nextAvailableName(LootLockPlayerData data, String sourceName) {
    String base = normalizeProfileName(sourceName);
    if (base == null) {
      base = "Profile";
    }
    if (findProfileByName(data, base).isEmpty()) {
      return base;
    }
    for (int suffix = 2; suffix <= 999; suffix++) {
      String tail = " (" + suffix + ")";
      String prefix =
          base.length() + tail.length() <= 32 ? base : base.substring(0, 32 - tail.length());
      String candidate = prefix + tail;
      if (findProfileByName(data, candidate).isEmpty()) {
        return candidate;
      }
    }
    String tail = " copy";
    String prefix =
        base.length() + tail.length() <= 32 ? base : base.substring(0, 32 - tail.length());
    return prefix + tail;
  }

  static Optional<LootLockProfile> findProfileByName(LootLockPlayerData data, String name) {
    String normalized = normalizeProfileName(name);
    if (normalized == null) {
      return Optional.empty();
    }

    for (LootLockProfile profile : data.getProfiles()) {
      if (profile != null && normalized.equalsIgnoreCase(profile.getName())) {
        return Optional.of(profile);
      }
    }

    return Optional.empty();
  }

  static boolean containsRule(LootLockProfile profile, String itemId) {
    for (RuleEntry rule : profile.getRules()) {
      if (rule != null && itemId.equals(rule.itemId())) {
        return true;
      }
    }
    return false;
  }

  static RejectedItemAction normalizeRejectedItemAction(
      RejectedItemAction action, boolean allowDeleteRejectedItems) {
    if (!allowDeleteRejectedItems && action == RejectedItemAction.DELETE) {
      return RejectedItemAction.LEAVE_ON_GROUND;
    }
    return action == null ? RejectedItemAction.LEAVE_ON_GROUND : action;
  }

  static void applyGlobalEnable(LootLockPlayerData data, boolean enabled) {
    data.setEnabledForAll(enabled);
  }

  static LootLockProfile createProfileWithDefaults(String profileName) {
    return new LootLockProfile(
        UUID.randomUUID(),
        profileName,
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        List.of());
  }

  static void appendProfile(LootLockPlayerData data, LootLockProfile profile) {
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.add(profile);
    data.setProfiles(profiles);
  }

  static void removeProfileById(LootLockPlayerData data, UUID profileId) {
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.removeIf(profile -> profile != null && profile.getId().equals(profileId));
    data.setProfiles(profiles);
    if (profileId.equals(data.getActiveProfileId()) && !profiles.isEmpty()) {
      data.setActiveProfileId(profiles.get(0).getId());
    }
  }

  static boolean addRuleToProfile(LootLockProfile profile, String itemId) {
    if (containsRule(profile, itemId)) {
      return false;
    }
    List<RuleEntry> rules = new ArrayList<>(profile.getRules());
    rules.add(new RuleEntry(itemId));
    profile.setRules(rules);
    return true;
  }

  static boolean removeRuleFromProfile(LootLockProfile profile, String itemId) {
    List<RuleEntry> rules = new ArrayList<>(profile.getRules());
    boolean removed = rules.removeIf(rule -> rule != null && itemId.equals(rule.itemId()));
    if (removed) {
      profile.setRules(rules);
    }
    return removed;
  }

  static void clearRulesOnProfile(LootLockProfile profile) {
    profile.setRules(List.of());
  }
}
