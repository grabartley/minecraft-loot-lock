package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RuleEntry;
import com.grahambartley.network.ClientMutationSync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class RuleListScreen extends Screen {
  private final Screen parent;
  private TextFieldWidget searchField;
  private ButtonWidget removeButton;
  private ButtonWidget clearButton;
  private int selectedIndex = -1;
  private boolean confirmClear;

  public RuleListScreen(Screen parent) {
    super(Text.literal("Rules"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int top = this.height / 5;

    searchField =
        new TextFieldWidget(this.textRenderer, left, top, 200, 20, Text.literal("Search"));
    searchField.setMaxLength(80);
    addDrawableChild(searchField);

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal("Add Item"),
                button -> this.client.setScreen(new ItemSearchScreen(this)))
            .dimensions(left, top + 128, 97, 20)
            .build());
    removeButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeSelectedRule())
                .dimensions(left + 103, top + 128, 97, 20)
                .build());
    clearButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear All"), button -> clearRulesWithConfirm())
                .dimensions(left, top + 152, 200, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left, top + 176, 200, 20)
            .build());
    refreshButtonState();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      context.drawTextWithShadow(
          textRenderer, Text.literal("No active profile"), this.width / 2 - 100, 40, 0xE06666);
      refreshButtonState();
      return;
    }

    LootLockProfile profile = profileOptional.get();
    List<RuleEntry> visible = visibleRules(profile);
    int listTop = this.height / 5 + 26;
    for (int i = 0; i < visible.size() && i < 5; i++) {
      RuleEntry rule = visible.get(i);
      int color = i == selectedIndex ? 0xFFF3B0 : 0xDADADA;
      context.drawTextWithShadow(
          textRenderer, Text.literal(rule.itemId()), this.width / 2 - 98, listTop + i * 16, color);
    }

    List<RuleEntry> unresolved =
        RuleListController.unresolvedRules(profile.getRules(), this::isResolvableItemId);
    for (int i = 0; i < unresolved.size() && i < 2; i++) {
      context.drawTextWithShadow(
          textRenderer,
          Text.literal("Unresolved: " + unresolved.get(i).itemId()),
          this.width / 2 - 98,
          listTop + 84 + i * 10,
          0xE8A87C);
    }

    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Search id, name, or namespace in Add Item"),
        this.width / 2 - 100,
        this.height / 5 + 112,
        0xB0B0B0);

    if (profile.getMode() == FilterMode.ALLOWLIST && profile.getRules().isEmpty()) {
      context.drawTextWithShadow(
          textRenderer,
          Text.literal("Warning: allowlist has zero rules, all pickups blocked"),
          this.width / 2 - 100,
          this.height / 5 + 200,
          0xE06666);
    }
    refreshButtonState();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }
    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      return false;
    }
    int listTop = this.height / 5 + 26;
    int left = this.width / 2 - 100;
    if (mouseX < left || mouseX > left + 200 || mouseY < listTop || mouseY > listTop + 80) {
      return false;
    }
    int index = (int) ((mouseY - listTop) / 16);
    if (index >= 0 && index < visibleRules(profileOptional.get()).size()) {
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

  private void removeSelectedRule() {
    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      return;
    }
    List<RuleEntry> visible = visibleRules(profileOptional.get());
    if (selectedIndex < 0 || selectedIndex >= visible.size()) {
      return;
    }
    saveRules(
        RuleListController.withRuleRemoved(
            profileOptional.get().getRules(), visible.get(selectedIndex).itemId()));
    selectedIndex = -1;
  }

  private void clearRulesWithConfirm() {
    if (!confirmClear) {
      confirmClear = true;
      clearButton.setMessage(Text.literal("Confirm Clear"));
      return;
    }
    saveRules(List.of());
    confirmClear = false;
    clearButton.setMessage(Text.literal("Clear All"));
    selectedIndex = -1;
  }

  public void saveRuleToggle(String itemId) {
    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      return;
    }
    List<RuleEntry> rules = profileOptional.get().getRules();
    boolean contains = rules.stream().anyMatch(rule -> rule.itemId().equals(itemId));
    List<RuleEntry> next =
        contains
            ? RuleListController.withRuleRemoved(rules, itemId)
            : RuleListController.withRuleAdded(rules, itemId);
    saveRules(next);
  }

  private void saveRules(List<RuleEntry> nextRules) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> dataOptional = state.getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    UUID activeProfileId = dataOptional.get().getActiveProfileId();
    Optional<ClientDraftSaveRequest> saveRequest =
        state
            .beginDraft(activeProfileId)
            .map(
                draft -> {
                  draft.setRules(nextRules);
                  return state.buildSaveRequest();
                })
            .orElse(Optional.empty());
    saveRequest.ifPresent(ClientMutationSync::sendSaveRequest);
  }

  private Optional<LootLockProfile> activeProfile() {
    return LootLockClient.getState().getSnapshot().flatMap(LootLockPlayerData::getActiveProfile);
  }

  private List<RuleEntry> visibleRules(LootLockProfile profile) {
    List<RuleEntry> deduped = RuleListController.dedupeRules(profile.getRules());
    return RuleListController.filterRules(
        deduped, searchField == null ? "" : searchField.getText());
  }

  private boolean isResolvableItemId(String itemId) {
    Identifier identifier = Identifier.tryParse(itemId);
    return identifier != null && Registries.ITEM.containsId(identifier);
  }

  private void refreshButtonState() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    boolean editable = dataOptional.map(LootLockPlayerData::isClientCanEdit).orElse(false);
    List<RuleEntry> visible =
        dataOptional
            .flatMap(LootLockPlayerData::getActiveProfile)
            .map(this::visibleRules)
            .orElse(new ArrayList<>());
    boolean validSelection = selectedIndex >= 0 && selectedIndex < visible.size();
    removeButton.active = editable && validSelection;
    clearButton.active = editable;
  }
}
