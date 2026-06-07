package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.data.LootLockPlayerData;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class ItemSearchScreen extends Screen {
  private static final int ROW_HEIGHT = 22;
  private static final int MIN_ROWS_PER_PAGE = 4;
  private static final int LIST_WIDTH = 304;
  private static final int GRID_BUTTON_WIDTH = 150;
  private static final int GRID_GAP = 4;
  private static final int TITLE_Y = 18;
  private static final int SUBTITLE_Y = 30;
  private static List<ItemSearchController.ItemCandidate> cachedAllItems;

  private final RuleListScreen parent;
  private TextFieldWidget searchField;
  private ButtonWidget addSelectedButton;
  private ButtonWidget previousPageButton;
  private ButtonWidget nextPageButton;
  private List<ItemSearchController.ItemCandidate> filteredItems = List.of();
  private String lastFilterQuery = "";
  private int selectedIndex = -1;
  private int pageStart;
  private int rowsPerPage = MIN_ROWS_PER_PAGE;
  private int listTop;
  private int statusLineY;
  private int listLeft;

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
    searchField.setChangedListener(
        ignored -> {
          selectedIndex = -1;
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

      if (absoluteIndex == selectedIndex) {
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
    if (absoluteIndex >= 0 && absoluteIndex < visible.size()) {
      selectedIndex = absoluteIndex;
      refreshButtonState(visible);
      return true;
    }
    return false;
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.setScreen(parent);
    }
  }

  private static List<ItemSearchController.ItemCandidate> allItems() {
    if (cachedAllItems != null) {
      return cachedAllItems;
    }

    List<ItemSearchController.ItemCandidate> built = new ArrayList<>();
    for (Item item : Registries.ITEM) {
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
    if (!query.equals(lastFilterQuery)) {
      recomputeFilter();
    }
    return filteredItems;
  }

  private void addSelected() {
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    if (selectedIndex < 0 || selectedIndex >= visible.size()) {
      return;
    }
    parent.saveRuleToggle(visible.get(selectedIndex).itemId());
    close();
  }

  private void refreshButtonState(List<ItemSearchController.ItemCandidate> visible) {
    boolean editable =
        LootLockClient.getState()
            .getSnapshot()
            .map(LootLockPlayerData::isClientCanEdit)
            .orElse(false);
    boolean hasSelection = selectedIndex >= 0 && selectedIndex < visible.size();
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

  private void invalidateFilter() {
    lastFilterQuery = "__invalidate__";
  }

  private void recomputeFilter() {
    String query = searchField == null ? "" : searchField.getText();
    filteredItems = ItemSearchController.filter(allItems(), query);
    lastFilterQuery = query;
    if (selectedIndex >= filteredItems.size()) {
      selectedIndex = -1;
    }
  }
}
