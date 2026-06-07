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
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public final class RuleListScreen extends Screen {
  private static final int ROW_HEIGHT = 22;
  private static final int MIN_ROWS_PER_PAGE = 4;
  private static final long CLEAR_CONFIRM_TIMEOUT_MS = 3000L;
  private static final int LIST_WIDTH = 304;
  private static final int GRID_BUTTON_WIDTH = 150;
  private static final int GRID_GAP = 4;
  private static final int BACK_BUTTON_WIDTH = 200;
  private static final int TITLE_Y = 18;
  private static final int SUBTITLE_Y = 30;
  private static final int FILTER_Y = 54;

  private final Screen parent;
  private TextFieldWidget searchField;
  private ButtonWidget removeButton;
  private ButtonWidget clearButton;
  private ButtonWidget previousPageButton;
  private ButtonWidget nextPageButton;
  private List<RuleEntry> filteredRules = List.of();
  private String lastFilterQuery = "";
  private String lastRuleSignature = "";
  private int selectedIndex = -1;
  private int pageStart;
  private int rowsPerPage = MIN_ROWS_PER_PAGE;
  private int listTop;
  private int listLeft;
  private int statusLineY;
  private boolean confirmClear;
  private long clearConfirmExpiresAt;

  public RuleListScreen(Screen parent) {
    super(Text.literal("Rules"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    listLeft = this.width / 2 - LIST_WIDTH / 2;
    int rightColumn = listLeft + GRID_BUTTON_WIDTH + GRID_GAP;
    int backLeft = this.width / 2 - BACK_BUTTON_WIDTH / 2;

    int backY = this.height - 28;
    int clearY = backY - 24;
    int pagerY = clearY - 24;
    int addRemoveY = pagerY - 24;
    statusLineY = addRemoveY - 12;
    listTop = FILTER_Y + 26;

    int availableListHeight = Math.max(ROW_HEIGHT, statusLineY - 4 - listTop);
    rowsPerPage = Math.max(MIN_ROWS_PER_PAGE, availableListHeight / ROW_HEIGHT);

    searchField =
        new TextFieldWidget(
            this.textRenderer, listLeft, FILTER_Y, LIST_WIDTH, 20, Text.literal("Filter rules..."));
    searchField.setMaxLength(80);
    searchField.setPlaceholder(Text.literal("Filter rules..."));
    searchField.setChangedListener(
        ignored -> {
          selectedIndex = -1;
          pageStart = 0;
          invalidateFilter();
        });
    addDrawableChild(searchField);
    setFocused(searchField);

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal("Add Item"),
                button -> this.client.setScreen(new ItemSearchScreen(this)))
            .dimensions(listLeft, addRemoveY, GRID_BUTTON_WIDTH, 20)
            .build());
    removeButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Remove"), button -> removeSelectedRule())
                .dimensions(rightColumn, addRemoveY, GRID_BUTTON_WIDTH, 20)
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
    clearButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Clear All"), button -> clearRulesWithConfirm())
                .dimensions(listLeft, clearY, LIST_WIDTH, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(backLeft, backY, BACK_BUTTON_WIDTH, 20)
            .build());

    recomputeVisibleRules(activeProfile().orElse(null));
    refreshButtonState(activeProfile().orElse(null), filteredRules);
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, TITLE_Y, 0xFFFFFF);

    if (confirmClear && System.currentTimeMillis() >= clearConfirmExpiresAt) {
      resetClearConfirmation();
    }

    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      context.drawTextWithShadow(
          textRenderer, Text.literal("No active profile"), listLeft, SUBTITLE_Y + 4, 0xE06666);
      refreshButtonState(null, List.of());
      return;
    }

    LootLockProfile profile = profileOptional.get();
    subtitleText(context, profile);
    List<RuleEntry> visible = visibleRules(profile);

    for (int row = 0; row < rowsPerPage; row++) {
      int absoluteIndex = pageStart + row;
      if (absoluteIndex >= visible.size()) {
        break;
      }
      RuleEntry rule = visible.get(absoluteIndex);
      int rowY = listTop + row * ROW_HEIGHT;

      if (absoluteIndex == selectedIndex) {
        context.fill(listLeft, rowY - 1, listLeft + LIST_WIDTH, rowY + ROW_HEIGHT - 1, 0x40FFFFFF);
      }

      drawRuleItemIcon(context, rule.itemId(), listLeft + 2, rowY);
      Identifier identifier = Identifier.tryParse(rule.itemId());
      String displayName = rule.itemId();
      if (identifier != null && Registries.ITEM.containsId(identifier)) {
        displayName = Registries.ITEM.get(identifier).getName().getString();
      }
      context.drawTextWithShadow(
          textRenderer, Text.literal(displayName), listLeft + 24, rowY + 2, 0xDADADA);
      context.drawTextWithShadow(
          textRenderer, Text.literal(rule.itemId()), listLeft + 24, rowY + 12, 0x9A9A9A);
    }

    List<RuleEntry> unresolved =
        RuleListController.unresolvedRules(profile.getRules(), this::isResolvableItemId);
    for (int i = 0; i < unresolved.size() && i < 2; i++) {
      context.drawTextWithShadow(
          textRenderer,
          Text.literal("Unresolved: " + unresolved.get(i).itemId()),
          listLeft,
          statusLineY - 12 + i * 10,
          0xE8A87C);
    }

    int totalRules = RuleListController.dedupeRules(profile.getRules()).size();
    int totalPages = Math.max(1, (int) Math.ceil((double) visible.size() / rowsPerPage));
    String status = totalRules + " rules";
    if (totalPages > 1) {
      int currentPage = (pageStart / rowsPerPage) + 1;
      status += " \u00b7 Page " + currentPage + "/" + totalPages;
    }
    context.drawTextWithShadow(
        textRenderer,
        Text.literal(status).formatted(Formatting.GRAY),
        listLeft,
        statusLineY,
        0xA0A0A0);

    if (profile.getMode() == FilterMode.ALLOWLIST && profile.getRules().isEmpty()) {
      context.drawTextWithShadow(
          textRenderer,
          Text.literal("Warning: allowlist has zero rules, all pickups blocked")
              .formatted(Formatting.RED),
          listLeft,
          statusLineY + 12,
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
    int listBottom = listTop + rowsPerPage * ROW_HEIGHT;
    if (mouseX < listLeft
        || mouseX > listLeft + LIST_WIDTH
        || mouseY < listTop
        || mouseY > listBottom) {
      return false;
    }
    int row = (int) ((mouseY - listTop) / ROW_HEIGHT);
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

  private void subtitleText(DrawContext context, LootLockProfile profile) {
    context.drawCenteredTextWithShadow(
        textRenderer, subtitle(profile), this.width / 2, SUBTITLE_Y, 0xFFFFFF);
  }

  static Text subtitle(LootLockProfile profile) {
    return Text.literal("Profile: ")
        .formatted(Formatting.GRAY)
        .append(Text.literal(profile.getName()).formatted(Formatting.YELLOW))
        .append(Text.literal(" \u00b7 Mode: ").formatted(Formatting.GRAY))
        .append(Text.literal(friendlyMode(profile.getMode())).formatted(Formatting.YELLOW));
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
    recomputeVisibleRules(activeProfile().orElse(null));
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
    recomputeVisibleRules(activeProfile().orElse(null));
  }

  public void saveRuleToggle(String itemId) {
    Optional<LootLockProfile> profileOptional = activeProfile();
    if (profileOptional.isEmpty()) {
      return;
    }
    List<RuleEntry> rules = profileOptional.get().getRules();
    List<RuleEntry> next = RuleListController.toggleRule(rules, itemId);
    saveRules(next);
    recomputeVisibleRules(activeProfile().orElse(null));
  }

  private void saveRules(List<RuleEntry> nextRules) {
    String query = searchField == null ? "" : searchField.getText();
    List<RuleEntry> deduped = RuleListController.dedupeRules(nextRules);
    filteredRules = RuleListController.filterRules(deduped, query);
    lastFilterQuery = query;
    lastRuleSignature = rulesSignature(deduped);
    if (selectedIndex >= filteredRules.size()) {
      selectedIndex = -1;
    }
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
    String profileSignature = rulesSignature(profile.getRules());
    if (!query.equals(lastFilterQuery) || !profileSignature.equals(lastRuleSignature)) {
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
    nextPageButton.active = pageStart + rowsPerPage < visible.size();
  }

  private void previousPage() {
    pageStart = Math.max(0, pageStart - rowsPerPage);
  }

  private void nextPage() {
    List<RuleEntry> visible = activeProfile().map(this::visibleRules).orElse(new ArrayList<>());
    if (pageStart + rowsPerPage < visible.size()) {
      pageStart += rowsPerPage;
    }
  }

  private void invalidateFilter() {
    lastFilterQuery = "__invalidate__";
  }

  private void recomputeVisibleRules(LootLockProfile profile) {
    if (profile == null) {
      filteredRules = List.of();
      lastFilterQuery = searchField == null ? "" : searchField.getText();
      lastRuleSignature = "";
      return;
    }
    List<RuleEntry> deduped = RuleListController.dedupeRules(profile.getRules());
    String query = searchField == null ? "" : searchField.getText();
    filteredRules = RuleListController.filterRules(deduped, query);
    lastFilterQuery = query;
    lastRuleSignature = rulesSignature(deduped);
    if (selectedIndex >= filteredRules.size()) {
      selectedIndex = -1;
    }
  }

  private void resetClearConfirmation() {
    confirmClear = false;
    clearConfirmExpiresAt = 0L;
    clearButton.setMessage(Text.literal("Clear All"));
  }

  private String rulesSignature(List<RuleEntry> rules) {
    StringBuilder builder = new StringBuilder();
    for (RuleEntry rule : RuleListController.dedupeRules(rules)) {
      builder.append(rule.itemId()).append(';');
    }
    return builder.toString();
  }

  private void drawRuleItemIcon(DrawContext context, String itemId, int x, int y) {
    Identifier identifier = Identifier.tryParse(itemId);
    if (identifier == null || !Registries.ITEM.containsId(identifier)) {
      return;
    }
    context.drawItem(new ItemStack(Registries.ITEM.get(identifier)), x, y);
  }

  static String friendlyMode(FilterMode mode) {
    if (mode == FilterMode.ALLOWLIST) {
      return "Allowlist";
    }
    return "Denylist";
  }
}
