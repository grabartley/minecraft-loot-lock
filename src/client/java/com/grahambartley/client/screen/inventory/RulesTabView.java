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
  /** Exposed so the inventory mixin can swallow the inventory keybind while the user is typing. */
  public boolean isSearchFieldFocused() {
    return searchField != null && searchField.isFocused();
  }

  static final int BULK_BAR_HEIGHT = 12;
  static final int SEARCH_HEIGHT = 16;
  static final int FOOTER_HEIGHT = 16;
  static final int FOOTER_GAP = 4;

  /** Upper bound on simultaneously-visible result rows. Extras are pre-created and hidden. */
  static final int MAX_ROWS = 8;

  private static final long DOUBLE_CLICK_MS = 300L;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final RulesSelectionState selection = new RulesSelectionState();

  private LootLockInventoryPanel panel;
  private int visibleRows = 4;
  // Per-view offsets relative to the panel content inset origin. These never change after attach;
  // all live positions are derived as panel.getContentInsetX/Y + offset to keep the view glued to
  // the container as it moves (e.g. when the recipe book shifts the inventory).
  private int searchOffsetY;
  private int bulkOffsetY;
  private int rowsTopOffsetY;
  private int rowsBottomOffsetY;
  private int footerOffsetY;
  private TextFieldWidget searchField;
  private ButtonWidget addSelectedButton;
  private ButtonWidget clearAllButton;
  private final List<RuleRowButton> rowButtons = new ArrayList<>();

  private List<ItemCandidate> visibleResults = List.of();
  private boolean showingSearch;
  private boolean visible;
  private long lastClickTime;
  private String lastClickedItemId;
  private int scrollOffset;

  public void attach(LootLockInventoryPanel panel, Consumer<ClickableWidget> addDrawableChild) {
    this.panel = panel;
    widgets.clear();
    rowButtons.clear();
    selection.clear();

    searchField =
        new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0,
            0,
            10,
            SEARCH_HEIGHT,
            Text.literal("Loot Lock search"));
    searchField.setMaxLength(64);
    searchField.setPlaceholder(Text.literal("Search items to add..."));
    searchField.setChangedListener(this::onSearchChanged);
    addDrawableChild.accept(searchField);
    widgets.add(searchField);

    // Pre-create the upper bound of row widgets so screen / GUI-scale changes can grow the
    // visible-row count without re-mounting widgets through the host's children list.
    for (int i = 0; i < MAX_ROWS; i++) {
      int rowIndex = i;
      RuleRowButton row =
          new RuleRowButton(
              0,
              0,
              10,
              null,
              "",
              "",
              false,
              () -> {
                int idx = rowIndex + scrollOffset;
                return idx < visibleResults.size()
                    && selection.contains(visibleResults.get(idx).itemId());
              },
              () -> onRowPressed(rowIndex + scrollOffset));
      addDrawableChild.accept(row);
      rowButtons.add(row);
      widgets.add(row);
    }

    addSelectedButton =
        ButtonWidget.builder(Text.literal("Add selected"), button -> addSelected())
            .dimensions(0, 0, 10, FOOTER_HEIGHT)
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
            .dimensions(0, 0, 10, FOOTER_HEIGHT)
            .build();
    addDrawableChild.accept(clearAllButton);
    widgets.add(clearAllButton);

    setVisible(false);
    relayout();
    refresh();
  }

  /**
   * Recomputes widget positions + dimensions from the panel's live content-inset, including the
   * visibleRows count derived from current panel height. Called from the panel whenever its anchor
   * or height changes (window resize, GUI scale change, recipe-book open, etc.).
   */
  public void relayout() {
    if (panel == null || searchField == null) {
      return;
    }
    int viewX = panel.getContentInsetX();
    int viewY = panel.getContentInsetY();
    int viewWidth = panel.getContentInsetWidth();
    int viewHeight = panel.getContentInsetHeight();

    searchOffsetY = 0;
    bulkOffsetY = SEARCH_HEIGHT + 2;
    rowsTopOffsetY = SEARCH_HEIGHT + BULK_BAR_HEIGHT + 2;
    int footerReserved = FOOTER_GAP + FOOTER_HEIGHT;
    int rowsAvailable = viewHeight - rowsTopOffsetY - footerReserved;
    int rowStride = RuleRowButton.ROW_HEIGHT + 1;
    visibleRows = Math.max(1, Math.min(MAX_ROWS, rowsAvailable / rowStride));
    rowsBottomOffsetY = rowsTopOffsetY + visibleRows * rowStride;
    footerOffsetY = rowsBottomOffsetY + FOOTER_GAP;

    searchField.setPosition(viewX, viewY + searchOffsetY);
    searchField.setWidth(viewWidth);

    for (int i = 0; i < rowButtons.size(); i++) {
      RuleRowButton row = rowButtons.get(i);
      if (i < visibleRows) {
        row.setPosition(viewX, viewY + rowsTopOffsetY + i * rowStride);
        row.setWidth(viewWidth);
      } else {
        // Park hidden rows off-screen so a stale hover from a previous layout cannot reach them.
        row.setPosition(-9999, -9999);
      }
    }

    int footerY = viewY + footerOffsetY;
    int halfWidth = (viewWidth - 4) / 2;
    if (addSelectedButton != null) {
      addSelectedButton.setPosition(viewX, footerY);
      addSelectedButton.setWidth(halfWidth);
    }
    if (clearAllButton != null) {
      clearAllButton.setPosition(viewX + halfWidth + 4, footerY);
      clearAllButton.setWidth(halfWidth);
    }
  }

  private int viewX() {
    return panel == null ? 0 : panel.getContentInsetX();
  }

  private int viewY() {
    return panel == null ? 0 : panel.getContentInsetY();
  }

  private int viewWidth() {
    return panel == null ? 0 : panel.getContentInsetWidth();
  }

  private int rowsTopY() {
    return viewY() + rowsTopOffsetY;
  }

  private int rowsBottomY() {
    return viewY() + rowsBottomOffsetY;
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

    // Clamp scroll to the windowed range so resizing or trimming results doesn't strand the user.
    int maxOffset = Math.max(0, visibleResults.size() - visibleRows);
    if (scrollOffset > maxOffset) {
      scrollOffset = maxOffset;
    }
    if (scrollOffset < 0) {
      scrollOffset = 0;
    }

    for (int i = 0; i < rowButtons.size(); i++) {
      RuleRowButton row = rowButtons.get(i);
      if (i >= visibleRows) {
        // Row exists in the widget pool but is hidden because the current panel height can't fit
        // it. relayout() also parks it off-screen so a stale hover can't fire.
        row.visible = false;
        continue;
      }
      int candidateIndex = i + scrollOffset;
      if (candidateIndex >= visibleResults.size()) {
        row.visible = false;
        continue;
      }
      ItemCandidate candidate = visibleResults.get(candidateIndex);
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
    scrollOffset = 0;
    refresh();
  }

  /** Called by the inventory mixin when the user wheels over the rules content area. */
  public boolean mouseScrolledInRows(double mouseX, double mouseY, double amount) {
    if (!visible
        || rowButtons.isEmpty()
        || mouseY < rowsTopY()
        || mouseY > rowsBottomY()
        || mouseX < viewX()
        || mouseX > viewX() + viewWidth()) {
      return false;
    }
    int maxOffset = Math.max(0, visibleResults.size() - visibleRows);
    if (maxOffset == 0) {
      return false;
    }
    int newOffset = scrollOffset - (int) Math.signum(amount);
    if (newOffset < 0) {
      newOffset = 0;
    }
    if (newOffset > maxOffset) {
      newOffset = maxOffset;
    }
    if (newOffset != scrollOffset) {
      scrollOffset = newOffset;
      refresh();
    }
    return true;
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

    // When showing current rules, only a double click removes the rule. Single click is a no-op
    // so an accidental tap doesn't immediately destroy data.
    long now = System.currentTimeMillis();
    boolean doubleClick =
        now - lastClickTime < DOUBLE_CLICK_MS
            && clickedCandidate.itemId().equals(lastClickedItemId);
    lastClickTime = now;
    lastClickedItemId = clickedCandidate.itemId();
    if (doubleClick) {
      RuleMutations.removeFromActiveProfile(clickedCandidate.itemId());
      refresh();
    }
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
    int viewX = viewX();
    int viewY = viewY();
    int viewWidth = viewWidth();

    // Bulk bar above the rows: "N results" on left, modifier hint with kbd pills on right.
    int bulkY = viewY + bulkOffsetY;
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
      int areaHeight = rowsBottomY() - rowsTopY();
      int centerY = rowsTopY() + areaHeight / 2;
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
      // Use the registry's translated display name so casing matches "Diamond Sword" not the raw
      // lowercase "diamond sword" path. Falls back to the path with title-casing if the item is
      // missing from the registry (modded item that was removed).
      String displayName =
          item != null ? item.getName().getString() : titleCase(prettyName(itemId));
      candidates.add(new ItemCandidate(itemId, displayName, namespaceOf(itemId), item));
    }
    return candidates;
  }

  private static String titleCase(String raw) {
    if (raw == null || raw.isEmpty()) {
      return "";
    }
    String[] parts = raw.split(" ");
    StringBuilder out = new StringBuilder();
    for (String part : parts) {
      if (part.isEmpty()) {
        continue;
      }
      if (!out.isEmpty()) {
        out.append(' ');
      }
      out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }
    return out.toString();
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
