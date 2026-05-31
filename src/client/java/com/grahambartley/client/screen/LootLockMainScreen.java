package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class LootLockMainScreen extends Screen {
  private final Screen parent;
  private ButtonWidget activeProfileButton;
  private ButtonWidget modeButton;
  private ButtonWidget actionButton;
  private ButtonWidget enabledButton;
  private ButtonWidget editRulesButton;
  private ButtonWidget settingsButton;
  private ButtonWidget importExportButton;
  private ButtonWidget policyDeleteToggleButton;

  public LootLockMainScreen(Screen parent) {
    super(Text.literal("LootLock"));
    this.parent = parent;
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int totalHeight = 200;
    int rowY = Math.max(34, (this.height - totalHeight) / 2);

    activeProfileButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Active: -"), button -> cycleActiveProfile())
                .dimensions(left, rowY, 200, 20)
                .build());

    modeButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Mode: -"), button -> toggleMode())
                .dimensions(left, rowY + 24, 200, 20)
                .build());

    actionButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Action: -"), button -> toggleAction())
                .dimensions(left, rowY + 48, 200, 20)
                .build());

    enabledButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Enabled: -"), button -> toggleEnabled())
                .dimensions(left, rowY + 72, 200, 20)
                .build());

    editRulesButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal("Edit Rules"),
                    button -> this.client.setScreen(new RuleListScreen(this)))
                .dimensions(left, rowY + 104, 97, 20)
                .build());
    addDrawableChild(
        ButtonWidget.builder(
                Text.literal("Profiles"),
                button -> this.client.setScreen(new ProfileListScreen(this)))
            .dimensions(left + 103, rowY + 104, 97, 20)
            .build());

    settingsButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal("Settings"),
                    button -> this.client.setScreen(new SettingsScreen(this)))
                .dimensions(left, rowY + 128, 97, 20)
                .build());
    importExportButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Import / Export (soon)"), button -> {})
                .dimensions(left + 103, rowY + 128, 97, 20)
                .build());

    policyDeleteToggleButton =
        addDrawableChild(
            ButtonWidget.builder(Text.literal("Policy: -"), button -> toggleServerPolicyDelete())
                .dimensions(left, rowY + 152, 200, 20)
                .build());

    addDrawableChild(
        ButtonWidget.builder(Text.literal("Done"), button -> close())
            .dimensions(left, rowY + 180, 200, 20)
            .build());

    refreshButtons();
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);

    ClientLootLockState state = LootLockClient.getState();
    boolean canEdit = state.getSnapshot().map(LootLockPlayerData::isClientCanEdit).orElse(false);
    SupportStateViewModel supportState =
        SupportStateViewModel.fromState(
            state.isServerSupportsLootLock(), state.isSynced(), canEdit);
    String serverState = state.isServerSupportsLootLock() ? "Supported" : "Unsupported";
    context.drawTextWithShadow(
        textRenderer, Text.literal("Server: " + serverState), this.width / 2 - 100, 40, 0xC0C0C0);
    context.drawTextWithShadow(
        textRenderer,
        Text.literal(supportState.message()),
        this.width / 2 - 100,
        52,
        supportState.color());

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
        profile ->
            profile.setMode(
                profile.getMode() == FilterMode.DENYLIST
                    ? FilterMode.ALLOWLIST
                    : FilterMode.DENYLIST));
  }

  private void toggleAction() {
    if (!LootLockClient.getState().isAllowDeleteRejectedItems()) {
      return;
    }
    mutateActiveProfile(
        profile ->
            profile.setRejectedItemAction(
                profile.getRejectedItemAction() == RejectedItemAction.LEAVE_ON_GROUND
                    ? RejectedItemAction.DELETE
                    : RejectedItemAction.LEAVE_ON_GROUND));
  }

  private void toggleServerPolicyDelete() {
    ClientMutationSync.sendServerPolicyUpdateRequest(
        !LootLockClient.getState().isAllowDeleteRejectedItems());
  }

  private void toggleEnabled() {
    mutateActiveProfile(profile -> profile.setEnabled(!profile.isEnabled()));
  }

  private void mutateActiveProfile(Consumer<LootLockProfile> mutator) {
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
                  mutator.accept(draft.getDraft());
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
    policyDeleteToggleButton.active = synced && editable;
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
    activeProfileButton.setMessage(Text.literal("Active: " + profile.getName()));
    modeButton.setMessage(Text.literal("Mode: " + friendlyMode(profile.getMode())));
    actionButton.setMessage(
        Text.literal("Action: " + friendlyAction(profile.getRejectedItemAction())));
    enabledButton.setMessage(Text.literal("Enabled: " + (profile.isEnabled() ? "On" : "Off")));
    policyDeleteToggleButton.setMessage(
        Text.literal(
            "Policy Delete: "
                + (LootLockClient.getState().isAllowDeleteRejectedItems()
                    ? "Allowed"
                    : "Blocked")));
  }

  private static String friendlyMode(FilterMode mode) {
    return mode == null ? "Unknown" : titleCase(mode.name());
  }

  private static String friendlyAction(RejectedItemAction action) {
    if (action == RejectedItemAction.LEAVE_ON_GROUND) {
      return "Leave on ground";
    }
    return action == null ? "Unknown" : titleCase(action.name());
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
