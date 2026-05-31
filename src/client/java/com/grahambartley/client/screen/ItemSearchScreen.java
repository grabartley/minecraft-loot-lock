package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class ItemSearchScreen extends Screen {
  private final RuleListScreen parent;
  private final List<ItemSearchController.ItemCandidate> allItems = new ArrayList<>();
  private TextFieldWidget searchField;
  private ButtonWidget toggleButton;
  private int selectedIndex = -1;

  public ItemSearchScreen(RuleListScreen parent) {
    super(Text.literal("Item Search"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int top = this.height / 5;
    searchField =
        new TextFieldWidget(this.textRenderer, left, top, 200, 20, Text.literal("Search items"));
    searchField.setMaxLength(100);
    addDrawableChild(searchField);

    toggleButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Add"), button -> toggleSelected())
                .dimensions(left, top + 132, 200, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left, top + 156, 200, 20)
            .build());

    hydrateItems();
    refreshButtonState();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    int top = this.height / 5 + 26;
    for (int i = 0; i < visible.size() && i < 6; i++) {
      ItemSearchController.ItemCandidate candidate = visible.get(i);
      int color = i == selectedIndex ? 0xFFF3B0 : 0xDADADA;
      context.drawTextWithShadow(
          textRenderer,
          Text.literal(candidate.displayName() + " [" + candidate.namespace() + "]"),
          this.width / 2 - 98,
          top + i * 16,
          color);
      context.drawTextWithShadow(
          textRenderer,
          Text.literal(candidate.itemId()),
          this.width / 2 - 98,
          top + i * 16 + 8,
          0x9A9A9A);
    }
    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Search by name, id, namespace"),
        this.width / 2 - 100,
        this.height / 5 + 118,
        0xB0B0B0);
    refreshButtonState();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    int listTop = this.height / 5 + 26;
    int left = this.width / 2 - 100;
    if (mouseX < left || mouseX > left + 200 || mouseY < listTop || mouseY > listTop + 96) {
      return false;
    }
    int index = (int) ((mouseY - listTop) / 16);
    if (index >= 0 && index < visibleItems().size()) {
      selectedIndex = index;
      refreshButtonState();
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

  private void hydrateItems() {
    allItems.clear();
    for (Item item : Registries.ITEM) {
      Identifier id = Registries.ITEM.getId(item);
      String itemId = id.toString();
      String name = item.getName().getString();
      allItems.add(new ItemSearchController.ItemCandidate(itemId, name, id.getNamespace()));
    }
  }

  private List<ItemSearchController.ItemCandidate> visibleItems() {
    return ItemSearchController.filter(allItems, searchField == null ? "" : searchField.getText());
  }

  private void toggleSelected() {
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    if (selectedIndex < 0 || selectedIndex >= visible.size()) {
      return;
    }
    parent.saveRuleToggle(visible.get(selectedIndex).itemId());
  }

  private void refreshButtonState() {
    List<ItemSearchController.ItemCandidate> visible = visibleItems();
    boolean editable =
        LootLockClient.getState()
            .getSnapshot()
            .map(LootLockPlayerData::isClientCanEdit)
            .orElse(false);
    boolean hasSelection = selectedIndex >= 0 && selectedIndex < visible.size();
    toggleButton.active = editable && hasSelection;

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
}
