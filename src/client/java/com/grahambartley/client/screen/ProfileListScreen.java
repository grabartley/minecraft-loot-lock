package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.network.ClientToServerPackets;
import com.grahambartley.network.PacketIds;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

public final class ProfileListScreen extends Screen {
  private final Screen parent;
  private TextFieldWidget nameField;
  private ButtonWidget deleteButton;
  private int selectedIndex;

  public ProfileListScreen(Screen parent) {
    super(Text.literal("Profiles"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int top = this.height / 5;

    nameField =
        new TextFieldWidget(this.textRenderer, left, top + 124, 200, 20, Text.literal("Name"));
    nameField.setMaxLength(ProfileNameValidator.MAX_UI_PROFILE_NAME_LENGTH);
    addDrawableChild(nameField);

    addDrawableChild(
        ButtonWidget.builder(Text.literal("Create"), button -> createProfile())
            .dimensions(left, top + 148, 64, 20)
            .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Rename"), button -> renameProfile())
            .dimensions(left + 68, top + 148, 64, 20)
            .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Duplicate"), button -> duplicateProfile())
            .dimensions(left + 136, top + 148, 64, 20)
            .build());

    deleteButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Delete"), button -> deleteProfile())
                .dimensions(left, top + 172, 97, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Activate"), button -> activateProfile())
            .dimensions(left + 103, top + 172, 97, 20)
            .build());

    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left, top + 196, 200, 20)
            .build());

    refreshDeleteButton();
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
      return;
    }

    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    int listTop = this.height / 5;
    for (int i = 0; i < profiles.size() && i < 5; i++) {
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
          listTop + i * 20,
          color);
    }

    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Click profile rows to select."),
        this.width / 2 - 100,
        listTop + 106,
        0xB0B0B0);
    context.drawTextWithShadow(
        textRenderer,
        Text.literal("Name (max 32 chars):"),
        this.width / 2 - 100,
        listTop + 114,
        0xB0B0B0);
    refreshDeleteButton();
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

    int listTop = this.height / 5;
    int left = this.width / 2 - 100;
    if (mouseX < left || mouseX > left + 200 || mouseY < listTop || mouseY > listTop + 100) {
      return false;
    }

    int index = (int) ((mouseY - listTop) / 20);
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (index >= 0 && index < profiles.size()) {
      selectedIndex = index;
      nameField.setText(ProfileNameValidator.sanitize(profiles.get(index).getName()));
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
    if (dataOptional.isEmpty() || !ClientPlayNetworking.canSend(PacketIds.CREATE_PROFILE_C2S)) {
      return;
    }
    String name = ProfileNameValidator.sanitize(nameField.getText());
    if (!ProfileNameValidator.isValid(name)) {
      return;
    }
    LootLockPlayerData data = dataOptional.get();
    ClientPlayNetworking.send(
        PacketIds.CREATE_PROFILE_C2S,
        ClientToServerPackets.writeCreateProfilePayload(data.getRevision(), name, null));
  }

  private void renameProfile() {
    mutateSelectedProfile(
        profile -> profile.setName(ProfileNameValidator.sanitize(nameField.getText())));
  }

  private void duplicateProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty() || !ClientPlayNetworking.canSend(PacketIds.CREATE_PROFILE_C2S)) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (selectedIndex < 0 || selectedIndex >= profiles.size()) {
      return;
    }
    LootLockProfile selected = profiles.get(selectedIndex);
    String duplicateName = ProfileUiController.nextDuplicateName(profiles, selected.getName());
    ClientPlayNetworking.send(
        PacketIds.CREATE_PROFILE_C2S,
        ClientToServerPackets.writeCreateProfilePayload(
            dataOptional.get().getRevision(), duplicateName, selected));
  }

  private void deleteProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty() || !ClientPlayNetworking.canSend(PacketIds.DELETE_PROFILE_C2S)) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (!ProfileUiController.canDelete(profiles)
        || selectedIndex < 0
        || selectedIndex >= profiles.size()) {
      return;
    }

    ClientPlayNetworking.send(
        PacketIds.DELETE_PROFILE_C2S,
        ClientToServerPackets.writeDeleteProfilePayload(
            dataOptional.get().getRevision(), profiles.get(selectedIndex).getId()));
  }

  private void activateProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty() || !ClientPlayNetworking.canSend(PacketIds.ACTIVATE_PROFILE_C2S)) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (selectedIndex < 0 || selectedIndex >= profiles.size()) {
      return;
    }
    ClientPlayNetworking.send(
        PacketIds.ACTIVATE_PROFILE_C2S,
        ClientToServerPackets.writeActivateProfilePayload(
            dataOptional.get().getRevision(), profiles.get(selectedIndex).getId()));
  }

  private void mutateSelectedProfile(java.util.function.Consumer<LootLockProfile> mutator) {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty() || !ClientPlayNetworking.canSend(PacketIds.UPDATE_PROFILE_C2S)) {
      return;
    }
    List<LootLockProfile> profiles = dataOptional.get().getProfiles();
    if (selectedIndex < 0 || selectedIndex >= profiles.size()) {
      return;
    }
    LootLockProfile profile = profiles.get(selectedIndex);
    LootLockProfile cloned =
        new LootLockProfile(
            profile.getId(),
            profile.getName(),
            profile.getMode(),
            profile.getRejectedItemAction(),
            profile.isEnabled(),
            profile.getRules());
    mutator.accept(cloned);
    if (!ProfileNameValidator.isValid(cloned.getName())) {
      return;
    }
    ClientPlayNetworking.send(
        PacketIds.UPDATE_PROFILE_C2S,
        ClientToServerPackets.writeUpdateProfilePayload(dataOptional.get().getRevision(), cloned));
  }

  private void refreshDeleteButton() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    deleteButton.active =
        dataOptional.map(data -> ProfileUiController.canDelete(data.getProfiles())).orElse(false);
  }
}
