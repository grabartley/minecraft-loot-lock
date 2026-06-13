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
  static final int VISIBLE_ROWS = 4;
  static final int BULK_BAR_HEIGHT = 12;
  static final int SEARCH_HEIGHT = 16;
  private static final long DOUBLE_CLICK_MS = 300L;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final RulesSelectionState selection = new RulesSelectionState();

  private int viewX;
  private int viewY;
  private int viewWidth;
  private int rowsTopY;
  private int rowsBottomY;
  private TextFieldWidget searchField;
  private ButtonWidget addSelectedButton;
  private ButtonWidget clearAllButton;
  private final List<RuleRowButton> rowButtons = new ArrayList<>();

  private List<ItemCandidate> visibleResults = List.of();
  private boolean showingSearch;
  private boolean visible;
  private long lastClickTime;
  private String lastClickedItemId;

  public void attach(
      int viewX, int viewY, int viewWidth, Consumer<ClickableWidget> addDrawableChild) {
    this.viewX = viewX;
    this.viewY = viewY;
    this.viewWidth = viewWidth;
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
            SEARCH_HEIGHT,
            Text.literal("Loot Lock search"));
    searchField.setMaxLength(64);
    searchField.setPlaceholder(Text.literal("Search items to add..."));
    searchField.setChangedListener(this::onSearchChanged);
    addDrawableChild.accept(searchField);
    widgets.add(searchField);

    // Reserve vertical space for the bulk bar between search and rows.
    rowsTopY = searchY + SEARCH_HEIGHT + BULK_BAR_HEIGHT + 2;
    for (int i = 0; i < VISIBLE_ROWS; i++) {
      int rowY = rowsTopY + i * (RuleRowButton.ROW_HEIGHT + 1);
      int rowIndex = i;
      RuleRowButton row =
          new RuleRowButton(
              viewX,
              rowY,
              viewWidth,
              null,
              "",
              "",
              false,
              () -> {
                int idx = rowIndex;
                return idx < visibleResults.size()
                    && selection.contains(visibleResults.get(idx).itemId());
              },
              () -> onRowPressed(rowIndex));
      addDrawableChild.accept(row);
      rowButtons.add(row);
      widgets.add(row);
    }
    rowsBottomY = rowsTopY + VISIBLE_ROWS * (RuleRowButton.ROW_HEIGHT + 1);

    int footerY = rowsBottomY + 4;
    int halfWidth = (viewWidth - 4) / 2;
    addSelectedButton =
        ButtonWidget.builder(Text.literal("Add selected"), button -> addSelected())
            .dimensions(viewX, footerY, halfWidth, 16)
            .build();
    addDrawableChild.accept(addSelectedButton);
    widgets.add(addSelectedButton);

    clearAllButton =
        ButtonWidget.builder(
                Text.literal("Clear all"),
                button -> {
                  if (showingSearch) {
                    if (searchField != null) {
                      searchField.setText("");
                    }
                  } else {
                    requestClearAll();
                  }
                })
            .dimensions(viewX + halfWidth + 4, footerY, halfWidth, 16)
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
      RuleRowButton row = rowButtons.get(i);
      if (i >= visibleResults.size()) {
        row.visible = false;
        continue;
      }
      ItemCandidate candidate = visibleResults.get(i);
      row.visible = true;
      boolean inList = showingSearch && ownedItemIds.contains(candidate.itemId());
      row.update(candidate.item(), candidate.displayName(), candidate.itemId(), inList);
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
      if (showingSearch) {
        clearAllButton.visible = true;
        clearAllButton.setMessage(Text.literal("Clear search"));
      } else {
        clearAllButton.visible = !visibleResults.isEmpty();
        clearAllButton.setMessage(Text.literal("Clear all"));
      }
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
      selection.onClick(
          visibleResults, index, Screen.hasShiftDown(), ModifierKeys.isAdditiveSelectionDown());
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
    MinecraftClient client = MinecraftClient.getInstance();

    // Bulk bar above the rows: "N results" on left, modifier hint with kbd pills on right.
    int bulkY = viewY + SEARCH_HEIGHT + 2;
    String leftText;
    if (showingSearch) {
      int n = visibleResults.size();
      leftText = n + " result" + (n == 1 ? "" : "s");
    } else {
      int n = visibleResults.size();
      leftText = n + (n == 1 ? " rule in profile" : " rules in profile");
    }
    context.drawText(
        client.textRenderer,
        Text.literal(leftText).formatted(Formatting.GRAY),
        viewX,
        bulkY,
        0xFF9A9AA4,
        false);
    if (showingSearch) {
      drawHintWithKbds(context, client, viewX + viewWidth, bulkY);
    }

    // Empty state placed centered in the rows area.
    if (visibleResults.isEmpty()) {
      String big = showingSearch ? "No items match" : "No items here yet";
      String sub =
          showingSearch
              ? "Try a different name or id."
              : "Search above or Alt+click an item in your inventory.";
      int areaHeight = rowsBottomY - rowsTopY;
      int centerY = rowsTopY + areaHeight / 2;
      int bigWidth = client.textRenderer.getWidth(big);
      int subWidth = client.textRenderer.getWidth(sub);
      context.drawText(
          client.textRenderer,
          Text.literal(big).formatted(Formatting.GRAY),
          viewX + (viewWidth - bigWidth) / 2,
          centerY - 6,
          0xFFCFCFD6,
          false);
      context.drawText(
          client.textRenderer,
          Text.literal(sub).formatted(Formatting.GRAY),
          viewX + (viewWidth - subWidth) / 2,
          centerY + 4,
          0xFF9A9AA4,
          false);
    }
  }

  private static void drawHintWithKbds(
      DrawContext context, MinecraftClient client, int rightX, int y) {
    String shift = "Shift";
    String range = " range ";
    String cmd = "Ctrl";
    String pick = " pick";
    int cmdW = client.textRenderer.getWidth(cmd) + 4;
    int shiftW = client.textRenderer.getWidth(shift) + 4;
    int rangeW = client.textRenderer.getWidth(range);
    int pickW = client.textRenderer.getWidth(pick);
    int totalW = shiftW + rangeW + cmdW + pickW;
    int cursorX = rightX - totalW;
    // Shift pill.
    paintKbd(context, client, shift, cursorX, y, shiftW);
    cursorX += shiftW;
    context.drawText(client.textRenderer, Text.literal(range), cursorX, y, 0xFF9A9AA4, false);
    cursorX += rangeW;
    paintKbd(context, client, cmd, cursorX, y, cmdW);
    cursorX += cmdW;
    context.drawText(client.textRenderer, Text.literal(pick), cursorX, y, 0xFF9A9AA4, false);
  }

  private static void paintKbd(
      DrawContext context, MinecraftClient client, String text, int x, int y, int width) {
    context.fill(x, y - 1, x + width, y + 9, 0xFF3A3A42);
    context.drawText(client.textRenderer, Text.literal(text), x + 2, y, 0xFFDCDCE2, false);
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
      net.minecraft.util.Identifier id = net.minecraft.util.Identifier.tryParse(itemId);
      net.minecraft.item.Item item =
          id == null ? null : net.minecraft.registry.Registries.ITEM.get(id);
      candidates.add(new ItemCandidate(itemId, prettyName(itemId), namespaceOf(itemId), item));
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
}
