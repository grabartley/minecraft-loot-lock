package com.grahambartley.client.screen;

import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Predicate;

public final class RuleListController {
  private RuleListController() {}

  public static List<RuleEntry> dedupeRules(List<RuleEntry> rules) {
    Set<String> seen = new LinkedHashSet<>();
    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        continue;
      }
      seen.add(rule.itemId());
    }
    List<RuleEntry> deduped = new ArrayList<>();
    for (String itemId : seen) {
      deduped.add(new RuleEntry(itemId));
    }
    return deduped;
  }

  public static List<RuleEntry> filterRules(List<RuleEntry> rules, String query) {
    String normalized = normalize(query);
    if (normalized.isBlank()) {
      return new ArrayList<>(rules);
    }
    List<RuleEntry> filtered = new ArrayList<>();
    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null) {
        continue;
      }
      if (normalize(rule.itemId()).contains(normalized)) {
        filtered.add(rule);
      }
    }
    return filtered;
  }

  public static List<RuleEntry> unresolvedRules(
      List<RuleEntry> rules, Predicate<String> isResolvable) {
    List<RuleEntry> unresolved = new ArrayList<>();
    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        continue;
      }
      if (!isResolvable.test(rule.itemId())) {
        unresolved.add(rule);
      }
    }
    return unresolved;
  }

  public static List<RuleEntry> withRuleAdded(List<RuleEntry> rules, String itemId) {
    List<RuleEntry> deduped = dedupeRules(rules);
    for (RuleEntry rule : deduped) {
      if (rule.itemId().equals(itemId)) {
        return deduped;
      }
    }
    deduped.add(new RuleEntry(itemId));
    return deduped;
  }

  public static List<RuleEntry> withRuleRemoved(List<RuleEntry> rules, String itemId) {
    List<RuleEntry> updated = new ArrayList<>();
    for (RuleEntry rule : dedupeRules(rules)) {
      if (!rule.itemId().equals(itemId)) {
        updated.add(rule);
      }
    }
    return updated;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
