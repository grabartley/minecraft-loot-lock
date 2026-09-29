package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.screen.ItemSearchController;
import com.grahambartley.lootlock.client.screen.ItemSearchController.ItemCandidate;
import com.grahambartley.lootlock.client.screen.RuleListController;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.text.LootLockLang;
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
import net.minecraft.util.Identifier;

public final class RulesTabView {
  public boolean isSearchFieldFocused() {
    return searchField != null && searchField.isFocused();
  }

  static final int BULK_BAR_HEIGHT = 12;
  static final int SEARCH_HEIGHT = 16;
  static final int FOOTER_HEIGHT = 16;
  static final int FOOTER_GAP = 4;

  static final int MAX_ROWS = 8;

  private static final long DOUBLE_CLICK_MS = 300L;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final RulesSelectionState selection = new RulesSelectionState();

  private LootLockInventoryPanel panel;
  private int visibleRows = 4;
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
  private boolean overlayHidden;
  private boolean searchHiddenByOverlay;
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
            Text.translatable(LootLockLang.RULES_SEARCH_FIELD));
    searchField.setMaxLength(64);
    searchField.setPlaceholder(Text.translatable(LootLockLang.RULES_SEARCH_PLACEHOLDER));
    searchField.setChangedListener(this::onSearchChanged);
    addDrawableChild.accept(searchField);
    widgets.add(searchField);

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
        ButtonWidget.builder(
                Text.translatable(LootLockLang.RULES_ADD_SELECTED), button -> addSelected())
            .dimensions(0, 0, 10, FOOTER_HEIGHT)
            .build();
    addDrawableChild.accept(addSelectedButton);
    widgets.add(addSelectedButton);

    clearAllButton =
        ButtonWidget.builder(
                Text.translatable(LootLockLang.RULES_CLEAR_ALL),
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

  public void relayout() {
    if (panel == null || searchField == null) {
      return;
    }
    int viewX = panel.getContentInsetX();
    int viewY = panel.getContentInsetY();
    int viewWidth = panel.getContentInsetWidth();
    int viewHeight = panel.getContentInsetHeight();

    searchOffsetY = 0;
    bulkOffsetY = SEARCH_HEIGHT + 4;
    rowsTopOffsetY = SEARCH_HEIGHT + BULK_BAR_HEIGHT + 4;
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

  public void clearSearch() {
    if (searchField != null && !searchField.getText().isEmpty()) {
      searchField.setText("");
    }
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
    applyWidgetVisibility();
    if (!visible) {
      selection.clear();
    }
  }

  public void setOverlayHidden(boolean overlayHidden, int overlayBottomY) {
    boolean searchUnderOverlay =
        overlayHidden && searchField != null && searchField.getY() < overlayBottomY;
    if (this.overlayHidden == overlayHidden && this.searchHiddenByOverlay == searchUnderOverlay) {
      return;
    }
    this.overlayHidden = overlayHidden;
    this.searchHiddenByOverlay = searchUnderOverlay;
    applyWidgetVisibility();
  }

  private void applyWidgetVisibility() {
    boolean widgetVisible = visible && !overlayHidden;
    boolean searchVisible = visible && !searchHiddenByOverlay;
    for (ClickableWidget widget : widgets) {
      if (widget == searchField) {
        widget.visible = searchVisible;
      } else {
        widget.visible = widgetVisible;
      }
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
    } else if (query.startsWith(RuleEntry.TAG_PREFIX)) {
      showingSearch = true;
      visibleResults =
          ItemSearchController.filter(
              RulesTagCatalog.all(), query.substring(RuleEntry.TAG_PREFIX.length()));
    } else {
      showingSearch = true;
      visibleResults = ItemSearchController.filter(RulesItemCatalog.all(), query);
    }

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
      row.setTooltip(Tooltip.of(rowTooltip(candidate.itemId())));
    }

    if (addSelectedButton != null) {
      addSelectedButton.visible = showingSearch;
      addSelectedButton.active = showingSearch && selection.size() > 0;
      int n = selection.size();
      addSelectedButton.setMessage(
          n == 0
              ? Text.translatable(LootLockLang.RULES_ADD_SELECTED)
              : Text.translatable(LootLockLang.RULES_ADD_SELECTED_COUNT, n));
    }
    if (clearAllButton != null) {
      if (showingSearch) {
        clearAllButton.visible = true;
        clearAllButton.setMessage(Text.translatable(LootLockLang.RULES_CLEAR_SEARCH));
      } else {
        clearAllButton.visible = !visibleResults.isEmpty();
        clearAllButton.setMessage(Text.translatable(LootLockLang.RULES_CLEAR_ALL));
      }
    }
  }

  void onSearchChanged(String value) {
    selection.clear();
    scrollOffset = 0;
    refresh();
  }

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
            Text.translatable(LootLockLang.RULES_CLEAR_CONFIRM_TITLE),
            Text.translatable(LootLockLang.RULES_CLEAR_CONFIRM_BODY)));
  }

  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!visible) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    int viewX = viewX();
    int viewY = viewY();
    int viewWidth = viewWidth();

    int bulkY = viewY + bulkOffsetY;
    int bulkCount = visibleResults.size();
    String bulkKey;
    if (showingSearch) {
      bulkKey =
          bulkCount == 1
              ? LootLockLang.RULES_BULK_RESULTS_ONE
              : LootLockLang.RULES_BULK_RESULTS_MANY;
    } else {
      bulkKey =
          bulkCount == 1 ? LootLockLang.RULES_BULK_RULES_ONE : LootLockLang.RULES_BULK_RULES_MANY;
    }
    context.drawText(
        client.textRenderer,
        Text.translatable(bulkKey, bulkCount),
        viewX,
        bulkY,
        Palette.INK_DIM,
        false);
    if (showingSearch) {
      drawHint(context, client, viewX + viewWidth, bulkY);
    }

    if (visibleResults.isEmpty()) {
      Text big =
          Text.translatable(
              showingSearch
                  ? LootLockLang.RULES_EMPTY_SEARCH_TITLE
                  : LootLockLang.RULES_EMPTY_PROFILE_TITLE);
      List<Text> subLines =
          showingSearch
              ? List.of(Text.translatable(LootLockLang.RULES_EMPTY_SEARCH_SUBTITLE))
              : List.of(
                  Text.translatable(LootLockLang.RULES_EMPTY_PROFILE_SUBTITLE_1),
                  Text.translatable(LootLockLang.RULES_EMPTY_PROFILE_SUBTITLE_2));
      int areaHeight = rowsBottomY() - rowsTopY();
      int centerY = rowsTopY() + areaHeight / 2;
      int bigWidth = client.textRenderer.getWidth(big);
      context.drawText(
          client.textRenderer,
          big,
          viewX + (viewWidth - bigWidth) / 2,
          centerY - 6,
          Palette.INK,
          false);
      int subY = centerY + 4;
      int lineHeight = 10;
      for (Text sub : subLines) {
        int subWidth = client.textRenderer.getWidth(sub);
        context.drawText(
            client.textRenderer,
            sub,
            viewX + (viewWidth - subWidth) / 2,
            subY,
            Palette.INK_DIM,
            false);
        subY += lineHeight;
      }
    }
  }

  private static void drawHint(DrawContext context, MinecraftClient client, int rightX, int y) {
    Text hint =
        Text.empty()
            .append(Text.translatable(LootLockLang.RULES_HINT_SHIFT).withColor(Palette.INK))
            .append(Text.translatable(LootLockLang.RULES_HINT_RANGE))
            .append(Text.translatable(LootLockLang.RULES_HINT_CTRL).withColor(Palette.INK))
            .append(Text.translatable(LootLockLang.RULES_HINT_PICK));
    int x = rightX - client.textRenderer.getWidth(hint);
    context.drawText(client.textRenderer, hint, x, y, Palette.INK_DIM, false);
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

  static Text rowTooltip(String entryId) {
    if (entryId == null) {
      return Text.empty();
    }
    if (!entryId.startsWith(RuleEntry.TAG_PREFIX)) {
      return Text.literal(entryId);
    }
    String tagPath = entryId.substring(RuleEntry.TAG_PREFIX.length());
    Identifier tagId = Identifier.tryParse(tagPath);
    if (tagId == null) {
      return Text.literal(entryId);
    }
    int count = RulesTagCatalog.resolvedCount(tagId);
    if (count < 0) {
      return Text.translatable(LootLockLang.RULES_TAG_TOOLTIP_UNKNOWN, entryId);
    }
    String key =
        count == 1 ? LootLockLang.RULES_TAG_TOOLTIP_ONE : LootLockLang.RULES_TAG_TOOLTIP_MANY;
    return Text.translatable(key, entryId, count);
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
