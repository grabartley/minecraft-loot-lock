package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class RulesSelectionState {
  private final Set<String> selectedItemIds = new LinkedHashSet<>();
  private int anchorIndex = -1;

  public Set<String> selectedItemIds() {
    return Set.copyOf(selectedItemIds);
  }

  public int size() {
    return selectedItemIds.size();
  }

  public boolean contains(String itemId) {
    return selectedItemIds.contains(itemId);
  }

  public void clear() {
    selectedItemIds.clear();
    anchorIndex = -1;
  }

  public void onClick(List<ItemCandidate> visible, int index, boolean shiftDown, boolean ctrlDown) {
    if (index < 0 || index >= visible.size()) {
      return;
    }
    String clickedItemId = visible.get(index).itemId();

    if (shiftDown && anchorIndex >= 0 && anchorIndex < visible.size()) {
      int start = Math.min(anchorIndex, index);
      int end = Math.max(anchorIndex, index);
      for (int i = start; i <= end; i++) {
        selectedItemIds.add(visible.get(i).itemId());
      }
      return;
    }

    if (ctrlDown) {
      if (selectedItemIds.contains(clickedItemId)) {
        selectedItemIds.remove(clickedItemId);
      } else {
        selectedItemIds.add(clickedItemId);
      }
      anchorIndex = index;
      return;
    }

    selectedItemIds.clear();
    selectedItemIds.add(clickedItemId);
    anchorIndex = index;
  }
}
