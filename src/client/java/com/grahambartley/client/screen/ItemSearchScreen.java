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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ItemSearchScreen extends Screen {
  private static final int ROW_HEIGHT = 22;
  private static final int MIN_ROWS_PER_PAGE = 4;
  private static List<ItemSearchController.ItemCandidate> cachedAllItems;

  private final RuleListScreen parent;
  private TextFieldWidget searchField;
  private ButtonWidget toggleButton;
  private ButtonWidget previousPageButton;
  private ButtonWidget nextPageButton;
  private List<ItemSearchController.ItemCandidate> filteredItems = List.of();
  private String lastFilterQuery = "";
  private int selectedIndex = -1;
  private int pageStart;
  private int rowsPerPage = MIN_ROWS_PER_PAGE;
  private int listTop;
  private int helperTextY;

  public ItemSearchScreen(RuleListScreen parent) {
    super(Text.literal("Item Search"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int top = this.height / 5;
    int pagerY = this.height - 76;
    int toggleY = this.height - 52;
    int backY = this.height - 28;
    listTop = top + 26;
    helperTextY = pagerY - 12;
    int availableListHeight = Math.max(ROW_HEIGHT, helperTextY - 8 - listTop);
    rowsPerPage = Math.max(MIN_ROWS_PER_PAGE, availableListHeight / ROW_HEIGHT);
    searchField =
        new TextFieldWidget(this.textRenderer, left, top, 200, 20, Text.literal("Search items"));
    searchField.setMaxLength(100);
    searchField.setChangedListener(
        ignored -> {
          selectedIndex = -1;
          pageStart = 0;
          invalidateFilter();
        });
    addDrawableChild(searchField);
    setFocused(searchField);

    toggleButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add"), button -> toggleSelected())
                .dimensions(left, toggleY, 200, 20)
                .build());
    previousPageButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Prev"), button -> previousPage())
                .dimensions(left, pagerY, 97, 20)
                .build());
    nextPageButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Next"), button -> nextPage())
                .dimensions(left + 103, pagerY, 97, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left, backY, 200, 20)
            .build());

    recomputeFilter();
    refreshButtonState(filteredItems);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    for (int row = 0; row < rowsPerPage; row++) {
      int absoluteIndex = pageStart + row;
      if (absoluteIndex >= visible.size()) {
        break;
      }
      ItemSearchController.ItemCandidate candidate = visible.get(absoluteIndex);
      int color = absoluteIndex == selectedIndex ? 0xFFF3B0 : 0xDADADA;
      context.drawTextWithShadow(
          textRenderer,
          Text.literal(candidate.displayName() + " [" + candidate.namespace() + "]"),
          this.width / 2 - 78,
          listTop + row * ROW_HEIGHT,
          color);
      context.drawTextWithShadow(
          textRenderer,
          Text.literal(candidate.itemId()),
          this.width / 2 - 78,
          listTop + row * ROW_HEIGHT + 10,
          0x9A9A9A);
      context.drawItem(
          new ItemStack(candidate.item()), this.width / 2 - 98, listTop + row * ROW_HEIGHT);
    }
    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Search by name, id, namespace"),
        this.width / 2 - 100,
        helperTextY,
        0xB0B0B0);
    refreshButtonState(visible);
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    int left = this.width / 2 - 100;
    int listBottom = listTop + rowsPerPage * ROW_HEIGHT;
    if (mouseX < left || mouseX > left + 200 || mouseY < listTop || mouseY > listBottom) {
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

  private void toggleSelected() {
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    if (selectedIndex < 0 || selectedIndex >= visible.size()) {
      return;
    }
    parent.saveRuleToggle(visible.get(selectedIndex).itemId());
  }

  private void refreshButtonState(List<ItemSearchController.ItemCandidate> visible) {
    boolean editable =
        LootLockClient.getState()
            .getSnapshot()
            .map(LootLockPlayerData::isClientCanEdit)
            .orElse(false);
    boolean hasSelection = selectedIndex >= 0 && selectedIndex < visible.size();
    toggleButton.active = editable && hasSelection;
    previousPageButton.active = pageStart > 0;
    nextPageButton.active = pageStart + rowsPerPage < visible.size();

    if (!hasSelection) {
      toggleButton.setMessage(Text.literal("Add"));
      return;
    }
    Optional<LootLockProfile> activeProfile =
        LootLockClient.getState().getSnapshot().flatMap(LootLockPlayerData::getActiveProfile);
    String itemId = visible.get(selectedIndex).itemId();
    boolean contains =
        activeProfile
            .map(
                profile ->
                    profile.getRules().stream().map(RuleEntry::itemId).anyMatch(itemId::equals))
            .orElse(false);
    toggleButton.setMessage(Text.literal(contains ? "Remove" : "Add"));
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
