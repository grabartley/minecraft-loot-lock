package com.grahambartley.lootlock.client.screen;

import com.grahambartley.lootlock.data.RuleEntry;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RuleListController {
  private RuleListController() {}

  public static List<RuleEntry> dedupeRules(List<RuleEntry> rules) {
    Set<String> seen = itemIdSet(rules);
    List<RuleEntry> deduped = new ArrayList<>();
    for (String itemId : seen) {
      deduped.add(new RuleEntry(itemId));
    }
    return deduped;
  }

  public static List<RuleEntry> withRulesAdded(List<RuleEntry> rules, List<String> itemIds) {
    List<RuleEntry> deduped = dedupeRules(rules);
    Set<String> seen = itemIdSet(deduped);
    for (String itemId : itemIds) {
      if (itemId == null || itemId.isBlank() || !seen.add(itemId)) {
        continue;
      }
      deduped.add(new RuleEntry(itemId));
    }
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

  private static Set<String> itemIdSet(List<RuleEntry> rules) {
    Set<String> seen = new LinkedHashSet<>();
    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        continue;
      }
      seen.add(rule.itemId());
    }
    return seen;
  }
}
