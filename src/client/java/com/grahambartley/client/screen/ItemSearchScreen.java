package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.util.InputUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public final class ItemSearchScreen extends Screen {
  private static final int ROW_HEIGHT = 22;
  private static final int MIN_ROWS_PER_PAGE = 4;
  private static final int LIST_WIDTH = 304;
  private static final int GRID_BUTTON_WIDTH = 150;
  private static final int GRID_GAP = 4;
  private static final int TITLE_Y = 18;
  private static final int SUBTITLE_Y = 30;
  private static final long DOUBLE_CLICK_MS = 500L;
  private static List<ItemSearchController.ItemCandidate> cachedAllItems;

  private final RuleListScreen parent;
  private TextFieldWidget searchField;
  private ButtonWidget addSelectedButton;
  private ButtonWidget previousPageButton;
  private ButtonWidget nextPageButton;
  private List<ItemSearchController.ItemCandidate> filteredItems = List.of();
  private String lastFilterQuery = "";
  private String lastRuleSignature = "";
  private List<String> selectedItemIds = List.of();
  private int lastClickedIndex = -1;
  private long lastClickTime;
  private int pageStart;
  private int rowsPerPage = MIN_ROWS_PER_PAGE;
  private int listTop;
  private int statusLineY;
  private int listLeft;
  private String feedbackMessage;
  private long feedbackExpiresAt;

  public ItemSearchScreen(RuleListScreen parent) {
    super(Text.literal("Item Search"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    listLeft = this.width / 2 - LIST_WIDTH / 2;
    int rightColumn = listLeft + GRID_BUTTON_WIDTH + GRID_GAP;

    int pagerY = this.height - 28;
    int addY = pagerY - 24;
    statusLineY = addY - 14;
    listTop = 80;
    int availableListHeight = Math.max(ROW_HEIGHT, statusLineY - 4 - listTop);
    rowsPerPage = Math.max(MIN_ROWS_PER_PAGE, availableListHeight / ROW_HEIGHT);

    searchField =
        new TextFieldWidget(
            this.textRenderer,
            listLeft,
            SUBTITLE_Y + 24,
            LIST_WIDTH,
            20,
            Text.literal("Search items"));
    searchField.setMaxLength(100);
    searchField.setPlaceholder(Text.literal("Search items..."));
    searchField.setChangedListener(
        ignored -> {
          selectedItemIds = List.of();
          lastClickedIndex = -1;
          pageStart = 0;
          invalidateFilter();
        });
    addDrawableChild(searchField);
    setFocused(searchField);

    addSelectedButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add Selected"), button -> addSelected())
                .dimensions(listLeft, addY, GRID_BUTTON_WIDTH, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(rightColumn, addY, GRID_BUTTON_WIDTH, 20)
            .build());
    previousPageButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Prev"), button -> previousPage())
                .dimensions(listLeft, pagerY, GRID_BUTTON_WIDTH, 20)
                .build());
    nextPageButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Next"), button -> nextPage())
                .dimensions(rightColumn, pagerY, GRID_BUTTON_WIDTH, 20)
                .build());

    recomputeFilter();
    refreshButtonState(filteredItems);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, TITLE_Y, 0xFFFFFF);

    String profileName =
        LootLockClient.getState()
            .getSnapshot()
            .flatMap(LootLockPlayerData::getActiveProfile)
            .map(p -> p.getName())
            .orElse("Unknown");
    context.drawCenteredTextWithShadow(
        textRenderer, subtitle(profileName), this.width / 2, SUBTITLE_Y, 0xFFFFFF);

    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    for (int row = 0; row < rowsPerPage; row++) {
      int absoluteIndex = pageStart + row;
      if (absoluteIndex >= visible.size()) {
        break;
      }
      ItemSearchController.ItemCandidate candidate = visible.get(absoluteIndex);
      int rowY = listTop + row * ROW_HEIGHT;

      if (selectedItemIds.contains(candidate.itemId())) {
        context.fill(listLeft, rowY - 1, listLeft + LIST_WIDTH, rowY + ROW_HEIGHT - 1, 0x40FFFFFF);
      }

      context.drawItem(new ItemStack(candidate.item()), listLeft + 2, rowY);
      context.drawTextWithShadow(
          textRenderer, Text.literal(candidate.displayName()), listLeft + 24, rowY + 2, 0xDADADA);
      context.drawTextWithShadow(
          textRenderer, Text.literal(candidate.itemId()), listLeft + 24, rowY + 12, 0x9A9A9A);
    }

    int totalResults = visible.size();
    int totalPages = Math.max(1, (int) Math.ceil((double) totalResults / rowsPerPage));
    String status = totalResults + " results";
    if (totalPages > 1) {
      int currentPage = Math.min(totalPages, (pageStart / rowsPerPage) + 1);
      status += " \u00b7 Page " + currentPage + "/" + totalPages;
    }
    context.drawTextWithShadow(textRenderer, Text.literal(status), listLeft, statusLineY, 0xA0A0A0);

    if (feedbackMessage != null) {
      if (System.currentTimeMillis() < feedbackExpiresAt) {
        context.drawTextWithShadow(
            textRenderer,
            Text.literal(feedbackMessage),
            addSelectedButton.getX() + addSelectedButton.getWidth() + 8,
            statusLineY,
            0x55FF55);
      } else {
        feedbackMessage = null;
      }
    }

    refreshButtonState(visible);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    int listBottom = listTop + rowsPerPage * ROW_HEIGHT;
    if (mouseX < listLeft
        || mouseX > listLeft + LIST_WIDTH
        || mouseY < listTop
        || mouseY > listBottom) {
      return false;
    }
    int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
    int absoluteIndex = pageStart + row;
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    if (absoluteIndex < 0 || absoluteIndex >= visible.size()) {
      return false;
    }

    boolean additiveSelection = isAdditiveSelection();
    boolean shiftDown = Screen.hasShiftDown();
    long now = System.currentTimeMillis();
    boolean isDoubleClick =
        isPrimaryDoubleClick(
            button,
            additiveSelection,
            shiftDown,
            lastClickedIndex,
            absoluteIndex,
            lastClickTime,
            now);
    lastClickTime = now;

    ItemSearchController.SelectionState selectionState =
        ItemSearchController.select(
            visible,
            selectedItemIds,
            lastClickedIndex,
            absoluteIndex,
            additiveSelection,
            shiftDown);
    selectedItemIds = selectionState.selectedItemIds();
    lastClickedIndex = selectionState.lastClickedIndex();
    refreshButtonState(visible);

    if (isDoubleClick) {
      addSelected();
    }
    return true;
  }

  @Override
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      if (!selectedItemIds.isEmpty()) {
        selectedItemIds = List.of();
        lastClickedIndex = -1;
        refreshButtonState(visibleItems());
        return true;
      }
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.setScreen(parent);
    }
  }

  private static boolean isUnobtainable(Item item) {
    return UnobtainableItems.isUnobtainable(item);
  }

  private static List<ItemSearchController.ItemCandidate> allItems() {
    if (cachedAllItems != null) {
      return cachedAllItems;
    }

    List<ItemSearchController.ItemCandidate> built = new ArrayList<>();
    for (Item item : Registries.ITEM) {
      if (isUnobtainable(item)) {
        continue;
      }
      Identifier id = Registries.ITEM.getId(item);
      String itemId = id.toString();
      String name = item.getName().getString();
      built.add(new ItemSearchController.ItemCandidate(itemId, name, id.getNamespace(), item));
    }
    cachedAllItems = Collections.unmodifiableList(built);
    return cachedAllItems;
  }

  private List<ItemSearchController.ItemCandidate> visibleItems() {
    String query = searchField == null ? "" : searchField.getText();
    String ruleSignature = activeRuleSignature();
    if (!query.equals(lastFilterQuery) || !ruleSignature.equals(lastRuleSignature)) {
      recomputeFilter();
    }
    return filteredItems;
  }

  private void addSelected() {
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    if (selectedItemIds.isEmpty()) {
      return;
    }
    List<String> itemIds =
        ItemSearchController.selectedItemIdsInVisibleOrder(visible, selectedItemIds);
    int added = parent.addRules(itemIds);
    feedbackMessage = "Added " + added + " item" + (added != 1 ? "s" : "");
    feedbackExpiresAt = System.currentTimeMillis() + 2000L;
  }

  private void refreshButtonState(List<ItemSearchController.ItemCandidate> visible) {
    boolean editable =
        LootLockClient.getState()
            .getSnapshot()
            .map(LootLockPlayerData::isClientCanEdit)
            .orElse(false);
    boolean hasSelection = !selectedItemIds.isEmpty();
    addSelectedButton.active = editable && hasSelection;
    previousPageButton.active = pageStart > 0;
    nextPageButton.active = pageStart + rowsPerPage < visible.size();
  }

  private void previousPage() {
    pageStart = Math.max(0, pageStart - rowsPerPage);
  }

  private void nextPage() {
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    if (pageStart + rowsPerPage < visible.size()) {
      pageStart += rowsPerPage;
    }
  }

  static Text subtitle(String profileName) {
    return Text.literal("Adding to ")
        .formatted(Formatting.GRAY)
        .append(Text.literal(profileName).formatted(Formatting.YELLOW));
  }

  /**
   * Returns true when a click should add to (rather than replace) the current selection.
   *
   * <p>{@code controlDown} covers Ctrl on all platforms. {@code commandDown} covers the Cmd key on
   * macOS, which is the standard additive modifier in Mac UI conventions across Finder, file
   * dialogs, and every major Mac application.
   */
  static boolean isAdditiveSelectionClick(boolean controlDown, boolean commandDown) {
    return controlDown || commandDown;
  }

  static boolean isPrimaryDoubleClick(
      int button,
      boolean additiveSelection,
      boolean shiftDown,
      int lastClickedIndex,
      int absoluteIndex,
      long lastClickTime,
      long now) {
    return button == 0
        && !additiveSelection
        && !shiftDown
        && lastClickedIndex >= 0
        && absoluteIndex == lastClickedIndex
        && (now - lastClickTime) < DOUBLE_CLICK_MS;
  }

  private boolean isAdditiveSelection() {
    long windowHandle = this.client != null ? this.client.getWindow().getHandle() : -1L;
    return isAdditiveSelectionClick(Screen.hasControlDown(), isCommandDown(windowHandle));
  }

  static boolean isCommandDown(long windowHandle) {
    if (windowHandle < 0) {
      return false;
    }
    return InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_LEFT_SUPER)
        || InputUtil.isKeyPressed(windowHandle, GLFW.GLFW_KEY_RIGHT_SUPER);
  }

  private void invalidateFilter() {
    lastFilterQuery = "__invalidate__";
    selectedItemIds = List.of();
    lastClickedIndex = -1;
  }

  private void recomputeFilter() {
    Optional<LootLockProfile> profile =
        LootLockClient.getState().getSnapshot().flatMap(LootLockPlayerData::getActiveProfile);
    String query = searchField == null ? "" : searchField.getText();
    filteredItems =
        ItemSearchController.filter(
            allItems(),
            query,
            RuleListController.itemIdSet(profile.map(LootLockProfile::getRules).orElse(List.of())));
    lastFilterQuery = query;
    lastRuleSignature =
        profile.map(LootLockProfile::getRules).map(ItemSearchScreen::rulesSignature).orElse("");
    selectedItemIds = ItemSearchController.retainVisibleSelection(filteredItems, selectedItemIds);
    if (lastClickedIndex >= filteredItems.size()) {
      lastClickedIndex = -1;
    }
  }

  private String activeRuleSignature() {
    return LootLockClient.getState()
        .getSnapshot()
        .flatMap(LootLockPlayerData::getActiveProfile)
        .map(LootLockProfile::getRules)
        .map(ItemSearchScreen::rulesSignature)
        .orElse("");
  }

  private static String rulesSignature(List<RuleEntry> rules) {
    return String.join(",", RuleListController.itemIdSet(rules));
  }
}
