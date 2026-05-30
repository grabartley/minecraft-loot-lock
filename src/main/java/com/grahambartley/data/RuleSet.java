package com.grahambartley.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

public final class RuleSet {
  private static final RuleSet EMPTY = new RuleSet(Collections.emptySet());

  private final Set<Identifier> itemIds;

  private RuleSet(Set<Identifier> itemIds) {
    this.itemIds = Set.copyOf(itemIds);
  }

  public static RuleSet empty() {
    return EMPTY;
  }

  public static RuleSet fromRuleEntries(Collection<RuleEntry> rules) {
    if (rules == null || rules.isEmpty()) {
      return empty();
    }

    Set<Identifier> compiled = new HashSet<>();
    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        continue;
      }

      Identifier identifier = Identifier.tryParse(rule.itemId());
      if (identifier != null) {
        compiled.add(identifier);
      }
    }

    if (compiled.isEmpty()) {
      return empty();
    }

    return new RuleSet(compiled);
  }

  public boolean contains(Item item) {
    return itemIds.contains(Registries.ITEM.getId(item));
  }

  public boolean contains(Identifier itemId) {
    return itemIds.contains(itemId);
  }

  public boolean isEmpty() {
    return itemIds.isEmpty();
  }

  public Set<Identifier> itemIds() {
    return itemIds;
  }
}
