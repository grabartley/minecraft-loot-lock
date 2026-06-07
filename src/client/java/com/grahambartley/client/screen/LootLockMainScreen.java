package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.state.ClientDraftProfile;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.network.ClientMutationSync;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Main LootLock entry screen.
 *
 * <p>Delete policy is shown here as read-only status text. Operators can still change it with the
 * policy command until that control is relocated into settings.
 */
public final class LootLockMainScreen extends Screen {
  private static final int GRID_BUTTON_WIDTH = 150;
  private static final int GRID_GAP = 4;
  private static final int DONE_BUTTON_WIDTH = 200;
  private static final int SUBTITLE_Y = 30;
  private static final int ROW_SPACING = 24;

  private final Screen parent;
  private ButtonWidget profilesButton;
  private ButtonWidget activeProfileButton;
  private ButtonWidget modeButton;
  private ButtonWidget actionButton;
  private ButtonWidget enabledButton;
  private ButtonWidget editRulesButton;
  private ButtonWidget settingsButton;
  private ButtonWidget importExportButton;

  public LootLockMainScreen(Screen parent) {
    super(Text.literal("LootLock"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - GRID_BUTTON_WIDTH - GRID_GAP / 2;
    int right = this.width / 2 + GRID_GAP / 2;
    int rowY = SUBTITLE_Y + 20;
    int doneX = this.width / 2 - DONE_BUTTON_WIDTH / 2;

    activeProfileButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Profile: -"), button -> cycleActiveProfile())
                .dimensions(left, rowY, GRID_BUTTON_WIDTH, 20)
                .build());

    enabledButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Enabled: -"), button -> toggleEnabled())
                .dimensions(right, rowY, GRID_BUTTON_WIDTH, 20)
                .build());

    modeButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Mode: -"), button -> toggleMode())
                .dimensions(left, rowY + ROW_SPACING, GRID_BUTTON_WIDTH, 20)
                .build());

    actionButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Action: -"), button -> toggleAction())
                .dimensions(right, rowY + ROW_SPACING, GRID_BUTTON_WIDTH, 20)
                .build());

    editRulesButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal("Edit Rules"),
                    button -> this.client.setScreen(new RuleListScreen(this)))
                .dimensions(left, rowY + ROW_SPACING * 2, GRID_BUTTON_WIDTH, 20)
                .build());
    profilesButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal("Profiles"),
                    button -> this.client.setScreen(new ProfileListScreen(this)))
                .dimensions(right, rowY + ROW_SPACING * 2, GRID_BUTTON_WIDTH, 20)
                .build());

    settingsButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal("Settings"),
                    button -> this.client.setScreen(new SettingsScreen(this)))
                .dimensions(left, rowY + ROW_SPACING * 3, GRID_BUTTON_WIDTH, 20)
                .build());
    importExportButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Import / Export"), button -> {})
                .dimensions(right, rowY + ROW_SPACING * 3, GRID_BUTTON_WIDTH, 20)
                .tooltip(Tooltip.of(Text.literal("Coming in a future release")))
                .build());

    addDrawableChild(
        ButtonWidget.builder(Text.literal("Done"), button -> close())
            .dimensions(doneX, this.height - 27, DONE_BUTTON_WIDTH, 20)
            .build());

    refreshButtons();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    ClientLootLockState state = LootLockClient.getState();
    int footerY = deletePolicyY();
    context.drawCenteredTextWithShadow(
        textRenderer,
        serverStateText(state.isServerSupportsLootLock()),
        this.width / 2,
        SUBTITLE_Y,
        0xFFFFFF);
    context.drawCenteredTextWithShadow(
        textRenderer,
        deletePolicyText(state.isAllowDeleteRejectedItems()),
        this.width / 2,
        footerY,
        0xA0A0A0);

    refreshButtons();
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.setScreen(parent);
    }
  }

  private void cycleActiveProfile() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData data = dataOptional.get();
    if (data.getProfiles().isEmpty()) {
      return;
    }

    Optional<java.util.UUID> nextProfileId =
        ProfileUiController.nextProfileId(data.getProfiles(), data.getActiveProfileId());
    if (nextProfileId.isEmpty()) {
      return;
    }
    ClientMutationSync.sendActivateRequest(data.getRevision(), nextProfileId.get());
  }

  private void toggleMode() {
    mutateActiveProfile(
        draft ->
            draft.setMode(
                draft.getDraft().getMode() == FilterMode.DENYLIST
                    ? FilterMode.ALLOWLIST
                    : FilterMode.DENYLIST));
  }

  private void toggleAction() {
    if (!LootLockClient.getState().isAllowDeleteRejectedItems()) {
      return;
    }
    if (shouldConfirmEnableDelete()) {
      this.client.setScreen(
          new ConfirmScreen(
              confirmed -> {
                if (this.client == null) {
                  return;
                }
                this.client.setScreen(this);
                if (confirmed) {
                  mutateActiveProfile(
                      draft -> draft.setRejectedItemAction(RejectedItemAction.DELETE));
                }
              },
              Text.literal(deleteConfirmTitle()),
              Text.literal(deleteConfirmMessage())));
      return;
    }
    mutateActiveProfile(
        draft ->
            draft.setRejectedItemAction(
                draft.getDraft().getRejectedItemAction() == RejectedItemAction.LEAVE_ON_GROUND
                    ? RejectedItemAction.DELETE
                    : RejectedItemAction.LEAVE_ON_GROUND));
  }

  private boolean shouldConfirmEnableDelete() {
    if (this.client == null) {
      return false;
    }
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    if (dataOptional.isEmpty()) {
      return false;
    }
    Optional<LootLockProfile> activeProfile = dataOptional.get().getActiveProfile();
    if (activeProfile.isEmpty()) {
      return false;
    }
    if (activeProfile.get().getRejectedItemAction() == RejectedItemAction.DELETE) {
      return false;
    }
    return LootLockClient.getClientSettingsManager()
        .getSettingsCopy()
        .isConfirmBeforeEnablingDelete();
  }

  private void toggleEnabled() {
    mutateActiveProfile(draft -> draft.setEnabled(!draft.getDraft().isEnabled()));
  }

  private void mutateActiveProfile(Consumer<ClientDraftProfile> mutator) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> dataOptional = state.getSnapshot();
    if (dataOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData data = dataOptional.get();
    Optional<ClientDraftSaveRequest> saveRequest =
        state
            .beginDraft(data.getActiveProfileId())
            .map(
                draft -> {
                  mutator.accept(draft);
                  return state.buildSaveRequest();
                })
            .orElse(Optional.empty());

    if (saveRequest.isEmpty()) {
      return;
    }
    ClientMutationSync.sendSaveRequest(saveRequest.get());
  }

  private void refreshButtons() {
    Optional<LootLockPlayerData> dataOptional = LootLockClient.getState().getSnapshot();
    boolean synced = LootLockClient.getState().isSynced();
    boolean editable =
        SupportStateViewModel.fromState(
                LootLockClient.getState().isServerSupportsLootLock(),
                synced,
                dataOptional.map(LootLockPlayerData::isClientCanEdit).orElse(false))
            .editable();
    boolean activeAvailable =
        dataOptional.flatMap(LootLockPlayerData::getActiveProfile).isPresent();

    activeProfileButton.active = synced && editable && activeAvailable;
    modeButton.active = synced && editable && activeAvailable;
    actionButton.active = synced && editable && activeAvailable;
    enabledButton.active = synced && editable && activeAvailable;

    editRulesButton.active = synced && editable && activeAvailable;
    profilesButton.active = true;
    settingsButton.active = true;
    // Import/export UI is still pending follow-up implementation.
    importExportButton.active = false;

    if (dataOptional.isEmpty()) {
      return;
    }

    LootLockPlayerData data = dataOptional.get();
    Optional<LootLockProfile> activeProfile = data.getActiveProfile();
    if (activeProfile.isEmpty()) {
      return;
    }

    LootLockProfile profile = activeProfile.get();
    activeProfileButton.setMessage(activeProfileButtonText(profile.getName()));
    modeButton.setMessage(Text.literal("Mode: " + friendlyMode(profile.getMode())));
    actionButton.setMessage(
        Text.literal("Action: " + friendlyAction(profile.getRejectedItemAction())));
    enabledButton.setMessage(Text.literal("Enabled: " + (profile.isEnabled() ? "On" : "Off")));
  }

  private static String friendlyMode(FilterMode mode) {
    return mode == null ? "Unknown" : titleCase(mode.name());
  }

  static String friendlyAction(RejectedItemAction action) {
    if (action == RejectedItemAction.LEAVE_ON_GROUND) {
      return "Leave";
    }
    if (action == RejectedItemAction.DELETE) {
      return "Delete";
    }
    return action == null ? "Unknown" : titleCase(action.name());
  }

  static Text serverStateText(boolean supported) {
    return Text.literal("Server: ")
        .append(
            Text.literal(supported ? "Supported" : "Unsupported")
                .formatted(supported ? Formatting.GREEN : Formatting.RED));
  }

  static Text deletePolicyText(boolean allowed) {
    MutableText value =
        Text.literal(allowed ? "Allowed" : "Blocked")
            .formatted(allowed ? Formatting.GREEN : Formatting.RED);
    return Text.literal("Delete policy: ").formatted(Formatting.GRAY).append(value);
  }

  static Text activeProfileButtonText(String profileName) {
    return Text.literal("Profile: ")
        .append(Text.literal("★ " + profileName).formatted(Formatting.YELLOW));
  }

  private static int deletePolicyY() {
    int rowY = SUBTITLE_Y + 20;
    return rowY + ROW_SPACING * 4 + 14;
  }

  static String deleteConfirmTitle() {
    return "Enable delete mode?";
  }

  static String deleteConfirmMessage() {
    return "Rejected dropped items are permanently deleted and cannot be recovered.";
  }

  private static String titleCase(String raw) {
    String normalized = raw.toLowerCase(Locale.ROOT).replace('_', ' ');
    String[] parts = normalized.split(" ");
    StringBuilder out = new StringBuilder();
    for (String part : parts) {
      if (part.isBlank()) {
        continue;
      }
      if (!out.isEmpty()) {
        out.append(' ');
      }
      out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
    }
    return out.toString();
  }
}
