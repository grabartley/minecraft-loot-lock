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
  private static final int ROWS_PER_PAGE = 4;
  private static final long CLEAR_CONFIRM_TIMEOUT_MS = 3000L;

  private final Screen parent;
  private TextFieldWidget searchField;
  private ButtonWidget removeButton;
  private ButtonWidget clearButton;
  private ButtonWidget previousPageButton;
  private ButtonWidget nextPageButton;
  private List<RuleEntry> filteredRules = List.of();
  private String lastFilterQuery = "";
  private int selectedIndex = -1;
  private int pageStart;
  private boolean confirmClear;
  private long clearConfirmExpiresAt;

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
    searchField.setChangedListener(
        ignored -> {
          selectedIndex = -1;
          pageStart = 0;
          invalidateFilter();
        });
    addDrawableChild(searchField);

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal("Add Item"),
                button -> this.client.setScreen(new ItemSearchScreen(this)))
            .dimensions(left, top + 146, 97, 20)
            .build());
    removeButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeSelectedRule())
                .dimensions(left + 103, top + 146, 97, 20)
                .build());
    clearButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear All"), button -> clearRulesWithConfirm())
                .dimensions(left, top + 170, 200, 20)
                .build());
    previousPageButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Prev"), button -> previousPage())
                .dimensions(left, top + 122, 97, 20)
                .build());
    nextPageButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Next"), button -> nextPage())
                .dimensions(left + 103, top + 122, 97, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left, top + 194, 200, 20)
            .build());
    recomputeVisibleRules(activeProfile().orElse(null));
    refreshButtonState(activeProfile().orElse(null), filteredRules);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    if (confirmClear && System.currentTimeMillis() >= clearConfirmExpiresAt) {
      resetClearConfirmation();
    }

    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      context.drawTextWithShadow(
          textRenderer, Text.literal("No active profile"), this.width / 2 - 100, 40, 0xE06666);
      refreshButtonState(null, List.of());
      return;
    }

    LootLockProfile profile = profileOptional.get();
    List<RuleEntry> visible = visibleRules(profile);
    int listTop = this.height / 5 + 26;
    for (int row = 0; row < ROWS_PER_PAGE; row++) {
      int absoluteIndex = pageStart + row;
      if (absoluteIndex >= visible.size()) {
        break;
      }
      RuleEntry rule = visible.get(absoluteIndex);
      int color = absoluteIndex == selectedIndex ? 0xFFF3B0 : 0xDADADA;
      context.drawTextWithShadow(
          textRenderer,
          Text.literal(rule.itemId()),
          this.width / 2 - 98,
          listTop + row * 16,
          color);
    }

    List<RuleEntry> unresolved =
        RuleListController.unresolvedRules(profile.getRules(), this::isResolvableItemId);
    for (int i = 0; i < unresolved.size() && i < 2; i++) {
      context.drawTextWithShadow(
          textRenderer,
          Text.literal("Unresolved: " + unresolved.get(i).itemId()),
          this.width / 2 - 98,
          listTop + 68 + i * 10,
          0xE8A87C);
    }

    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Search id, name, or namespace in Add Item"),
        this.width / 2 - 100,
        this.height / 5 + 108,
        0xB0B0B0);

    if (profile.getMode() == FilterMode.ALLOWLIST && profile.getRules().isEmpty()) {
      context.drawTextWithShadow(
          textRenderer,
          Text.literal("Warning: allowlist has zero rules, all pickups blocked"),
          this.width / 2 - 100,
          this.height / 5 + 200,
          0xE06666);
    }
    refreshButtonState(profile, visible);
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
    int row = (int) ((mouseY - listTop) / 16);
    int absoluteIndex = pageStart + row;
    List<RuleEntry> visible = visibleRules(profileOptional.get());
    if (absoluteIndex >= 0 && absoluteIndex < visible.size()) {
      selectedIndex = absoluteIndex;
      refreshButtonState(profileOptional.get(), visible);
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
      clearConfirmExpiresAt = System.currentTimeMillis() + CLEAR_CONFIRM_TIMEOUT_MS;
      clearButton.setMessage(Text.literal("Confirm Clear"));
      return;
    }
    saveRules(List.of());
    resetClearConfirmation();
    selectedIndex = -1;
  }

  public void saveRuleToggle(String itemId) {
    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      return;
    }
    List<RuleEntry> rules = profileOptional.get().getRules();
    List<RuleEntry> next = RuleListController.toggleRule(rules, itemId);
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
    String query = searchField == null ? "" : searchField.getText();
    if (!query.equals(lastFilterQuery)) {
      recomputeVisibleRules(profile);
    }
    return filteredRules;
  }

  private boolean isResolvableItemId(String itemId) {
    Identifier identifier = Identifier.tryParse(itemId);
    return identifier != null && Registries.ITEM.containsId(identifier);
  }

  private void refreshButtonState(LootLockProfile profile, List<RuleEntry> visible) {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    boolean editable = dataOptional.map(LootLockPlayerData::isClientCanEdit).orElse(false);
    boolean validSelection = selectedIndex >= 0 && selectedIndex < visible.size();
    removeButton.active = editable && validSelection;
    clearButton.active =
        editable
            && profile != null
            && !RuleListController.dedupeRules(profile.getRules()).isEmpty();
    previousPageButton.active = pageStart > 0;
    nextPageButton.active = pageStart + ROWS_PER_PAGE < visible.size();
  }

  private void previousPage() {
    pageStart = Math.max(0, pageStart - ROWS_PER_PAGE);
  }

  private void nextPage() {
    List<RuleEntry> visible = activeProfile().map(this::visibleRules).orElse(new ArrayList<>());
    if (pageStart + ROWS_PER_PAGE < visible.size()) {
      pageStart += ROWS_PER_PAGE;
    }
  }

  private void invalidateFilter() {
    lastFilterQuery = "__invalidate__";
  }

  private void recomputeVisibleRules(LootLockProfile profile) {
    if (profile == null) {
      filteredRules = List.of();
      lastFilterQuery = searchField == null ? "" : searchField.getText();
      return;
    }
    List<RuleEntry> deduped = RuleListController.dedupeRules(profile.getRules());
    String query = searchField == null ? "" : searchField.getText();
    filteredRules = RuleListController.filterRules(deduped, query);
    lastFilterQuery = query;
    if (selectedIndex >= filteredRules.size()) {
      selectedIndex = -1;
    }
  }

  private void resetClearConfirmation() {
    confirmClear = false;
    clearConfirmExpiresAt = 0L;
    clearButton.setMessage(Text.literal("Clear All"));
  }
}
