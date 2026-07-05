package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Mutable multi-select model for the rules-tab search results. Behaviour:
 *
 * <ul>
 *   <li>Plain click on a row collapses selection to that single row and records it as the anchor.
 *   <li>Ctrl / Cmd click toggles the row's selection independently and moves the anchor.
 *   <li>Shift click selects the inclusive range from the anchor to the clicked row.
 * </ul>
 *
 * Anchors persist across plain selects and toggles so subsequent Shift clicks behave like a file
 * manager. Searches and other resets clear both the selection and the anchor.
 */
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

  /** Handle a click on a row at {@code index} in the current visible result list. */
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
