package com.grahambartley.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class ItemSearchController {
  private ItemSearchController() {}

  public static List<ItemCandidate> filter(List<ItemCandidate> source, String query) {
    String normalized = normalize(query);
    List<ItemCandidate> filtered = new ArrayList<>();
    for (ItemCandidate candidate : source) {
      if (normalized.isBlank()) {
        filtered.add(candidate);
        continue;
      }
      if (normalize(candidate.itemId()).contains(normalized)
          || normalize(candidate.displayName()).contains(normalized)
          || normalize(candidate.namespace()).contains(normalized)) {
        filtered.add(candidate);
      }
    }
    filtered.sort(Comparator.comparing(ItemCandidate::itemId));
    return filtered;
  }

  private static String normalize(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }

  public record ItemCandidate(String itemId, String displayName, String namespace) {}
}
