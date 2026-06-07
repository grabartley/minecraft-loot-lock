package com.grahambartley.client.screen;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.item.Item;

public final class ItemSearchController {
  private static final Pattern SEPARATOR_PATTERN = Pattern.compile("[_:./-]+");

  private ItemSearchController() {}

  public static List<ItemCandidate> filter(List<ItemCandidate> source, String query) {
    return filter(source, query, Set.of());
  }

  public static List<ItemCandidate> filter(
      List<ItemCandidate> source, String query, Set<String> excludedItemIds) {
    String normalized = normalize(query);
    String[] queryTokens = normalized.isBlank() ? new String[0] : normalized.split("\\s+");
    Set<String> excluded = new HashSet<>(excludedItemIds);
    List<ItemCandidate> filtered = new ArrayList<>();
    for (ItemCandidate candidate : source) {
      if (candidate == null || excluded.contains(candidate.itemId())) {
        continue;
      }
      if (normalized.isBlank()) {
        filtered.add(candidate);
        continue;
      }
      String searchable =
          normalize(candidate.itemId())
              + " "
              + normalize(candidate.displayName())
              + " "
              + normalize(candidate.namespace());
      if (matchesTokens(searchable, queryTokens)) {
        filtered.add(candidate);
      }
    }
    filtered.sort(Comparator.comparing(ItemCandidate::itemId));
    return filtered;
  }

  private static boolean matchesTokens(String searchable, String[] queryTokens) {
    for (String token : queryTokens) {
      if (!searchable.contains(token)) {
        return false;
      }
    }
    return true;
  }

  private static String normalize(String value) {
    if (value == null) {
      return "";
    }
    return SEPARATOR_PATTERN
        .matcher(value.trim().toLowerCase(Locale.ROOT))
        .replaceAll(" ")
        .replaceAll("\\s+", " ")
        .trim();
  }

  public record ItemCandidate(String itemId, String displayName, String namespace, Item item) {}
}
