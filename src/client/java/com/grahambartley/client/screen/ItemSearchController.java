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

  static SelectionState select(
      List<ItemCandidate> visible,
      List<String> selectedItemIds,
      int lastClickedIndex,
      int clickedIndex,
      boolean controlDown,
      boolean shiftDown) {
    if (clickedIndex < 0 || clickedIndex >= visible.size()) {
      return new SelectionState(List.copyOf(selectedItemIds), lastClickedIndex);
    }

    String clickedItemId = visible.get(clickedIndex).itemId();
    List<String> updatedSelection = new ArrayList<>();
    if (controlDown) {
      updatedSelection.addAll(selectedItemIds);
      if (updatedSelection.contains(clickedItemId)) {
        updatedSelection.remove(clickedItemId);
      } else {
        updatedSelection.add(clickedItemId);
      }
    } else if (shiftDown && lastClickedIndex >= 0 && lastClickedIndex < visible.size()) {
      int start = Math.min(lastClickedIndex, clickedIndex);
      int end = Math.max(lastClickedIndex, clickedIndex);
      for (int i = start; i <= end; i++) {
        updatedSelection.add(visible.get(i).itemId());
      }
    } else {
      updatedSelection.add(clickedItemId);
    }

    return new SelectionState(List.copyOf(updatedSelection), clickedIndex);
  }

  static List<String> retainVisibleSelection(
      List<ItemCandidate> visible, List<String> selectedItemIds) {
    Set<String> visibleIds = new HashSet<>();
    for (ItemCandidate candidate : visible) {
      visibleIds.add(candidate.itemId());
    }

    List<String> retained = new ArrayList<>();
    for (String selectedItemId : selectedItemIds) {
      if (visibleIds.contains(selectedItemId)) {
        retained.add(selectedItemId);
      }
    }
    return List.copyOf(retained);
  }

  static List<String> selectedItemIdsInVisibleOrder(
      List<ItemCandidate> visible, List<String> selectedItemIds) {
    Set<String> selectedIds = new HashSet<>(selectedItemIds);
    List<String> ordered = new ArrayList<>();
    for (ItemCandidate candidate : visible) {
      if (selectedIds.contains(candidate.itemId())) {
        ordered.add(candidate.itemId());
      }
    }
    return ordered;
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

  record SelectionState(List<String> selectedItemIds, int lastClickedIndex) {}

  public record ItemCandidate(String itemId, String displayName, String namespace, Item item) {}
}
