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
import net.minecraft.util.Identifier;

/**
 * Docked Loot Lock panel rendered alongside the survival inventory, styled pixel-faithful to the
 * vanilla Minecraft GUI prototype in {@code ux_redesign_2/Loot Lock.html}. Composes the brand
 * header (icon + title + Client interactive switch + Server read-only switch), a profile bar with
 * cycle arrows and a dropdown manager, master Mode and Action segmented controls, a plain-English
 * live summary, a tab strip, and a content well for the active tab. The whole region below the
 * header dims and stops accepting input when the global Client toggle is off.
 */
public final class LootLockInventoryPanel {
  public static final int WIDTH = 240;
  public static final int HEIGHT = 280;
  private static final int SIDE_PADDING = 6;
  private static final int HEADER_HEIGHT = 24;
  private static final int PROFILE_ROW_HEIGHT = 20;
  private static final int CONTROL_ROW_HEIGHT = 18;
  private static final int SUMMARY_HEIGHT = 36;
  private static final int TAB_HEIGHT = 20;
  private static final int CONTENT_PADDING = 4;
  private static final int SWITCH_WIDTH = 42;
  private static final int SWITCH_HEIGHT = 16;
  private static final int CTL_LABEL_WIDTH = 48;
  private static final Identifier ICON_TEXTURE = LootLockIconButton.ICON_TEXTURE;

  private final List<ClickableWidget> allWidgets = new ArrayList<>();
  private final List<ClickableWidget> lockableWidgets = new ArrayList<>();
  private final List<ClickableWidget> dropdownWidgets = new ArrayList<>();

  private final RulesTabView rulesView = new RulesTabView();
  private final SettingsTabView settingsView = new SettingsTabView();
  private PanelTab activeTab = PanelTab.RULES;

  private InventoryScreen host;
  private int panelX;
  private int panelY;
  private boolean open;
  private boolean dropdownOpen;

  private VanillaTab rulesTabButton;
  private VanillaTab settingsTabButton;
  private VanillaSwitch clientSwitch;
  private VanillaSwitch serverSwitch;
  private NavArrowButton prevProfileButton;
  private NavArrowButton nextProfileButton;
  private ProfilePill profilePill;
  private SegmentedButton modeAllowButton;
  private SegmentedButton modeDenyButton;
  private SegmentedButton actionLeaveButton;
  private SegmentedButton actionDeleteButton;
  private ButtonWidget newProfileButton;

  // Position references for paint code that draws labels.
  private int headerY;
  private int profileY;
  private int modeY;
  private int actionY;
  private int summaryY;
  private int tabsY;
  private int contentY;
  private int contentHeight;

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

  public void attach(
      InventoryScreen host, int panelX, int panelY, Consumer<ClickableWidget> addDrawableChild) {
    this.host = host;
    this.panelX = panelX;
    this.panelY = panelY;
    allWidgets.clear();
    lockableWidgets.clear();
    dropdownWidgets.clear();

    int innerLeft = panelX + SIDE_PADDING;
    int innerRight = panelX + WIDTH - SIDE_PADDING;
    int innerWidth = innerRight - innerLeft;
    int cursorY = panelY + SIDE_PADDING;

    // Header: icon + title + Client + Server switches in one row.
    headerY = cursorY;
    int switchY = cursorY + (HEADER_HEIGHT - SWITCH_HEIGHT) / 2;
    int serverSwitchX = innerRight - SWITCH_WIDTH;
    int serverLabelX = serverSwitchX - 32;
    int clientSwitchX = serverLabelX - SWITCH_WIDTH - 4;
    int clientLabelX = clientSwitchX - 32;
    serverSwitch =
        new VanillaSwitch(
            serverSwitchX,
            switchY,
            SWITCH_WIDTH,
            SWITCH_HEIGHT,
            () -> LootLockClient.getState().isServerSupportsLootLock(),
            null,
            true,
            true);
    addDrawableChild.accept(serverSwitch);
    allWidgets.add(serverSwitch);
    clientSwitch =
        new VanillaSwitch(
            clientSwitchX,
            switchY,
            SWITCH_WIDTH,
            SWITCH_HEIGHT,
            () -> currentGloballyEnabled(),
            this::onClientSwitchPressed,
            false,
            false);
    addDrawableChild.accept(clientSwitch);
    allWidgets.add(clientSwitch);
    cursorY += HEADER_HEIGHT + 2;

    // Profile bar: prev | pill | next.
    profileY = cursorY;
    int navWidth = 14;
    int pillX = innerLeft + navWidth + 3;
    int pillWidth = innerWidth - navWidth * 2 - 6;
    int nextX = pillX + pillWidth + 3;
    prevProfileButton =
        new NavArrowButton(
            innerLeft, cursorY, navWidth, PROFILE_ROW_HEIGHT, false, () -> cycleActiveProfile(-1));
    addDrawableChild.accept(prevProfileButton);
    allWidgets.add(prevProfileButton);
    lockableWidgets.add(prevProfileButton);
    profilePill =
        new ProfilePill(
            pillX,
            cursorY,
            pillWidth,
            PROFILE_ROW_HEIGHT,
            () -> activeProfile().map(LootLockInventoryPanel::colorForProfile).orElse(Palette.SLOT),
            () -> activeProfile().map(LootLockProfile::getName).orElse("--"),
            () -> activeProfile().map(LootLockInventoryPanel::ruleCountLabel).orElse(""),
            this::toggleDropdown);
    addDrawableChild.accept(profilePill);
    allWidgets.add(profilePill);
    lockableWidgets.add(profilePill);
    nextProfileButton =
        new NavArrowButton(
            nextX, cursorY, navWidth, PROFILE_ROW_HEIGHT, true, () -> cycleActiveProfile(1));
    addDrawableChild.accept(nextProfileButton);
    allWidgets.add(nextProfileButton);
    lockableWidgets.add(nextProfileButton);
    cursorY += PROFILE_ROW_HEIGHT + 3;

    // Mode row.
    modeY = cursorY;
    int segLeft = innerLeft + CTL_LABEL_WIDTH;
    int segWidth = (innerRight - segLeft) / 2;
    modeAllowButton =
        new SegmentedButton(
            segLeft,
            cursorY,
            segWidth,
            CONTROL_ROW_HEIGHT,
            Text.literal("Allowlist"),
            Palette.ALLOW,
            () -> activeProfile().map(p -> p.getMode() == FilterMode.ALLOWLIST).orElse(false),
            () -> setMode(FilterMode.ALLOWLIST));
    addDrawableChild.accept(modeAllowButton);
    allWidgets.add(modeAllowButton);
    lockableWidgets.add(modeAllowButton);
    modeDenyButton =
        new SegmentedButton(
            segLeft + segWidth,
            cursorY,
            segWidth,
            CONTROL_ROW_HEIGHT,
            Text.literal("Denylist"),
            Palette.DENY,
            () -> activeProfile().map(p -> p.getMode() == FilterMode.DENYLIST).orElse(false),
            () -> setMode(FilterMode.DENYLIST));
    addDrawableChild.accept(modeDenyButton);
    allWidgets.add(modeDenyButton);
    lockableWidgets.add(modeDenyButton);
    cursorY += CONTROL_ROW_HEIGHT + 2;

    // Action row.
    actionY = cursorY;
    actionLeaveButton =
        new SegmentedButton(
            segLeft,
            cursorY,
            segWidth,
            CONTROL_ROW_HEIGHT,
            Text.literal("Leave"),
            0xFF6A6F78,
            () ->
                activeProfile()
                    .map(p -> p.getRejectedItemAction() == RejectedItemAction.LEAVE_ON_GROUND)
                    .orElse(false),
            () -> setAction(RejectedItemAction.LEAVE_ON_GROUND));
    addDrawableChild.accept(actionLeaveButton);
    allWidgets.add(actionLeaveButton);
    lockableWidgets.add(actionLeaveButton);
    actionDeleteButton =
        new SegmentedButton(
            segLeft + segWidth,
            cursorY,
            segWidth,
            CONTROL_ROW_HEIGHT,
            Text.literal("Delete"),
            Palette.DENY,
            () ->
                activeProfile()
                    .map(p -> p.getRejectedItemAction() == RejectedItemAction.DELETE)
                    .orElse(false),
            () -> setAction(RejectedItemAction.DELETE));
    addDrawableChild.accept(actionDeleteButton);
    allWidgets.add(actionDeleteButton);
    lockableWidgets.add(actionDeleteButton);
    cursorY += CONTROL_ROW_HEIGHT + 4;

    // Summary block (painted in render()).
    summaryY = cursorY;
    cursorY += SUMMARY_HEIGHT + 3;

    // Tab strip.
    tabsY = cursorY;
    int tabWidth = innerWidth / 2;
    rulesTabButton =
        new VanillaTab(
            innerLeft,
            cursorY,
            tabWidth,
            TAB_HEIGHT,
            Text.literal("Rules"),
            () -> activeTab == PanelTab.RULES,
            () -> setTab(PanelTab.RULES));
    addDrawableChild.accept(rulesTabButton);
    allWidgets.add(rulesTabButton);
    lockableWidgets.add(rulesTabButton);
    settingsTabButton =
        new VanillaTab(
            innerLeft + tabWidth,
            cursorY,
            tabWidth,
            TAB_HEIGHT,
            Text.literal("Settings"),
            () -> activeTab == PanelTab.SETTINGS,
            () -> setTab(PanelTab.SETTINGS));
    addDrawableChild.accept(settingsTabButton);
    allWidgets.add(settingsTabButton);
    lockableWidgets.add(settingsTabButton);
    cursorY += TAB_HEIGHT;

    // Content well: dark recessed background, host for the active tab.
    contentY = cursorY;
    contentHeight = (panelY + HEIGHT - SIDE_PADDING) - cursorY;
    int contentInsetX = innerLeft + CONTENT_PADDING;
    int contentInsetY = contentY + CONTENT_PADDING;
    int contentInsetWidth = innerWidth - CONTENT_PADDING * 2;
    rulesView.attach(
        contentInsetX,
        contentInsetY,
        contentInsetWidth,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });
    settingsView.attach(
        contentInsetX,
        contentInsetY,
        contentInsetWidth,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });

    rebuildDropdownWidgets(
        addDrawableChild,
        pillX,
        panelY + SIDE_PADDING + HEADER_HEIGHT + PROFILE_ROW_HEIGHT + 5,
        pillWidth);

    applyVisibility();
    refresh();
  }

  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!open) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    Chrome.guiWindow(context, panelX, panelY, WIDTH, HEIGHT);

    // Header: brand icon + title text on the left.
    int iconSize = 16;
    int iconX = panelX + SIDE_PADDING + 1;
    int iconY = headerY + (HEADER_HEIGHT - iconSize) / 2;
    context.drawTexture(ICON_TEXTURE, iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
    context.drawText(
        client.textRenderer,
        Text.literal("Loot Lock"),
        iconX + iconSize + 4,
        headerY + (HEADER_HEIGHT - 8) / 2,
        Palette.INK,
        false);

    // Switch labels.
    int switchY = headerY + (HEADER_HEIGHT - 8) / 2;
    if (serverSwitch != null) {
      context.drawText(
          client.textRenderer,
          Text.literal("Server"),
          serverSwitch.getX() - 30,
          switchY,
          Palette.INK,
          false);
    }
    if (clientSwitch != null) {
      context.drawText(
          client.textRenderer,
          Text.literal("Client"),
          clientSwitch.getX() - 30,
          switchY,
          Palette.INK,
          false);
    }

    // Mode + Action row labels.
    context.drawText(
        client.textRenderer,
        Text.literal("Mode"),
        panelX + SIDE_PADDING,
        modeY + (CONTROL_ROW_HEIGHT - 8) / 2,
        Palette.INK,
        false);
    context.drawText(
        client.textRenderer,
        Text.literal("Action"),
        panelX + SIDE_PADDING,
        actionY + (CONTROL_ROW_HEIGHT - 8) / 2,
        Palette.INK,
        false);

    // Summary block: colored left border + dark recessed background + text.
    Optional<LootLockProfile> activeProfile = activeProfile();
    boolean globallyEnabled = currentGloballyEnabled();
    int accent;
    if (!globallyEnabled) {
      accent = 0xFF7A7A7A;
    } else {
      accent =
          activeProfile
              .map(p -> p.getMode() == FilterMode.ALLOWLIST ? Palette.ALLOW : Palette.DENY)
              .orElse(Palette.INFO);
    }
    Chrome.summaryBlock(
        context, panelX + SIDE_PADDING, summaryY, WIDTH - SIDE_PADDING * 2, SUMMARY_HEIGHT, accent);
    context.drawTextWrapped(
        client.textRenderer,
        LootLockSummaryText.build(globallyEnabled, activeProfile.orElse(null)),
        panelX + SIDE_PADDING + 8,
        summaryY + 5,
        WIDTH - SIDE_PADDING * 2 - 12,
        0xFFF0F0F0);

    // Content well behind the tab content.
    Chrome.well(context, panelX + SIDE_PADDING, contentY, WIDTH - SIDE_PADDING * 2, contentHeight);

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

  public PanelTab getActiveTab() {
    return activeTab;
  }

  public void refresh() {
    boolean globallyEnabled = currentGloballyEnabled();
    boolean canDelete = LootLockClient.getState().isAllowDeleteRejectedItems();
    if (actionDeleteButton != null) {
      actionDeleteButton.active = open && globallyEnabled && canDelete;
    }
    applyLock(globallyEnabled);
    rulesView.refresh();
  }

  public void handleClientToggle() {
    GlobalEnableController.toggle(MinecraftClient.getInstance());
  }

  void onClientSwitchPressed() {
    handleClientToggle();
  }

  /** Stable per-profile color derived from the profile's UUID hash. */
  static int colorForProfile(LootLockProfile profile) {
    int[] palette = {
      Palette.ALLOW,
      Palette.INFO,
      Palette.DENY,
      Palette.GOLD,
      Palette.PURPLE,
      0xFFD77A2A,
      0xFF3AA6A0,
      0xFF8A8A90
    };
    int hash = profile.getId() == null ? 0 : Math.abs(profile.getId().hashCode());
    return palette[hash % palette.length];
  }

  static String ruleCountLabel(LootLockProfile profile) {
    int n = profile.getRules() == null ? 0 : profile.getRules().size();
    String mode = profile.getMode() == FilterMode.DENYLIST ? "deny" : "allow";
    return mode + " . " + n + (n == 1 ? " item" : " items");
  }

  boolean currentGloballyEnabled() {
    return LootLockClient.getState()
        .getSnapshot()
        .map(LootLockPlayerData::isGloballyEnabled)
        .orElse(true);
  }

  private void applyVisibility() {
    for (ClickableWidget widget : allWidgets) {
      widget.visible = open;
    }
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = open && dropdownOpen;
    }
    rulesView.setVisible(open && activeTab == PanelTab.RULES);
    settingsView.setVisible(open && activeTab == PanelTab.SETTINGS);
  }

  private void applyLock(boolean enabled) {
    for (ClickableWidget widget : lockableWidgets) {
      widget.active = open && enabled;
    }
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
      int rowY = y;
      int miniWidth = 18;
      int rowNameWidth = width - miniWidth * 3 - 6;
      ButtonWidget switchButton =
          ButtonWidget.builder(
                  Text.literal(profile.getName()).formatted(Formatting.YELLOW),
                  b -> activateProfile(profile.getId()))
              .dimensions(x, rowY, rowNameWidth, PROFILE_ROW_HEIGHT)
              .build();
      ButtonWidget renameButton =
          ButtonWidget.builder(Text.literal("R"), b -> renameProfile(profile))
              .dimensions(x + rowNameWidth + 2, rowY, miniWidth, PROFILE_ROW_HEIGHT)
              .build();
      ButtonWidget duplicateButton =
          ButtonWidget.builder(Text.literal("D"), b -> duplicateProfile(profile))
              .dimensions(x + rowNameWidth + 2 + miniWidth + 2, rowY, miniWidth, PROFILE_ROW_HEIGHT)
              .build();
      ButtonWidget deleteButton =
          ButtonWidget.builder(Text.literal("X"), b -> deleteProfile(profile))
              .dimensions(
                  x + rowNameWidth + 2 + miniWidth * 2 + 4, rowY, miniWidth, PROFILE_ROW_HEIGHT)
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
      y += PROFILE_ROW_HEIGHT + 2;
    }
    newProfileButton =
        ButtonWidget.builder(Text.literal("+ New profile"), b -> createProfile())
            .dimensions(x, y, width, PROFILE_ROW_HEIGHT)
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
}
