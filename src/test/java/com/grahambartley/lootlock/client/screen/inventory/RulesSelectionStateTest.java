package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import java.util.List;
import org.junit.jupiter.api.Test;

class RulesSelectionStateTest {
  @Test
  void plainClickCollapsesSelectionToTheClickedRowAndAnchorsIt() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c");

    state.onClick(visible, 1, false, false);

    assertEquals(1, state.size());
    assertTrue(state.contains("b"));
  }

  @Test
  void shiftClickSelectsInclusiveRangeFromAnchor() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c", "d", "e");

    state.onClick(visible, 1, false, false);
    state.onClick(visible, 3, true, false);

    assertEquals(3, state.size());
    assertTrue(state.contains("b"));
    assertTrue(state.contains("c"));
    assertTrue(state.contains("d"));
  }

  @Test
  void shiftClickWorksFromHigherIndexDownward() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c", "d", "e");

    state.onClick(visible, 4, false, false);
    state.onClick(visible, 2, true, false);

    assertEquals(3, state.size());
    assertTrue(state.contains("c"));
    assertTrue(state.contains("d"));
    assertTrue(state.contains("e"));
  }

  @Test
  void ctrlClickTogglesIndependentlyAndMovesAnchor() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c", "d");

    state.onClick(visible, 0, false, false);
    state.onClick(visible, 2, false, true);
    state.onClick(visible, 0, false, true);

    assertFalse(state.contains("a"));
    assertTrue(state.contains("c"));
    assertEquals(1, state.size());
  }

  @Test
  void ctrlClickOnAlreadySelectedRowDeselectsIt() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c");

    state.onClick(visible, 1, false, false);
    state.onClick(visible, 1, false, true);

    assertFalse(state.contains("b"));
    assertEquals(0, state.size());
  }

  @Test
  void shiftClickWithoutAnchorBehavesLikePlainClick() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c");

    state.onClick(visible, 1, true, false);

    assertEquals(1, state.size());
    assertTrue(state.contains("b"));
  }

  @Test
  void clearResetsSelectionAndAnchor() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b", "c");

    state.onClick(visible, 0, false, false);
    state.onClick(visible, 2, true, false);

    state.clear();

    assertEquals(0, state.size());
  }

  @Test
  void outOfRangeIndexIsIgnored() {
    RulesSelectionState state = new RulesSelectionState();
    List<ItemCandidate> visible = candidates("a", "b");

    state.onClick(visible, 5, false, false);
    state.onClick(visible, -1, false, false);

    assertEquals(0, state.size());
  }

  private static List<ItemCandidate> candidates(String... ids) {
    return java.util.Arrays.stream(ids)
        .map(id -> new ItemCandidate(id, id, "minecraft", null))
        .toList();
  }
}
