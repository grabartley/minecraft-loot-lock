package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.screen.ProfileImportScreen;
import com.grahambartley.lootlock.client.screen.ProfileUiController;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

final class ProfileDropdown {
  static final int FRAME_PAD = 5;
  static final int MINI_BUTTON_SIZE = 18;
  static final int ACTIONS_WIDTH = 80;
  private static final int HEADER_STRIP_HEIGHT = 14;
  private static final int FOOTER_BUTTON_HEIGHT = 16;
  private static final int SHADOW_OFFSET = 3;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final BooleanSupplier panelOpen;
  private final Runnable onOpenChanged;

  private Screen host;
  private ClickableWidget toggleWidget;
  private TextFieldWidget renameField;
  private UUID renamingProfileId;
  private boolean open;

  private int anchorX;
  private int anchorY;
  private int anchorWidth;
  private String signature = "";
  private int frameX;
  private int frameY;
  private int frameW;
  private int frameH;

  ProfileDropdown(BooleanSupplier panelOpen, Runnable onOpenChanged) {
    this.panelOpen = panelOpen;
    this.onOpenChanged = onOpenChanged;
  }

  TextFieldWidget attach(Screen host, ClickableWidget toggleWidget) {
    this.host = host;
    this.toggleWidget = toggleWidget;
    widgets.clear();
    renameField =
        new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            0,
            0,
            16,
            12,
            Text.translatable(LootLockLang.PROFILE_RENAME_FIELD));
    renameField.setMaxLength(32);
    renameField.setDrawsBackground(true);
    renameField.visible = false;
    return renameField;
  }

  void setAnchor(int x, int y, int width) {
    anchorX = x;
    anchorY = y;
    anchorWidth = width;
    signature = "";
    rebuildIfStale();
  }

  boolean isOpen() {
    return open;
  }

  int frameBottom() {
    return frameY + frameH;
  }

  boolean isRenameField(ClickableWidget widget) {
    return widget != null && widget == renameField;
  }

  boolean isRenaming() {
    return renamingProfileId != null;
  }

  void open() {
    open = true;
    rebuildIfStale();
    applyVisibility();
  }

  void toggle() {
    if (open) {
      close();
      return;
    }
    open();
  }

  void close() {
    if (isInlineRenameActive()) {
      cancelInlineRename();
    }
    open = false;
    applyVisibility();
  }

  void closeSilently() {
    cancelInlineRename();
    open = false;
  }

  void applyVisibility() {
    boolean visible = panelOpen.getAsBoolean() && open;
    for (ClickableWidget widget : widgets) {
      widget.visible = visible;
    }
    onOpenChanged.run();
  }

  void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!open || widgets.isEmpty()) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    context.fill(
        frameX + SHADOW_OFFSET,
        frameY + SHADOW_OFFSET,
        frameX + frameW + SHADOW_OFFSET,
        frameY + frameH + SHADOW_OFFSET,
        0x80000000);
    Chrome.panel(context, frameX, frameY, frameW, frameH);
    int headerY = frameY + 4;
    context.drawText(
        client.textRenderer,
        Text.translatable(LootLockLang.DROPDOWN_SWITCH_PROFILE),
        frameX + 8,
        headerY,
        Palette.TITLE,
        false);
    context.fill(frameX + 4, headerY + 10, frameX + frameW - 4, headerY + 11, Palette.SLOT);
    for (ClickableWidget widget : widgets) {
      if (widget.visible) {
        widget.render(context, mouseX, mouseY, delta);
      }
    }
    if (renameField != null) {
      renameField.render(context, mouseX, mouseY, delta);
    }
    paintChipHoverTooltip(context, mouseX, mouseY);
  }

  private void paintChipHoverTooltip(DrawContext context, int mouseX, int mouseY) {
    if (isInlineRenameActive()) {
      return;
    }
    for (ClickableWidget widget : widgets) {
      if (widget instanceof ProfileDropdownRow row
          && row.visible
          && row.isMouseOverChip(mouseX, mouseY)) {
        context.drawTooltip(
            MinecraftClient.getInstance().textRenderer,
            List.of(Text.translatable(LootLockLang.DROPDOWN_CHANGE_COLOR)),
            mouseX,
            mouseY);
        return;
      }
    }
  }

  boolean handleMouseClick(double mouseX, double mouseY, int button) {
    if (!panelOpen.getAsBoolean() || !open) {
      return false;
    }
    if (!contains(frameX, frameY, frameW, frameH, mouseX, mouseY)) {
      if (isInlineRenameActive()) {
        commitInlineRename();
      }
      if (toggleWidget == null
          || !contains(
              toggleWidget.getX(),
              toggleWidget.getY(),
              toggleWidget.getWidth(),
              toggleWidget.getHeight(),
              mouseX,
              mouseY)) {
        close();
      }
      return false;
    }
    if (renameField != null
        && contains(
            renameField.getX(),
            renameField.getY(),
            renameField.getWidth(),
            renameField.getHeight(),
            mouseX,
            mouseY)) {
      renameField.mouseClicked(mouseX, mouseY, button);
      return true;
    }
    for (ClickableWidget widget : widgets) {
      if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
        return true;
      }
    }
    return true;
  }

  private static boolean contains(int x, int y, int w, int h, double mouseX, double mouseY) {
    return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
  }

  void rebuildIfStale() {
    if (isInlineRenameActive()) {
      return;
    }
    Optional<LootLockPlayerData> snapshotOptional = LootLockClient.getState().getSnapshot();
    List<LootLockProfile> profiles =
        snapshotOptional.map(LootLockPlayerData::getProfiles).orElse(List.of());
    UUID activeId = snapshotOptional.map(LootLockPlayerData::getActiveProfileId).orElse(null);
    String newSignature = signature(snapshotOptional.isPresent(), activeId, profiles);
    if (newSignature.equals(signature) && !widgets.isEmpty()) {
      return;
    }
    signature = newSignature;
    widgets.clear();

    boolean canCreate = ProfileUiController.canCreateProfile(profiles);
    Tooltip atCapacityTooltip =
        Tooltip.of(Text.translatable(LootLockLang.DROPDOWN_AT_CAPACITY, PacketLimits.MAX_PROFILES));

    int y = anchorY + HEADER_STRIP_HEIGHT;
    for (LootLockProfile profile : profiles) {
      addProfileRow(
          profile, profile.getId().equals(activeId), profiles, canCreate, atCapacityTooltip, y);
      y += ProfileDropdownRow.ROW_HEIGHT + 1;
    }

    ButtonWidget newProfileButton =
        footerButton(
            Text.translatable(LootLockLang.DROPDOWN_NEW_PROFILE), this::createProfile, y + 3);
    ButtonWidget importButton =
        footerButton(
            Text.translatable(LootLockLang.DROPDOWN_IMPORT_PROFILE),
            this::openImportModal,
            y + 3 + FOOTER_BUTTON_HEIGHT + 3);
    for (ButtonWidget footer : List.of(newProfileButton, importButton)) {
      footer.active = canCreate;
      if (!canCreate) {
        footer.setTooltip(atCapacityTooltip);
      }
      widgets.add(footer);
    }
    tintWhenActive(newProfileButton, Formatting.GREEN);
    tintWhenActive(importButton, Formatting.AQUA);

    frameX = anchorX - FRAME_PAD;
    frameY = anchorY - FRAME_PAD;
    frameW = anchorWidth + FRAME_PAD * 2;
    frameH = importButton.getY() + FOOTER_BUTTON_HEIGHT + FRAME_PAD - frameY;

    boolean visible = panelOpen.getAsBoolean() && open;
    for (ClickableWidget widget : widgets) {
      widget.visible = visible;
    }
  }

  private static String signature(
      boolean hasSnapshot, UUID activeId, List<LootLockProfile> profiles) {
    if (!hasSnapshot) {
      return "empty";
    }
    StringBuilder builder = new StringBuilder(activeId == null ? "-" : activeId.toString());
    for (LootLockProfile profile : profiles) {
      builder
          .append('|')
          .append(profile.getId())
          .append('=')
          .append(profile.getName())
          .append(':')
          .append(ProfileLabels.ruleCountLabel(profile))
          .append(':')
          .append(profile.getColor());
    }
    return builder.toString();
  }

  private void addProfileRow(
      LootLockProfile profile,
      boolean current,
      List<LootLockProfile> profiles,
      boolean canCreate,
      Tooltip atCapacityTooltip,
      int y) {
    int rowMainWidth = anchorWidth - ACTIONS_WIDTH - 4;
    widgets.add(
        new ProfileDropdownRow(
            anchorX,
            y,
            rowMainWidth,
            profile.getId(),
            ProfileColors.colorForProfile(profile),
            profile.getName(),
            ProfileLabels.ruleCountLabel(profile),
            current,
            () -> ActiveProfileActions.activate(profile.getId()),
            () -> ProfileColors.cycleProfileColor(profile.getId())));

    int actionsX = anchorX + rowMainWidth + 2;
    int gap = (ACTIONS_WIDTH - MINI_BUTTON_SIZE * 4) / 5;
    int buttonY = y + (ProfileDropdownRow.ROW_HEIGHT - MINI_BUTTON_SIZE) / 2;
    ButtonWidget rename =
        miniButton(
            Text.translatable(LootLockLang.BUTTON_MINI_RENAME),
            () -> startInlineRename(profile),
            actionsX + gap,
            buttonY);
    ButtonWidget duplicate =
        miniButton(
            Text.translatable(LootLockLang.BUTTON_MINI_DUPLICATE),
            () -> duplicateProfile(profile),
            actionsX + gap * 2 + MINI_BUTTON_SIZE,
            buttonY);
    duplicate.active = canCreate;
    if (!canCreate) {
      duplicate.setTooltip(atCapacityTooltip);
    }
    ButtonWidget export =
        miniButton(
            Text.translatable(LootLockLang.BUTTON_MINI_EXPORT),
            () -> exportProfile(profile),
            actionsX + gap * 3 + MINI_BUTTON_SIZE * 2,
            buttonY);
    export.setTooltip(Tooltip.of(Text.translatable(LootLockLang.BUTTON_MINI_EXPORT_TOOLTIP)));
    ButtonWidget delete =
        miniButton(
            Text.translatable(LootLockLang.BUTTON_MINI_DELETE),
            () -> deleteProfile(profile),
            actionsX + gap * 4 + MINI_BUTTON_SIZE * 3,
            buttonY);
    delete.active = ProfileUiController.canDelete(profiles);
    tintWhenActive(delete, Formatting.RED);
    widgets.addAll(List.of(rename, duplicate, export, delete));
  }

  static void tintWhenActive(ButtonWidget button, Formatting color) {
    if (button.active) {
      button.setMessage(button.getMessage().copy().formatted(color));
    }
  }

  private static ButtonWidget miniButton(Text glyph, Runnable action, int x, int y) {
    return ButtonWidget.builder(glyph, b -> action.run())
        .dimensions(x, y, MINI_BUTTON_SIZE, MINI_BUTTON_SIZE)
        .build();
  }

  private ButtonWidget footerButton(Text label, Runnable action, int y) {
    return ButtonWidget.builder(label, b -> action.run())
        .dimensions(anchorX, y, anchorWidth, FOOTER_BUTTON_HEIGHT)
        .build();
  }

  private void startInlineRename(LootLockProfile profile) {
    if (renameField == null) {
      return;
    }
    cancelInlineRename();
    ProfileDropdownRow row = findRow(profile.getId());
    if (row == null) {
      return;
    }
    renamingProfileId = profile.getId();
    int fieldX = row.nameRenderX() - 2;
    int fieldY = row.nameRenderY() - 2;
    renameField.setPosition(fieldX, fieldY);
    renameField.setWidth(row.getX() + row.getWidth() - fieldX - 2);
    renameField.setText(profile.getName());
    renameField.visible = true;
    renameField.setFocused(true);
    if (host != null) {
      host.setFocused(renameField);
    }
    row.setSuppressNameRender(true);
  }

  private void commitInlineRename() {
    if (renamingProfileId == null || renameField == null) {
      return;
    }
    String proposed = renameField.getText().trim();
    UUID target = renamingProfileId;
    cancelInlineRename();
    if (proposed.isEmpty()) {
      return;
    }
    ClientLootLockState state = LootLockClient.getState();
    state
        .beginDraft(target)
        .ifPresent(
            draft -> {
              if (proposed.equals(draft.getDraft().getName())) {
                return;
              }
              draft.setName(proposed);
              state.buildSaveRequest().ifPresent(ClientMutationSync::sendSaveRequest);
            });
  }

  void cancelInlineRename() {
    if (renamingProfileId == null) {
      return;
    }
    ProfileDropdownRow row = findRow(renamingProfileId);
    if (row != null) {
      row.setSuppressNameRender(false);
    }
    renamingProfileId = null;
    if (renameField != null) {
      renameField.setFocused(false);
      renameField.visible = false;
    }
    if (host != null) {
      host.setFocused(null);
    }
  }

  private ProfileDropdownRow findRow(UUID profileId) {
    if (profileId == null) {
      return null;
    }
    for (ClickableWidget widget : widgets) {
      if (widget instanceof ProfileDropdownRow row && profileId.equals(row.getProfileId())) {
        return row;
      }
    }
    return null;
  }

  boolean isInlineRenameActive() {
    return renamingProfileId != null && renameField != null && renameField.visible;
  }

  boolean handleInlineRenameKey(int keyCode, int scanCode, int modifiers) {
    if (!isInlineRenameActive()) {
      return false;
    }
    if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER) {
      commitInlineRename();
      return true;
    }
    if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
      cancelInlineRename();
      return true;
    }
    return renameField.keyPressed(keyCode, scanCode, modifiers);
  }

  boolean handleInlineRenameChar(char chr, int modifiers) {
    if (!isInlineRenameActive()) {
      return false;
    }
    return renameField.charTyped(chr, modifiers);
  }

  private void exportProfile(LootLockProfile profile) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.keyboard == null) {
      return;
    }
    ProfileShareController.export(
        profile,
        client.keyboard::setClipboard,
        (title, subtitle) -> LootLockToast.show(client, title, subtitle));
  }

  private void openImportModal() {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null) {
      return;
    }
    Screen current = client.currentScreen;
    close();
    client.setScreen(new ProfileImportScreen(current));
  }

  private void duplicateProfile(LootLockProfile profile) {
    LootLockClient.getState()
        .getSnapshot()
        .ifPresent(
            snapshot -> {
              String duplicateName =
                  ProfileUiController.nextDuplicateName(snapshot.getProfiles(), profile.getName());
              ClientMutationSync.sendCreateRequest(snapshot.getRevision(), duplicateName, profile);
              close();
            });
  }

  private void deleteProfile(LootLockProfile profile) {
    LootLockClient.getState()
        .getSnapshot()
        .filter(snapshot -> ProfileUiController.canDelete(snapshot.getProfiles()))
        .ifPresent(
            snapshot -> {
              ClientMutationSync.sendDeleteRequest(snapshot.getRevision(), profile.getId());
              close();
            });
  }

  private void createProfile() {
    LootLockClient.getState()
        .getSnapshot()
        .ifPresent(
            snapshot ->
                ClientMutationSync.sendCreateRequest(
                    snapshot.getRevision(),
                    ProfileUiController.nextDuplicateName(snapshot.getProfiles(), "New Profile"),
                    null));
  }
}
