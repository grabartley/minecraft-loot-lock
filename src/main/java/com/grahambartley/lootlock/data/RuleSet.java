package com.grahambartley.lootlock.data;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class RuleSet {
  private static final RuleSet EMPTY = new RuleSet(Collections.emptySet());

  public static final Function<Identifier, Collection<Identifier>> DEFAULT_TAG_RESOLVER =
      RuleSet::resolveTagFromRegistry;

  private final Set<Identifier> itemIds;

  private RuleSet(Set<Identifier> itemIds) {
    this.itemIds = Set.copyOf(itemIds);
  }

  public static RuleSet empty() {
    return EMPTY;
  }

  public static RuleSet fromRuleEntries(Collection<RuleEntry> rules) {
    return fromRuleEntries(rules, DEFAULT_TAG_RESOLVER);
  }

  public static RuleSet fromRuleEntries(
      Collection<RuleEntry> rules, Function<Identifier, Collection<Identifier>> tagResolver) {
    if (rules == null || rules.isEmpty()) {
      return empty();
    }

    Set<Identifier> compiled = new HashSet<>();
    for (RuleEntry rule : rules) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        continue;
      }

      if (rule.isTag()) {
        Identifier tagId = Identifier.tryParse(rule.tagPath());
        if (tagId == null) {
          continue;
        }
        Collection<Identifier> resolved = tagResolver.apply(tagId);
        if (resolved != null) {
          compiled.addAll(resolved);
        }
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

  private static Collection<Identifier> resolveTagFromRegistry(Identifier tagId) {
    TagKey<Item> tagKey = TagKey.of(RegistryKeys.ITEM, tagId);
    Optional<RegistryEntryList.Named<Item>> entryList = Registries.ITEM.getEntryList(tagKey);
    if (entryList.isEmpty()) {
      return Set.of();
    }
    Set<Identifier> ids = new HashSet<>();
    for (RegistryEntry<Item> entry : entryList.get()) {
      Identifier id = Registries.ITEM.getId(entry.value());
      if (id != null) {
        ids.add(id);
      }
    }
    return ids;
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
