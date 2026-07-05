package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import com.grahambartley.lootlock.data.RuleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public final class RulesTagCatalog {
  private static volatile List<ItemCandidate> cached;

  private RulesTagCatalog() {}

  public static List<ItemCandidate> all() {
    List<ItemCandidate> snapshot = cached;
    if (snapshot != null) {
      return snapshot;
    }

    List<ItemCandidate> built = new ArrayList<>();
    Registries.ITEM
        .streamTags()
        .forEach(
            tagKey -> {
              Identifier tagId = tagKey.id();
              String entryId = RuleEntry.TAG_PREFIX + tagId;
              String displayName = tagId.getPath().replace('/', ' ').replace('_', ' ');
              built.add(new ItemCandidate(entryId, displayName, tagId.getNamespace(), null));
            });
    snapshot = Collections.unmodifiableList(built);
    cached = snapshot;
    return snapshot;
  }

  public static void invalidate() {
    cached = null;
  }

  public static int resolvedCount(Identifier tagId) {
    if (tagId == null) {
      return -1;
    }
    java.util.Optional<RegistryEntryList.Named<Item>> list =
        Registries.ITEM.getEntryList(TagKey.of(net.minecraft.registry.RegistryKeys.ITEM, tagId));
    return list.map(RegistryEntryList.Named::size).orElse(-1);
  }
}
