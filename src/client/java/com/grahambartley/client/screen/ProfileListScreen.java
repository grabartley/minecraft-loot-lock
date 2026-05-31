package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
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
import net.minecraft.text.Text;

public final class ProfileListScreen extends Screen {
  private static final int PROFILE_ROW_HEIGHT = 20;
  private static final int MIN_VISIBLE_PROFILE_ROWS = 5;

  private final Screen parent;
  private TextFieldWidget nameField;
  private ButtonWidget createButton;
  private ButtonWidget renameButton;
  private ButtonWidget duplicateButton;
  private ButtonWidget deleteButton;
  private ButtonWidget activateButton;
  private int selectedIndex;
  private int listTop;
  private int visibleProfileRows = MIN_VISIBLE_PROFILE_ROWS;
  private int helperTextY;

  public ProfileListScreen(Screen parent) {
    super(Text.literal("Profiles"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int backY = this.height - 28;
    int actionRowY = this.height - 52;
    int editRowY = this.height - 76;
    int nameFieldY = this.height - 100;
    helperTextY = nameFieldY - 10;
    listTop = 36;
    int availableListHeight = Math.max(PROFILE_ROW_HEIGHT, helperTextY - 12 - listTop);
    visibleProfileRows =
        Math.max(MIN_VISIBLE_PROFILE_ROWS, availableListHeight / PROFILE_ROW_HEIGHT);

    nameField =
        new TextFieldWidget(this.textRenderer, left, nameFieldY, 200, 20, Text.literal("Name"));
    nameField.setMaxLength(ProfileNameValidator.MAX_UI_PROFILE_NAME_LENGTH);
    addDrawableChild(nameField);
    setFocused(nameField);

    createButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Create"), button -> createProfile())
                .dimensions(left, editRowY, 64, 20)
                .build());
    renameButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Rename"), button -> renameProfile())
                .dimensions(left + 68, editRowY, 64, 20)
                .build());
    duplicateButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Duplicate"), button -> duplicateProfile())
                .dimensions(left + 136, editRowY, 64, 20)
                .build());

    deleteButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Delete"), button -> deleteProfile())
                .dimensions(left, actionRowY, 97, 20)
                .build());
    activateButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Activate"), button -> activateProfile())
                .dimensions(left + 103, actionRowY, 97, 20)
                .build());

    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left, backY, 200, 20)
            .build());

    seedSelectionFromSnapshot();
    refreshButtonState();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      context.drawTextWithShadow(
          textRenderer, Text.literal("No synced data"), this.width / 2 - 100, 40, 0xE06666);
      refreshButtonState();
      return;
    }

    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    for (int i = 0; i < profiles.size() && i < visibleProfileRows; i++) {
      LootLockProfile profile = profiles.get(i);
      if (profile == null) {
        continue;
      }
      int color = i == selectedIndex ? 0xFFF3B0 : 0xDADADA;
      String prefix = profile.getId().equals(dataOptional.get().getActiveProfileId()) ? "* " : "  ";
      context.drawTextWithShadow(
          textRenderer,
          Text.literal(prefix + profile.getName()),
          this.width / 2 - 98,
          listTop + i * PROFILE_ROW_HEIGHT,
          color);
    }

    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Click profile rows to select."),
        this.width / 2 - 100,
        helperTextY - 8,
        0xB0B0B0);
    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Name (max 32 chars):"),
        this.width / 2 - 100,
        helperTextY,
        0xB0B0B0);
    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Showing first " + visibleProfileRows + " profiles"),
        this.width / 2 - 100,
        helperTextY - 18,
        0x8F8F8F);

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

    int left = this.width / 2 - 100;
    int listBottom = listTop + visibleProfileRows * PROFILE_ROW_HEIGHT;
    if (mouseX < left || mouseX > left + 200 || mouseY < listTop || mouseY > listBottom) {
      return false;
    }

    int index = (int) ((mouseY - listTop) / PROFILE_ROW_HEIGHT);
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
        profile -> profile.setName(ProfileNameValidator.sanitize(nameField.getText())));
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

  private void mutateSelectedProfile(Consumer<LootLockProfile> mutator) {
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
                  mutator.accept(draft.getDraft());
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
}
