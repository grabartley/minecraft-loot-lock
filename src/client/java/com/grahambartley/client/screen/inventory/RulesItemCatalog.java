package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.screen.ItemSearchController.ItemCandidate;
import com.grahambartley.client.screen.UnobtainableItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Cached catalog of pickable items used to populate the Rules tab search. Skips items flagged as
 * unobtainable so the search results stay grounded in things a player can actually encounter.
 */
public final class RulesItemCatalog {
  private static volatile List<ItemCandidate> cached;

  private RulesItemCatalog() {}

  public static List<ItemCandidate> all() {
    List<ItemCandidate> snapshot = cached;
    if (snapshot != null) {
      return snapshot;
    }

    List<ItemCandidate> built = new ArrayList<>();
    for (Item item : Registries.ITEM) {
      if (UnobtainableItems.isUnobtainable(item)) {
        continue;
      }
      Identifier id = Registries.ITEM.getId(item);
      built.add(
          new ItemCandidate(id.toString(), item.getName().getString(), id.getNamespace(), item));
    }
    snapshot = Collections.unmodifiableList(built);
    cached = snapshot;
    return snapshot;
  }
}
