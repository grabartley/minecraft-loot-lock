package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.state.ClientDraftProfile;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ClientMutationSync;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class ProfileListScreen extends Screen {
  private static final int PROFILE_ROW_HEIGHT = 20;
  private static final int MIN_VISIBLE_PROFILE_ROWS = 5;
  private static final int LIST_WIDTH = 304;
  private static final int GRID_BUTTON_WIDTH = 150;
  private static final int GRID_GAP = 4;
  private static final int BACK_BUTTON_WIDTH = 200;
  private static final int TITLE_Y = 18;
  private static final int SUBTITLE_Y = 30;
  private static final int LIST_TOP = 52;

  private final Screen parent;
  private TextFieldWidget nameField;
  private ButtonWidget createButton;
  private ButtonWidget renameButton;
  private ButtonWidget duplicateButton;
  private ButtonWidget deleteButton;
  private ButtonWidget activateButton;
  private int selectedIndex;
  private int visibleProfileRows = MIN_VISIBLE_PROFILE_ROWS;
  private int statusLineY;
  private int inputLabelY;

  public ProfileListScreen(Screen parent) {
    super(Text.literal("Profiles"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int listLeft = this.width / 2 - LIST_WIDTH / 2;
    int gridLeft = this.width / 2 - LIST_WIDTH / 2;
    int rightColumnLeft = gridLeft + GRID_BUTTON_WIDTH + GRID_GAP;
    int backLeft = this.width / 2 - BACK_BUTTON_WIDTH / 2;
    int backY = this.height - 28;
    int deleteRowY = backY - 24;
    int createRowY = deleteRowY - 24;
    int activateRowY = createRowY - 24;
    int nameFieldY = activateRowY - 28;
    inputLabelY = nameFieldY - 10;
    statusLineY = inputLabelY - 16;
    int availableListHeight = Math.max(PROFILE_ROW_HEIGHT, statusLineY - 8 - LIST_TOP);
    visibleProfileRows =
        Math.max(MIN_VISIBLE_PROFILE_ROWS, availableListHeight / PROFILE_ROW_HEIGHT);

    nameField =
        new TextFieldWidget(
            this.textRenderer, listLeft, nameFieldY, LIST_WIDTH, 20, Text.literal("Profile Name"));
    nameField.setMaxLength(ProfileNameValidator.MAX_UI_PROFILE_NAME_LENGTH);
    addDrawableChild(nameField);
    setFocused(nameField);

    createButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Create"), button -> createProfile())
                .dimensions(gridLeft, createRowY, GRID_BUTTON_WIDTH, 20)
                .build());
    renameButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Rename"), button -> renameProfile())
                .dimensions(rightColumnLeft, activateRowY, GRID_BUTTON_WIDTH, 20)
                .build());
    duplicateButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Duplicate"), button -> duplicateProfile())
                .dimensions(rightColumnLeft, createRowY, GRID_BUTTON_WIDTH, 20)
                .build());

    deleteButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Delete"), button -> deleteProfile())
                .dimensions(gridLeft, deleteRowY, LIST_WIDTH, 20)
                .build());
    activateButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Activate"), button -> activateProfile())
                .dimensions(gridLeft, activateRowY, GRID_BUTTON_WIDTH, 20)
                .build());

    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(backLeft, backY, BACK_BUTTON_WIDTH, 20)
            .build());

    seedSelectionFromSnapshot();
    refreshButtonState();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, TITLE_Y, 0xFFFFFF);

    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      context.drawTextWithShadow(
          textRenderer, Text.literal("No synced data"), this.width / 2 - 100, LIST_TOP, 0xE06666);
      refreshButtonState();
      return;
    }

    LootLockPlayerData data = dataOptional.get();
    int listLeft = this.width / 2 - LIST_WIDTH / 2;
    Optional<Text> subtitle = activeSubtitle(data);
    subtitle.ifPresent(
        text ->
            context.drawCenteredTextWithShadow(
                textRenderer, text, this.width / 2, SUBTITLE_Y, 0xFFFFFF));

    List<LootLockProfile> profiles = data.getProfiles();
    for (int i = 0; i < profiles.size() && i < visibleProfileRows; i++) {
      LootLockProfile profile = profiles.get(i);
      if (profile == null) {
        continue;
      }
      int rowY = LIST_TOP + i * PROFILE_ROW_HEIGHT;
      boolean active = profile.getId().equals(data.getActiveProfileId());
      if (i == selectedIndex) {
        context.fill(
            listLeft, rowY - 1, listLeft + LIST_WIDTH, rowY + PROFILE_ROW_HEIGHT - 1, 0x40FFFFFF);
      }
      context.drawTextWithShadow(
          textRenderer,
          profileNameText(profile.getName(), active),
          listLeft + 4,
          rowY + 6,
          0xFFFFFF);
      if (active) {
        Text tag = activeTagText();
        context.drawTextWithShadow(
            textRenderer,
            tag,
            listLeft + LIST_WIDTH - 4 - textRenderer.getWidth(tag),
            rowY + 6,
            0xFFFFFF);
      }
    }

    context.drawTextWithShadow(
        textRenderer,
        listStatusText(profiles.size(), visibleProfileRows),
        listLeft,
        statusLineY,
        0xA0A0A0);
    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Profile name (max 32 chars):"),
        listLeft,
        inputLabelY,
        0xA0A0A0);

    refreshButtonState();
  }

  @Override
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (super.mouseClicked(mouseX, mouseY, button)) {
      return true;
    }

    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return false;
    }

    int listLeft = this.width / 2 - LIST_WIDTH / 2;
    int listBottom = LIST_TOP + visibleProfileRows * PROFILE_ROW_HEIGHT;
    if (mouseX < listLeft
        || mouseX > listLeft + LIST_WIDTH
        || mouseY < LIST_TOP
        || mouseY > listBottom) {
      return false;
    }

    int index = (int) ((mouseY - LIST_TOP) / PROFILE_ROW_HEIGHT);
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (index >= 0 && index < profiles.size()) {
      selectedIndex = index;
      nameField.setText(ProfileNameValidator.sanitize(profiles.get(index).getName()));
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

  private void createProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    String name = ProfileNameValidator.sanitize(nameField.getText());
    if (!ProfileNameValidator.isValid(name)) {
      return;
    }
    LootLockPlayerData data = dataOptional.get();
    ClientMutationSync.sendCreateRequest(data.getRevision(), name, null);
  }

  private void renameProfile() {
    mutateSelectedProfile(
        draft -> draft.setName(ProfileNameValidator.sanitize(nameField.getText())));
  }

  private void duplicateProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (selectedIndex < 0 || selectedIndex >= profiles.size()) {
      return;
    }
    LootLockProfile selected = profiles.get(selectedIndex);
    String duplicateName = ProfileUiController.nextDuplicateName(profiles, selected.getName());
    ClientMutationSync.sendCreateRequest(dataOptional.get().getRevision(), duplicateName, selected);
  }

  private void deleteProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (!ProfileUiController.canDelete(profiles)
        || selectedIndex < 0
        || selectedIndex >= profiles.size()) {
      return;
    }

    ClientMutationSync.sendDeleteRequest(
        dataOptional.get().getRevision(), profiles.get(selectedIndex).getId());
  }

  private void activateProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (selectedIndex < 0 || selectedIndex >= profiles.size()) {
      return;
    }
    ClientMutationSync.sendActivateRequest(
        dataOptional.get().getRevision(), profiles.get(selectedIndex).getId());
  }

  private void mutateSelectedProfile(Consumer<ClientDraftProfile> mutator) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> dataOptional = state.getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (selectedIndex < 0 || selectedIndex >= profiles.size()) {
      return;
    }

    LootLockProfile selected = profiles.get(selectedIndex);
    Optional<ClientDraftSaveRequest> saveRequest =
        state
            .beginDraft(selected.getId())
            .map(
                draft -> {
                  mutator.accept(draft);
                  return state.buildSaveRequest();
                })
            .orElse(Optional.empty());

    if (saveRequest.isEmpty()
        || !ProfileNameValidator.isValid(saveRequest.get().profile().getName())) {
      return;
    }
    ClientMutationSync.sendSaveRequest(saveRequest.get());
  }

  private void seedSelectionFromSnapshot() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty() || dataOptional.get().getProfiles().isEmpty()) {
      selectedIndex = -1;
      return;
    }

    selectedIndex = 0;
    nameField.setText(
        ProfileNameValidator.sanitize(dataOptional.get().getProfiles().get(0).getName()));
  }

  private void refreshButtonState() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    boolean editable = dataOptional.map(LootLockPlayerData::isClientCanEdit).orElse(false);
    boolean canDelete =
        dataOptional.map(data -> ProfileUiController.canDelete(data.getProfiles())).orElse(false);
    boolean hasSelection =
        dataOptional
            .map(data -> selectedIndex >= 0 && selectedIndex < data.getProfiles().size())
            .orElse(false);

    createButton.active = editable;
    renameButton.active = editable && hasSelection;
    duplicateButton.active = editable && hasSelection;
    activateButton.active = editable && hasSelection;
    deleteButton.active = editable && hasSelection && canDelete;
  }

  static Optional<Text> activeSubtitle(LootLockPlayerData data) {
    return data.getActiveProfile().map(profile -> activeSubtitle(profile.getName()));
  }

  static Text activeSubtitle(String activeName) {
    return Text.literal("Active: ")
        .formatted(Formatting.GRAY)
        .append(activeProfileNameText(activeName));
  }

  static MutableText activeProfileNameText(String name) {
    return Text.literal(name).formatted(Formatting.YELLOW);
  }

  static MutableText profileNameText(String name, boolean active) {
    return active ? activeProfileNameText(name) : Text.literal(name);
  }

  static MutableText activeTagText() {
    return Text.literal("★ active").formatted(Formatting.YELLOW);
  }

  static Text listStatusText(int profileCount, int visibleRows) {
    String status = profileCount + (profileCount == 1 ? " profile" : " profiles");
    if (profileCount <= visibleRows) {
      return Text.literal(status).formatted(Formatting.GRAY);
    }
    int pageCount = (int) Math.ceil((double) profileCount / Math.max(1, visibleRows));
    return Text.literal(status + ", Page 1/" + pageCount).formatted(Formatting.GRAY);
  }
}
