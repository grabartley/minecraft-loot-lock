package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.screen.ItemSearchController;
import com.grahambartley.client.screen.ItemSearchController.ItemCandidate;
import com.grahambartley.client.screen.RuleListController;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Rules tab content: inline search field, multi-select results list with Shift / Ctrl modifiers and
 * double-click to add, current rules list shown when the search field is empty, and footer actions
 * for Add selected and Clear all.
 *
 * <p>Holds onto its widgets across show/hide via the {@code visible} flag so input routing keeps
 * flowing through the host screen's vanilla widget dispatch.
 */
public final class RulesTabView {
  static final int VISIBLE_ROWS = 6;
  static final int ROW_HEIGHT = 18;
  static final int ROW_GAP = 1;
  private static final long DOUBLE_CLICK_MS = 300L;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final RulesSelectionState selection = new RulesSelectionState();

  private TextFieldWidget searchField;
  private ButtonWidget addSelectedButton;
  private ButtonWidget clearAllButton;
  private final List<ButtonWidget> rowButtons = new ArrayList<>();

  private List<ItemCandidate> visibleResults = List.of();
  private boolean showingSearch;
  private boolean visible;
  private long lastClickTime;
  private String lastClickedItemId;

  public void attach(
      int viewX, int viewY, int viewWidth, Consumer<ClickableWidget> addDrawableChild) {
    widgets.clear();
    rowButtons.clear();
    selection.clear();

    int searchY = viewY;
    searchField =
        new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            viewX,
            searchY,
            viewWidth,
            16,
            Text.literal("Search items to add..."));
    searchField.setMaxLength(64);
    searchField.setChangedListener(this::onSearchChanged);
    addDrawableChild.accept(searchField);
    widgets.add(searchField);

    int rowsTop = searchY + 18 + ROW_GAP;
    for (int i = 0; i < VISIBLE_ROWS; i++) {
      int rowY = rowsTop + i * (ROW_HEIGHT + ROW_GAP);
      int rowIndex = i;
      ButtonWidget row =
          ButtonWidget.builder(Text.empty(), button -> onRowPressed(rowIndex))
              .dimensions(viewX, rowY, viewWidth, ROW_HEIGHT)
              .build();
      addDrawableChild.accept(row);
      rowButtons.add(row);
      widgets.add(row);
    }

    int footerY = rowsTop + VISIBLE_ROWS * (ROW_HEIGHT + ROW_GAP) + 3;
    int halfWidth = (viewWidth - 4) / 2;
    addSelectedButton =
        ButtonWidget.builder(Text.literal("Add selected"), button -> addSelected())
            .dimensions(viewX, footerY, halfWidth, ROW_HEIGHT)
            .build();
    addDrawableChild.accept(addSelectedButton);
    widgets.add(addSelectedButton);

    clearAllButton =
        ButtonWidget.builder(Text.literal("Clear all"), button -> requestClearAll())
            .dimensions(viewX + halfWidth + 4, footerY, halfWidth, ROW_HEIGHT)
            .build();
    addDrawableChild.accept(clearAllButton);
    widgets.add(clearAllButton);

    setVisible(false);
    refresh();
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
    for (ClickableWidget widget : widgets) {
      widget.visible = visible;
    }
    if (!visible) {
      selection.clear();
    }
  }

  public void refresh() {
    if (!visible) {
      return;
    }
    String query = searchField == null ? "" : searchField.getText().trim();
    Set<String> ownedItemIds = ownedItemIds();
    if (query.isBlank()) {
      showingSearch = false;
      visibleResults = currentRulesAsCandidates();
    } else {
      showingSearch = true;
      visibleResults = ItemSearchController.filter(RulesItemCatalog.all(), query);
    }

    for (int i = 0; i < rowButtons.size(); i++) {
      ButtonWidget row = rowButtons.get(i);
      if (i >= visibleResults.size()) {
        row.visible = false;
        continue;
      }
      ItemCandidate candidate = visibleResults.get(i);
      row.visible = true;
      Text label = renderRowLabel(candidate, ownedItemIds);
      row.setMessage(label);
      row.setTooltip(Tooltip.of(Text.literal(candidate.itemId())));
    }

    if (addSelectedButton != null) {
      addSelectedButton.visible = showingSearch;
      addSelectedButton.active = showingSearch && selection.size() > 0;
      int n = selection.size();
      addSelectedButton.setMessage(
          n == 0 ? Text.literal("Add selected") : Text.literal("Add selected (" + n + ")"));
    }
    if (clearAllButton != null) {
      clearAllButton.visible = !showingSearch && !visibleResults.isEmpty();
    }
  }

  void onSearchChanged(String value) {
    selection.clear();
    refresh();
  }

  void onRowPressed(int index) {
    if (index < 0 || index >= visibleResults.size()) {
      return;
    }
    ItemCandidate clickedCandidate = visibleResults.get(index);
    if (showingSearch) {
      long now = System.currentTimeMillis();
      boolean doubleClick =
          now - lastClickTime < DOUBLE_CLICK_MS
              && clickedCandidate.itemId().equals(lastClickedItemId);
      lastClickTime = now;
      lastClickedItemId = clickedCandidate.itemId();
      if (doubleClick) {
        RuleMutations.addToActiveProfile(List.of(clickedCandidate.itemId()));
        selection.clear();
        refresh();
        return;
      }
      selection.onClick(visibleResults, index, Screen.hasShiftDown(), Screen.hasControlDown());
      refresh();
      return;
    }

    // When showing current rules, clicking a row removes it.
    RuleMutations.removeFromActiveProfile(clickedCandidate.itemId());
    refresh();
  }

  void addSelected() {
    if (selection.size() == 0) {
      return;
    }
    if (RuleMutations.addToActiveProfile(selection.selectedItemIds())) {
      selection.clear();
      refresh();
    }
  }

  void requestClearAll() {
    MinecraftClient client = MinecraftClient.getInstance();
    Screen current = client == null ? null : client.currentScreen;
    if (client == null || current == null) {
      return;
    }
    client.setScreen(
        new net.minecraft.client.gui.screen.ConfirmScreen(
            confirmed -> {
              if (confirmed) {
                RuleMutations.clearActiveProfile();
              }
              client.setScreen(current);
            },
            Text.literal("Clear every rule from this profile?"),
            Text.literal("This removes all of the active profile's rules. Cannot be undone.")));
  }

  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!visible) {
      return;
    }
    if (visibleResults.isEmpty()) {
      String message = showingSearch ? "No items match" : "No rules yet, search above to add some";
      context.drawText(
          MinecraftClient.getInstance().textRenderer,
          Text.literal(message).formatted(Formatting.GRAY),
          rowButtons.get(0).getX(),
          rowButtons.get(0).getY() + 4,
          0x6E6E6E,
          false);
    }
  }

  Text renderRowLabel(ItemCandidate candidate, Set<String> ownedItemIds) {
    if (!showingSearch) {
      return Text.literal(candidate.displayName()).formatted(Formatting.WHITE);
    }
    boolean inList = ownedItemIds.contains(candidate.itemId());
    boolean selected = selection.contains(candidate.itemId());
    String suffix = inList ? "  (in list)" : "";
    Formatting color =
        selected ? Formatting.YELLOW : (inList ? Formatting.DARK_GRAY : Formatting.WHITE);
    return Text.literal(candidate.displayName() + suffix).formatted(color);
  }

  Set<String> ownedItemIds() {
    LootLockProfile profile = activeProfile();
    if (profile == null || profile.getRules() == null) {
      return Collections.emptySet();
    }
    Set<String> owned = new HashSet<>();
    for (RuleEntry rule : profile.getRules()) {
      if (rule != null && rule.itemId() != null) {
        owned.add(rule.itemId());
      }
    }
    return owned;
  }

  List<ItemCandidate> currentRulesAsCandidates() {
    LootLockProfile profile = activeProfile();
    if (profile == null) {
      return List.of();
    }
    List<ItemCandidate> candidates = new ArrayList<>();
    for (RuleEntry rule : RuleListController.dedupeRules(profile.getRules())) {
      String itemId = rule.itemId();
      candidates.add(new ItemCandidate(itemId, prettyName(itemId), namespaceOf(itemId), null));
    }
    return candidates;
  }

  static String prettyName(String itemId) {
    if (itemId == null) {
      return "?";
    }
    int colon = itemId.indexOf(':');
    String path = colon < 0 ? itemId : itemId.substring(colon + 1);
    return path.replace('_', ' ');
  }

  static String namespaceOf(String itemId) {
    if (itemId == null) {
      return "";
    }
    int colon = itemId.indexOf(':');
    return colon < 0 ? "" : itemId.substring(0, colon);
  }

  private static LootLockProfile activeProfile() {
    return LootLockClient.getState()
        .getSnapshot()
        .flatMap(LootLockPlayerData::getActiveProfile)
        .orElse(null);
  }

  // Test-only accessors -----------------------------------------------------
  RulesSelectionState selectionForTest() {
    return selection;
  }

  void setVisibleResultsForTest(List<ItemCandidate> results) {
    this.visibleResults = results;
    this.showingSearch = true;
  }

  List<ItemCandidate> getVisibleResultsForTest() {
    return visibleResults;
  }

  boolean isShowingSearchForTest() {
    return showingSearch;
  }

  static UUID stableTestId() {
    return UUID.nameUUIDFromBytes("test".getBytes());
  }
}
