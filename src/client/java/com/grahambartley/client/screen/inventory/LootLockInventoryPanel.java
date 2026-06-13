package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.screen.ProfileUiController;
import com.grahambartley.client.state.ClientDraftProfile;
import com.grahambartley.client.state.ClientLootLockState;
import com.grahambartley.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.network.ClientMutationSync;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Docked Loot Lock panel rendered alongside the survival inventory. Composes a header (title +
 * Client interactive switch + Server read-only switch), a profile bar with cycle arrows and a
 * dropdown manager, master Mode and Action segmented controls, a plain-English live summary, and a
 * lockable region that disables every control below the header when the global Client toggle is
 * off.
 *
 * <p>All widgets persist as drawable children of the host {@link InventoryScreen} and use their
 * {@code visible} flag to fade in and out, which keeps input routing inside the vanilla widget
 * dispatch.
 */
public final class LootLockInventoryPanel {
  public static final int WIDTH = 220;
  public static final int HEIGHT = 320;
  private static final int SIDE_PADDING = 8;
  private static final int ROW_HEIGHT = 20;
  private static final int ROW_GAP = 4;

  private final List<ClickableWidget> allWidgets = new ArrayList<>();
  private final List<ClickableWidget> lockableWidgets = new ArrayList<>();
  private final List<ClickableWidget> dropdownWidgets = new ArrayList<>();

  private final RulesTabView rulesView = new RulesTabView();
  private final SettingsTabView settingsView = new SettingsTabView();
  private PanelTab activeTab = PanelTab.RULES;

  private ButtonWidget rulesTabButton;
  private ButtonWidget settingsTabButton;

  private InventoryScreen host;
  private int panelX;
  private int panelY;
  private boolean open;
  private boolean dropdownOpen;

  private ButtonWidget clientToggle;
  private ButtonWidget serverToggle;
  private ButtonWidget prevProfileButton;
  private ButtonWidget nextProfileButton;
  private ButtonWidget profilePillButton;
  private ButtonWidget modeAllowButton;
  private ButtonWidget modeDenyButton;
  private ButtonWidget actionLeaveButton;
  private ButtonWidget actionDeleteButton;

  private ButtonWidget newProfileButton;

  public boolean isOpen() {
    return open;
  }

  public void setOpen(boolean open) {
    this.open = open;
    if (!open) {
      dropdownOpen = false;
    }
    applyVisibility();
  }

  public void toggleOpen() {
    setOpen(!open);
  }

  /** Attaches the panel widgets to the host inventory screen at the given anchor position. */
  public void attach(
      InventoryScreen host, int panelX, int panelY, Consumer<ClickableWidget> addDrawableChild) {
    this.host = host;
    this.panelX = panelX;
    this.panelY = panelY;
    allWidgets.clear();
    lockableWidgets.clear();
    dropdownWidgets.clear();

    int cursorY = panelY + SIDE_PADDING;

    // Header: title row uses static text drawn in render(); add Client + Server toggle buttons.
    int toggleWidth = 70;
    int toggleX = panelX + WIDTH - SIDE_PADDING - toggleWidth;
    clientToggle =
        addButton(
            addDrawableChild,
            toggleX,
            cursorY,
            toggleWidth,
            ROW_HEIGHT,
            Text.literal("Client: ..."),
            button -> handleClientToggle());
    cursorY += ROW_HEIGHT + ROW_GAP;
    serverToggle =
        addButton(
            addDrawableChild,
            toggleX,
            cursorY,
            toggleWidth,
            ROW_HEIGHT,
            Text.literal("Server: ..."),
            button -> {});
    serverToggle.active = false;
    cursorY += ROW_HEIGHT + ROW_GAP;

    // Profile bar: previous, profile pill (opens dropdown), next.
    int navWidth = 20;
    int pillWidth = WIDTH - SIDE_PADDING * 2 - navWidth * 2 - 4;
    int prevX = panelX + SIDE_PADDING;
    int pillX = prevX + navWidth + 2;
    int nextX = pillX + pillWidth + 2;
    prevProfileButton =
        addButton(
            addDrawableChild,
            prevX,
            cursorY,
            navWidth,
            ROW_HEIGHT,
            Text.literal("<"),
            button -> cycleActiveProfile(-1));
    profilePillButton =
        addButton(
            addDrawableChild,
            pillX,
            cursorY,
            pillWidth,
            ROW_HEIGHT,
            Text.literal("Profile..."),
            button -> toggleDropdown());
    nextProfileButton =
        addButton(
            addDrawableChild,
            nextX,
            cursorY,
            navWidth,
            ROW_HEIGHT,
            Text.literal(">"),
            button -> cycleActiveProfile(1));
    lockableWidgets.add(prevProfileButton);
    lockableWidgets.add(profilePillButton);
    lockableWidgets.add(nextProfileButton);
    cursorY += ROW_HEIGHT + ROW_GAP;

    // Master controls: Mode (Allow / Deny) and Action (Leave / Delete) segmented buttons.
    int segWidth = (WIDTH - SIDE_PADDING * 2) / 2;
    modeAllowButton =
        addButton(
            addDrawableChild,
            panelX + SIDE_PADDING,
            cursorY,
            segWidth,
            ROW_HEIGHT,
            Text.literal("Allow"),
            button -> setMode(FilterMode.ALLOWLIST));
    modeDenyButton =
        addButton(
            addDrawableChild,
            panelX + SIDE_PADDING + segWidth,
            cursorY,
            segWidth,
            ROW_HEIGHT,
            Text.literal("Deny"),
            button -> setMode(FilterMode.DENYLIST));
    lockableWidgets.add(modeAllowButton);
    lockableWidgets.add(modeDenyButton);
    cursorY += ROW_HEIGHT + ROW_GAP;

    actionLeaveButton =
        addButton(
            addDrawableChild,
            panelX + SIDE_PADDING,
            cursorY,
            segWidth,
            ROW_HEIGHT,
            Text.literal("Leave"),
            button -> setAction(RejectedItemAction.LEAVE_ON_GROUND));
    actionDeleteButton =
        addButton(
            addDrawableChild,
            panelX + SIDE_PADDING + segWidth,
            cursorY,
            segWidth,
            ROW_HEIGHT,
            Text.literal("Delete"),
            button -> setAction(RejectedItemAction.DELETE));
    lockableWidgets.add(actionLeaveButton);
    lockableWidgets.add(actionDeleteButton);
    cursorY += ROW_HEIGHT + ROW_GAP;

    // Tab strip below the master controls.
    int tabsY = cursorY;
    int tabWidth = (WIDTH - SIDE_PADDING * 2) / 2;
    rulesTabButton =
        addButton(
            addDrawableChild,
            panelX + SIDE_PADDING,
            tabsY,
            tabWidth,
            ROW_HEIGHT,
            Text.literal("Rules"),
            button -> setTab(PanelTab.RULES));
    settingsTabButton =
        addButton(
            addDrawableChild,
            panelX + SIDE_PADDING + tabWidth,
            tabsY,
            tabWidth,
            ROW_HEIGHT,
            Text.literal("Settings"),
            button -> setTab(PanelTab.SETTINGS));
    lockableWidgets.add(rulesTabButton);
    lockableWidgets.add(settingsTabButton);
    cursorY += ROW_HEIGHT + ROW_GAP;

    // Tab view area: Rules and Settings widget banks both attach here. visibility toggles based on
    // the active tab.
    int viewWidth = WIDTH - SIDE_PADDING * 2;
    rulesView.attach(
        panelX + SIDE_PADDING,
        cursorY,
        viewWidth,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });
    settingsView.attach(
        panelX + SIDE_PADDING,
        cursorY,
        viewWidth,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });

    // Dropdown widgets: created up front so we can show / hide them in place.
    rebuildDropdownWidgets(
        addDrawableChild, pillX, panelY + SIDE_PADDING + (ROW_HEIGHT + ROW_GAP) * 5, pillWidth);

    applyVisibility();
    refresh();
  }

  /**
   * Renders the panel chrome (background, title, summary text). Panel widgets render themselves
   * through the host screen's normal widget pipeline.
   */
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!open) {
      return;
    }
    // Background frame: dark outer border, lighter face.
    context.fill(panelX - 1, panelY - 1, panelX + WIDTH + 1, panelY + HEIGHT + 1, 0xFF1B1B1B);
    context.fill(panelX, panelY, panelX + WIDTH, panelY + HEIGHT, 0xFFC6C6C6);

    // Title.
    MinecraftClient client = MinecraftClient.getInstance();
    int titleY = panelY + SIDE_PADDING + 6;
    context.drawText(
        client.textRenderer,
        Text.literal("Loot Lock").formatted(Formatting.DARK_GRAY),
        panelX + SIDE_PADDING,
        titleY,
        0x3B3B3B,
        false);

    // Summary line: drawn above the tab strip.
    Optional<LootLockProfile> activeProfile = activeProfile();
    boolean globallyEnabled =
        LootLockClient.getState()
            .getSnapshot()
            .map(LootLockPlayerData::isGloballyEnabled)
            .orElse(true);
    int summaryTop = panelY + SIDE_PADDING + (ROW_HEIGHT + ROW_GAP) * 4 + 4;
    context.drawTextWrapped(
        client.textRenderer,
        LootLockSummaryText.build(globallyEnabled, activeProfile.orElse(null)),
        panelX + SIDE_PADDING,
        summaryTop,
        WIDTH - SIDE_PADDING * 2,
        0x3B3B3B);

    // Per-view rendering for whatever the active tab paints itself.
    if (activeTab == PanelTab.RULES) {
      rulesView.render(context, mouseX, mouseY, delta);
    } else {
      settingsView.render(context, mouseX, mouseY, delta);
    }
  }

  public void setTab(PanelTab tab) {
    if (tab == null || tab == activeTab) {
      return;
    }
    activeTab = tab;
    applyVisibility();
    refresh();
  }

  /** Refresh button labels and lock state to match the current snapshot. */
  public void refresh() {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    boolean serverSupported = state.isServerSupportsLootLock();
    boolean globallyEnabled =
        snapshotOptional.map(LootLockPlayerData::isGloballyEnabled).orElse(true);

    if (clientToggle != null) {
      clientToggle.setMessage(
          Text.literal("Client: " + (globallyEnabled ? "ON" : "OFF"))
              .formatted(globallyEnabled ? Formatting.GREEN : Formatting.GRAY));
    }
    if (serverToggle != null) {
      serverToggle.setMessage(
          Text.literal("Server: " + (serverSupported ? "ON" : "OFF"))
              .formatted(serverSupported ? Formatting.GREEN : Formatting.RED));
    }

    Optional<LootLockProfile> activeProfile = activeProfile();
    if (profilePillButton != null) {
      profilePillButton.setMessage(
          Text.literal(activeProfile.map(LootLockProfile::getName).orElse("Profile..."))
              .formatted(Formatting.YELLOW));
    }

    LootLockProfile profile = activeProfile.orElse(null);
    if (modeAllowButton != null && modeDenyButton != null) {
      boolean isAllow = profile != null && profile.getMode() == FilterMode.ALLOWLIST;
      modeAllowButton.setMessage(
          Text.literal("Allow").formatted(isAllow ? Formatting.GREEN : Formatting.GRAY));
      modeDenyButton.setMessage(
          Text.literal("Deny").formatted(!isAllow ? Formatting.RED : Formatting.GRAY));
    }
    boolean canDelete = state.isAllowDeleteRejectedItems();
    if (actionLeaveButton != null && actionDeleteButton != null) {
      boolean isDelete =
          profile != null && profile.getRejectedItemAction() == RejectedItemAction.DELETE;
      actionLeaveButton.setMessage(
          Text.literal("Leave").formatted(!isDelete ? Formatting.WHITE : Formatting.GRAY));
      actionDeleteButton.setMessage(
          Text.literal("Delete").formatted(isDelete ? Formatting.RED : Formatting.GRAY));
      actionDeleteButton.active = open && globallyEnabled && canDelete;
    }

    applyLock(globallyEnabled);

    if (rulesTabButton != null && settingsTabButton != null) {
      rulesTabButton.setMessage(
          Text.literal("Rules")
              .formatted(activeTab == PanelTab.RULES ? Formatting.YELLOW : Formatting.GRAY));
      settingsTabButton.setMessage(
          Text.literal("Settings")
              .formatted(activeTab == PanelTab.SETTINGS ? Formatting.YELLOW : Formatting.GRAY));
    }

    rulesView.refresh();
  }

  public PanelTab getActiveTab() {
    return activeTab;
  }

  /** Returns true and sets the toggle when the snapshot is synced and editable. */
  public void handleClientToggle() {
    GlobalEnableController.toggle(MinecraftClient.getInstance());
  }

  // Internal helpers ---------------------------------------------------------

  private void applyVisibility() {
    for (ClickableWidget widget : allWidgets) {
      widget.visible = open;
    }
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = open && dropdownOpen;
    }
    // Tab views override their widgets' visibility based on the active tab.
    rulesView.setVisible(open && activeTab == PanelTab.RULES);
    settingsView.setVisible(open && activeTab == PanelTab.SETTINGS);
  }

  private void applyLock(boolean enabled) {
    for (ClickableWidget widget : lockableWidgets) {
      widget.active = open && enabled;
    }
    // The delete button has additional gating handled in refresh().
  }

  private ButtonWidget addButton(
      Consumer<ClickableWidget> addDrawableChild,
      int x,
      int y,
      int width,
      int height,
      Text label,
      ButtonWidget.PressAction onPress) {
    ButtonWidget button =
        ButtonWidget.builder(label, onPress).dimensions(x, y, width, height).build();
    addDrawableChild.accept(button);
    allWidgets.add(button);
    return button;
  }

  private void rebuildDropdownWidgets(
      Consumer<ClickableWidget> addDrawableChild, int x, int startY, int width) {
    int y = startY;
    Optional<LootLockPlayerData> snapshotOptional = LootLockClient.getState().getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    List<LootLockProfile> profiles = snapshotOptional.get().getProfiles();
    for (LootLockProfile profile : profiles) {
      // Each row: profile name (switch) + rename + duplicate + delete (mini-buttons).
      int rowY = y;
      int miniWidth = 20;
      int rowNameWidth = width - miniWidth * 3 - 6;
      ButtonWidget switchButton =
          ButtonWidget.builder(
                  Text.literal(profile.getName()).formatted(Formatting.YELLOW),
                  b -> activateProfile(profile.getId()))
              .dimensions(x, rowY, rowNameWidth, ROW_HEIGHT)
              .build();
      ButtonWidget renameButton =
          ButtonWidget.builder(Text.literal("R"), b -> renameProfile(profile))
              .dimensions(x + rowNameWidth + 2, rowY, miniWidth, ROW_HEIGHT)
              .build();
      ButtonWidget duplicateButton =
          ButtonWidget.builder(Text.literal("D"), b -> duplicateProfile(profile))
              .dimensions(x + rowNameWidth + 2 + miniWidth + 2, rowY, miniWidth, ROW_HEIGHT)
              .build();
      ButtonWidget deleteButton =
          ButtonWidget.builder(Text.literal("X"), b -> deleteProfile(profile))
              .dimensions(x + rowNameWidth + 2 + miniWidth * 2 + 4, rowY, miniWidth, ROW_HEIGHT)
              .build();
      deleteButton.active = ProfileUiController.canDelete(profiles);

      addDrawableChild.accept(switchButton);
      addDrawableChild.accept(renameButton);
      addDrawableChild.accept(duplicateButton);
      addDrawableChild.accept(deleteButton);
      allWidgets.add(switchButton);
      allWidgets.add(renameButton);
      allWidgets.add(duplicateButton);
      allWidgets.add(deleteButton);
      dropdownWidgets.add(switchButton);
      dropdownWidgets.add(renameButton);
      dropdownWidgets.add(duplicateButton);
      dropdownWidgets.add(deleteButton);
      y += ROW_HEIGHT + 2;
    }
    newProfileButton =
        ButtonWidget.builder(Text.literal("+ New profile"), b -> createProfile())
            .dimensions(x, y, width, ROW_HEIGHT)
            .build();
    addDrawableChild.accept(newProfileButton);
    allWidgets.add(newProfileButton);
    dropdownWidgets.add(newProfileButton);
  }

  private void toggleDropdown() {
    dropdownOpen = !dropdownOpen;
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = open && dropdownOpen;
    }
  }

  private Optional<LootLockProfile> activeProfile() {
    return LootLockClient.getState().getSnapshot().flatMap(LootLockPlayerData::getActiveProfile);
  }

  private void cycleActiveProfile(int direction) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    List<LootLockProfile> profiles = snapshot.getProfiles();
    if (profiles.isEmpty()) {
      return;
    }
    int currentIndex = -1;
    for (int i = 0; i < profiles.size(); i++) {
      if (profiles.get(i).getId().equals(snapshot.getActiveProfileId())) {
        currentIndex = i;
        break;
      }
    }
    if (currentIndex < 0) {
      currentIndex = 0;
    }
    int nextIndex = (currentIndex + direction + profiles.size()) % profiles.size();
    activateProfile(profiles.get(nextIndex).getId());
  }

  private void activateProfile(UUID profileId) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    ClientMutationSync.sendActivateRequest(snapshotOptional.get().getRevision(), profileId);
  }

  private void setMode(FilterMode mode) {
    mutateActive(draft -> draft.setMode(mode));
  }

  private void setAction(RejectedItemAction action) {
    if (action == RejectedItemAction.DELETE
        && !LootLockClient.getState().isAllowDeleteRejectedItems()) {
      return;
    }
    mutateActive(draft -> draft.setRejectedItemAction(action));
  }

  private void mutateActive(Consumer<ClientDraftProfile> mutator) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData data = snapshotOptional.get();
    state
        .beginDraft(data.getActiveProfileId())
        .ifPresent(
            draft -> {
              mutator.accept(draft);
              Optional<ClientDraftSaveRequest> saveRequest = state.buildSaveRequest();
              saveRequest.ifPresent(ClientMutationSync::sendSaveRequest);
            });
  }

  private void renameProfile(LootLockProfile profile) {
    // Vanilla doesn't ship an inline rename popup; the rename action is left to the host screen
    // to handle (story 4 surface) so we just no-op gracefully when called outside that context.
    closeDropdown();
  }

  private void duplicateProfile(LootLockProfile profile) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    String duplicateName =
        ProfileUiController.nextDuplicateName(snapshot.getProfiles(), profile.getName());
    ClientMutationSync.sendCreateRequest(snapshot.getRevision(), duplicateName, profile);
    closeDropdown();
  }

  private void deleteProfile(LootLockProfile profile) {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    if (!ProfileUiController.canDelete(snapshot.getProfiles())) {
      return;
    }
    ClientMutationSync.sendDeleteRequest(snapshot.getRevision(), profile.getId());
    closeDropdown();
  }

  private void createProfile() {
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockPlayerData snapshot = snapshotOptional.get();
    String name = ProfileUiController.nextDuplicateName(snapshot.getProfiles(), "New Profile");
    ClientMutationSync.sendCreateRequest(snapshot.getRevision(), name, null);
    closeDropdown();
  }

  private void closeDropdown() {
    dropdownOpen = false;
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = false;
    }
  }

  /** Test-visible accessor for the widgets the dropdown manages. */
  public List<ClickableWidget> dropdownWidgetsForTest() {
    return List.copyOf(dropdownWidgets);
  }

  /** Test-visible accessor for the widgets the lockable region manages. */
  public List<ClickableWidget> lockableWidgetsForTest() {
    return List.copyOf(lockableWidgets);
  }
}
