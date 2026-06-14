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
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
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
  public static final int WIDTH = 270;

  /** Natural / maximum height; the actual rendered height is clamped to fit the screen. */
  public static final int HEIGHT = 360;

  /**
   * Minimum panel height that still renders a usable layout — below this we just hide the panel.
   */
  private static final int MIN_HEIGHT = 200;

  /**
   * Sticky panel-open state survives inventory close + ConfirmScreen detours, so the user does not
   * have to re-open the docked panel after every safety prompt. Lives on the class so the next
   * {@link InventoryScreen} that re-attaches a panel can restore it.
   */
  private static boolean STICKY_OPEN_STATE = false;

  private static PanelTab STICKY_ACTIVE_TAB = PanelTab.RULES;

  public static boolean getStickyOpenState() {
    return STICKY_OPEN_STATE;
  }

  public static PanelTab getStickyActiveTab() {
    return STICKY_ACTIVE_TAB;
  }

  private static final int SIDE_PADDING = 8;
  private static final int HEADER_HEIGHT = 26;
  private static final int PROFILE_ROW_HEIGHT = 20;
  private static final int CONTROL_ROW_HEIGHT = 18;
  private static final int SUMMARY_HEIGHT = 38;
  private static final int TAB_HEIGHT = 20;
  private static final int CONTENT_PADDING = 6;
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

  private Screen host;
  private int panelX;
  private int panelY;
  private int currentHeight = HEIGHT;

  /** True when the screen is too small to render the panel even at MIN_HEIGHT — hide it. */
  private boolean fitsOnScreen = true;

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

  // Anchor + signature used to rebuild the dropdown widgets when the profile list changes.
  private int dropdownAnchorX;
  private int dropdownAnchorY;
  private int dropdownAnchorWidth;
  private String dropdownSignature = "";
  private int dropdownFrameX;
  private int dropdownFrameY;
  private int dropdownFrameW;
  private int dropdownFrameH;

  // Inline-rename overlay shown in place of a dropdown row's name while editing.
  private UUID renamingProfileId;
  private TextFieldWidget renameField;

  // Position references for paint code that draws labels.
  private int headerY;
  private int profileY;
  private int profileWellY;
  private int profileWellH;
  private int modeY;
  private int actionY;
  private int controlsWellY;
  private int controlsWellH;
  private int summaryY;
  private int tabsY;
  private int contentY;
  private int contentHeight;

  public boolean isOpen() {
    return open;
  }

  /** True when the panel is open and the user is typing into the Rules tab search input. */
  public boolean isSearchFieldFocused() {
    return open && activeTab == PanelTab.RULES && rulesView.isSearchFieldFocused();
  }

  /** Forward a mouse-wheel event to the Rules tab so the user can scroll through results. */
  public boolean handleMouseScroll(double mouseX, double mouseY, double amount) {
    if (!open || activeTab != PanelTab.RULES) {
      return false;
    }
    return rulesView.mouseScrolledInRows(mouseX, mouseY, amount);
  }

  /** True when the given screen-space point falls inside the panel's rectangle. */
  public boolean containsPoint(double mouseX, double mouseY) {
    return open
        && fitsOnScreen
        && mouseX >= panelX
        && mouseX < panelX + WIDTH
        && mouseY >= panelY
        && mouseY < panelY + currentHeight;
  }

  /** Live current height; differs from {@link #HEIGHT} when clamped to a small screen. */
  public int getCurrentHeight() {
    return currentHeight;
  }

  /** True when the screen had room to actually render the panel. */
  public boolean fitsOnScreen() {
    return fitsOnScreen;
  }

  public void setOpen(boolean open) {
    this.open = open;
    STICKY_OPEN_STATE = open;
    if (!open) {
      cancelInlineRename();
      dropdownOpen = false;
    }
    applyVisibility();
  }

  public void toggleOpen() {
    setOpen(!open);
  }

  public void attach(
      Screen host, int panelX, int panelY, Consumer<ClickableWidget> addDrawableChild) {
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
    cursorY += HEADER_HEIGHT + 6;

    // Profile bar lives inside a dark recessed well per CSS (.profile-bar.well). Pad 4px around
    // the row so the chrome reads as the prototype's recessed pill carrier.
    profileWellY = cursorY;
    profileWellH = PROFILE_ROW_HEIGHT + 8;
    profileY = cursorY + 4;
    int navWidth = 14;
    int pillX = innerLeft + navWidth + 3;
    int pillWidth = innerWidth - navWidth * 2 - 6;
    int nextX = pillX + pillWidth + 3;
    cursorY = profileY;
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
    cursorY = profileWellY + profileWellH + 6;

    // Controls section: dark well containing the Mode + Action rows. Labels render in light text
    // (#d2d2d8) inside the well, per CSS .ctl-label color.
    int controlsPad = 5;
    controlsWellY = cursorY;
    controlsWellH = controlsPad * 2 + CONTROL_ROW_HEIGHT * 2 + 2;
    cursorY = controlsWellY + controlsPad;

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
            Palette.LEAVE,
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
    cursorY = controlsWellY + controlsWellH + 6;

    // Summary block (painted in render()).
    summaryY = cursorY;
    cursorY += SUMMARY_HEIGHT + 6;

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
    rulesView.attach(
        this,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });
    settingsView.attach(
        this,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });

    dropdownAnchorX = pillX;
    dropdownAnchorY = panelY + SIDE_PADDING + HEADER_HEIGHT + PROFILE_ROW_HEIGHT + 5;
    dropdownAnchorWidth = pillWidth;
    dropdownSignature = "";
    rebuildDropdownIfStale();

    // Mount the rename field as a host child up front (hidden) so vanilla's keyPressed /
    // charTyped routing reaches it naturally when we mark it focused during inline rename. The
    // field is repositioned on-demand inside startInlineRename().
    renameField =
        new TextFieldWidget(
            MinecraftClient.getInstance().textRenderer,
            panelX,
            panelY,
            16,
            12,
            Text.literal("rename"));
    renameField.setMaxLength(32);
    renameField.setDrawsBackground(true);
    renameField.visible = false;
    addDrawableChild.accept(renameField);
    allWidgets.add(renameField);

    applyVisibility();
    refresh();
  }

  public int getPanelX() {
    return panelX;
  }

  public int getPanelY() {
    return panelY;
  }

  /** Live X of the content well's inner inset, used by tab views to render container-relative. */
  public int getContentInsetX() {
    return panelX + SIDE_PADDING + CONTENT_PADDING;
  }

  /** Live Y of the content well's inner inset. Tracks panelY through relocate(). */
  public int getContentInsetY() {
    return contentY + CONTENT_PADDING;
  }

  /** Live width of the content inset area. */
  public int getContentInsetWidth() {
    return WIDTH - SIDE_PADDING * 2 - CONTENT_PADDING * 2;
  }

  /** Live height of the content inset area. */
  public int getContentInsetHeight() {
    return contentHeight - CONTENT_PADDING * 2;
  }

  /**
   * Returns true when the supplied right-edge boundary leaves enough room for the panel and the
   * screen is tall enough to fit at least the minimum panel height. Callers use this to decide
   * whether to render the docked panel inline or open the dedicated {@link LootLockScreen}.
   */
  public static boolean canDock(int anchorX, int scaledWidth, int scaledHeight) {
    int margin = 2;
    return anchorX + WIDTH + margin <= scaledWidth && scaledHeight >= MIN_HEIGHT + margin * 2;
  }

  /**
   * Recomputes the panel anchor and dimensions, then snaps every widget to the resulting layout.
   * Called once per frame from the host so changes to the recipe-book layout, window size or GUI
   * scale flow through naturally. The panel is vertically centered in the available screen height
   * and horizontally anchored at {@code anchorX}.
   *
   * @param anchorX preferred X (docked: inventory's right edge + gap; full-screen: screen center)
   * @param scaledWidth current screen width in GUI units
   * @param scaledHeight current screen height in GUI units
   */
  public void layout(int anchorX, int scaledWidth, int scaledHeight) {
    int margin = 2;
    int availableHeight = scaledHeight - margin * 2;
    int newHeight = Math.min(HEIGHT, Math.max(MIN_HEIGHT, availableHeight));
    boolean newFits = canDock(anchorX, scaledWidth, scaledHeight);

    int newX = Math.max(margin, Math.min(scaledWidth - WIDTH - margin, anchorX));
    int newY = Math.max(margin, (scaledHeight - newHeight) / 2);

    if (newX == panelX && newY == panelY && newHeight == currentHeight && newFits == fitsOnScreen) {
      return;
    }
    panelX = newX;
    panelY = newY;
    currentHeight = newHeight;
    fitsOnScreen = newFits;
    applyLayout();
  }

  /** Repositions every widget + layout reference based on current panelX, panelY, currentHeight. */
  private void applyLayout() {
    int innerLeft = panelX + SIDE_PADDING;
    int innerRight = panelX + WIDTH - SIDE_PADDING;
    int innerWidth = innerRight - innerLeft;
    int cursorY = panelY + SIDE_PADDING;

    // Header.
    headerY = cursorY;
    int switchY = cursorY + (HEADER_HEIGHT - SWITCH_HEIGHT) / 2;
    int serverSwitchX = innerRight - SWITCH_WIDTH;
    int clientSwitchX = serverSwitchX - SWITCH_WIDTH - 40 - 4;
    if (serverSwitch != null) {
      serverSwitch.setPosition(serverSwitchX, switchY);
    }
    if (clientSwitch != null) {
      clientSwitch.setPosition(clientSwitchX, switchY);
    }
    cursorY += HEADER_HEIGHT + 6;

    // Profile well + arrows + pill.
    profileWellY = cursorY;
    profileWellH = PROFILE_ROW_HEIGHT + 8;
    profileY = cursorY + 4;
    int navWidth = 14;
    int pillX = innerLeft + navWidth + 3;
    int pillWidth = innerWidth - navWidth * 2 - 6;
    int nextX = pillX + pillWidth + 3;
    if (prevProfileButton != null) {
      prevProfileButton.setPosition(innerLeft, profileY);
    }
    if (profilePill != null) {
      profilePill.setPosition(pillX, profileY);
    }
    if (nextProfileButton != null) {
      nextProfileButton.setPosition(nextX, profileY);
    }
    cursorY = profileWellY + profileWellH + 6;

    // Controls well.
    int controlsPad = 5;
    controlsWellY = cursorY;
    controlsWellH = controlsPad * 2 + CONTROL_ROW_HEIGHT * 2 + 2;
    cursorY = controlsWellY + controlsPad;
    modeY = cursorY;
    int segLeft = innerLeft + CTL_LABEL_WIDTH;
    int segWidth = (innerRight - segLeft) / 2;
    if (modeAllowButton != null) {
      modeAllowButton.setPosition(segLeft, cursorY);
    }
    if (modeDenyButton != null) {
      modeDenyButton.setPosition(segLeft + segWidth, cursorY);
    }
    cursorY += CONTROL_ROW_HEIGHT + 2;
    actionY = cursorY;
    if (actionLeaveButton != null) {
      actionLeaveButton.setPosition(segLeft, cursorY);
    }
    if (actionDeleteButton != null) {
      actionDeleteButton.setPosition(segLeft + segWidth, cursorY);
    }
    cursorY = controlsWellY + controlsWellH + 6;

    // Summary, tabs.
    summaryY = cursorY;
    cursorY += SUMMARY_HEIGHT + 6;
    tabsY = cursorY;
    int tabWidth = innerWidth / 2;
    if (rulesTabButton != null) {
      rulesTabButton.setPosition(innerLeft, cursorY);
    }
    if (settingsTabButton != null) {
      settingsTabButton.setPosition(innerLeft + tabWidth, cursorY);
    }
    cursorY += TAB_HEIGHT;

    // Content well — takes the remaining space so a shorter panel just shows fewer rows.
    contentY = cursorY;
    contentHeight = (panelY + currentHeight - SIDE_PADDING) - cursorY;
    if (contentHeight < CONTENT_PADDING * 2 + 20) {
      // Bottom out before going negative; rules view will clamp visibleRows to its floor.
      contentHeight = CONTENT_PADDING * 2 + 20;
    }

    // Re-anchor dropdown.
    dropdownAnchorX = pillX;
    dropdownAnchorY = panelY + SIDE_PADDING + HEADER_HEIGHT + PROFILE_ROW_HEIGHT + 5;
    dropdownAnchorWidth = pillWidth;
    dropdownSignature = "";
    rebuildDropdownIfStale();

    rulesView.relayout();
    settingsView.relayout();
  }

  /**
   * Paints the chrome (panel frame, dark wells, summary block backing, content well). Must run
   * BEFORE the host screen renders its widget children so the wells sit behind everything.
   */
  public void paintChrome(DrawContext context) {
    if (!open || !fitsOnScreen) {
      return;
    }
    Chrome.guiWindow(context, panelX, panelY, WIDTH, currentHeight);
    Chrome.well(
        context, panelX + SIDE_PADDING, profileWellY, WIDTH - SIDE_PADDING * 2, profileWellH);
    Chrome.well(
        context, panelX + SIDE_PADDING, controlsWellY, WIDTH - SIDE_PADDING * 2, controlsWellH);

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
    Chrome.well(context, panelX + SIDE_PADDING, contentY, WIDTH - SIDE_PADDING * 2, contentHeight);
  }

  /**
   * Paints labels, text, and the brand icon on TOP of the widgets so they read clearly. Must run
   * AFTER the host screen renders its widget children.
   */
  public void paintForeground(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!open || !fitsOnScreen) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();

    // Header: brand icon + title text on the left.
    int iconSize = 22;
    int iconX = panelX + SIDE_PADDING + 1;
    int iconY = headerY + (HEADER_HEIGHT - iconSize) / 2;
    context.drawTexture(ICON_TEXTURE, iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
    context.drawText(
        client.textRenderer,
        Text.literal("Loot Lock"),
        iconX + iconSize + 4,
        headerY + (HEADER_HEIGHT - 8) / 2,
        0xFF2F2F2F,
        false);

    int switchY = headerY + (HEADER_HEIGHT - 8) / 2;
    boolean showServer = serverSwitch != null && serverSwitch.visible;
    if (showServer) {
      context.drawText(
          client.textRenderer,
          Text.literal("Server"),
          serverSwitch.getX() - 36,
          switchY,
          0xFF2F2F2F,
          false);
    }
    if (clientSwitch != null) {
      context.drawText(
          client.textRenderer,
          Text.literal("Client"),
          clientSwitch.getX() - 36,
          switchY,
          0xFF2F2F2F,
          false);
    }

    // Mode + Action labels in light text on the dark controls well.
    context.drawText(
        client.textRenderer,
        Text.literal("Mode"),
        panelX + SIDE_PADDING + 4,
        modeY + (CONTROL_ROW_HEIGHT - 8) / 2,
        0xFFD2D2D8,
        false);
    context.drawText(
        client.textRenderer,
        Text.literal("Action"),
        panelX + SIDE_PADDING + 4,
        actionY + (CONTROL_ROW_HEIGHT - 8) / 2,
        0xFFD2D2D8,
        false);

    // Summary text on top of its colored block.
    Optional<LootLockProfile> activeProfile = activeProfile();
    boolean globallyEnabled = currentGloballyEnabled();
    context.drawTextWrapped(
        client.textRenderer,
        LootLockSummaryText.build(globallyEnabled, activeProfile.orElse(null)),
        panelX + SIDE_PADDING + 8,
        summaryY + 5,
        WIDTH - SIDE_PADDING * 2 - 12,
        0xFFF0F0F0);

    if (activeTab == PanelTab.RULES) {
      rulesView.render(context, mouseX, mouseY, delta);
    } else {
      settingsView.render(context, mouseX, mouseY, delta);
    }

    paintEffectsStrip(context, mouseX, mouseY);

    if (dropdownOpen) {
      renderDropdown(context, mouseX, mouseY, delta);
    }
  }

  /**
   * Paints a compact horizontal strip of active status-effect icons floating above the panel. We
   * suppress the vanilla effect column via mixin (it would otherwise overlap the panel), so this
   * strip is what keeps active effects visible without conflicting with the recipe book on the
   * inventory's left side. Icon-only, hover shows the same name + remaining-duration tooltip the
   * vanilla HUD uses.
   */
  private void paintEffectsStrip(DrawContext context, int mouseX, int mouseY) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.player == null) {
      return;
    }
    Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
    if (effects.isEmpty()) {
      return;
    }
    int iconSize = 18;
    int gap = 2;
    int stripHeight = iconSize + 4;
    int stripY = panelY - stripHeight - 2;
    if (stripY < 0) {
      stripY = panelY + 1;
    }
    int totalIconsWidth = effects.size() * iconSize + (effects.size() - 1) * gap;
    int maxWidth = WIDTH - 4;
    if (totalIconsWidth > maxWidth) {
      totalIconsWidth = maxWidth;
    }
    int stripX = panelX + (WIDTH - totalIconsWidth) / 2;
    int padX = 4;
    int padY = 2;
    context.fill(
        stripX - padX,
        stripY - padY,
        stripX + totalIconsWidth + padX,
        stripY + iconSize + padY,
        0xC0000000);
    StatusEffectSpriteManager spriteManager = client.getStatusEffectSpriteManager();
    int cursorX = stripX;
    StatusEffectInstance hoveredEffect = null;
    for (StatusEffectInstance effect : effects) {
      if (cursorX + iconSize > stripX + maxWidth) {
        break;
      }
      Sprite sprite = spriteManager.getSprite(effect.getEffectType());
      context.drawSprite(cursorX, stripY, 0, iconSize, iconSize, sprite);
      if (mouseX >= cursorX
          && mouseX < cursorX + iconSize
          && mouseY >= stripY
          && mouseY < stripY + iconSize) {
        hoveredEffect = effect;
      }
      cursorX += iconSize + gap;
    }
    if (hoveredEffect != null) {
      Text name = Text.translatable(hoveredEffect.getTranslationKey());
      Text duration = StatusEffectUtil.getDurationText(hoveredEffect, 1.0f);
      context.drawTooltip(
          client.textRenderer,
          List.of(name, duration.copy().formatted(Formatting.GRAY)),
          mouseX,
          mouseY);
    }
  }

  /**
   * Paints the dropdown popup chrome and re-renders the dropdown widgets on top of the host's
   * widget render pass. Without this overlay step the mode/action button widgets, which the host
   * renders after the chrome pass, would punch through the dropdown popup.
   */
  private void renderDropdown(DrawContext context, int mouseX, int mouseY, float delta) {
    if (dropdownWidgets.isEmpty()) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    // Drop shadow + dark well + small header strip per design 03-after.png.
    context.fill(
        dropdownFrameX + 3,
        dropdownFrameY + 3,
        dropdownFrameX + dropdownFrameW + 3,
        dropdownFrameY + dropdownFrameH + 3,
        0x80000000);
    Chrome.well(context, dropdownFrameX, dropdownFrameY, dropdownFrameW, dropdownFrameH);
    int headerY = dropdownFrameY + 4;
    context.drawText(
        client.textRenderer,
        Text.literal("Switch profile"),
        dropdownFrameX + 8,
        headerY,
        Palette.GOLD,
        false);
    context.fill(
        dropdownFrameX + 4,
        headerY + 10,
        dropdownFrameX + dropdownFrameW - 4,
        headerY + 11,
        0xFF1E1E22);
    for (ClickableWidget widget : dropdownWidgets) {
      if (widget.visible) {
        widget.render(context, mouseX, mouseY, delta);
      }
    }
    if (renameField != null) {
      renameField.render(context, mouseX, mouseY, delta);
    }
  }

  public void setTab(PanelTab tab) {
    if (tab == null || tab == activeTab) {
      return;
    }
    activeTab = tab;
    STICKY_ACTIVE_TAB = tab;
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
    if (rulesTabButton != null) {
      int ruleCount =
          activeProfile().map(p -> p.getRules() == null ? 0 : p.getRules().size()).orElse(0);
      rulesTabButton.setMessage(Text.literal("Rules (" + ruleCount + ")"));
    }
    // Hide the Server toggle when on an integrated single-player server — there's no real peer to
    // mirror, and the toggle just adds visual noise. When hidden, slide the Client switch right so
    // the header doesn't have a vacant gap.
    boolean integrated = isIntegratedSingleplayer();
    if (serverSwitch != null) {
      serverSwitch.visible = open && !integrated;
    }
    if (clientSwitch != null) {
      int rightAnchor = panelX + WIDTH - SIDE_PADDING - SWITCH_WIDTH;
      int target = integrated ? rightAnchor : rightAnchor - SWITCH_WIDTH - 40;
      if (clientSwitch.getX() != target) {
        clientSwitch.setX(target);
      }
    }
    applyLock(globallyEnabled);
    rulesView.refresh();
    rebuildDropdownIfStale();
  }

  static boolean isIntegratedSingleplayer() {
    MinecraftClient client = MinecraftClient.getInstance();
    return client != null && client.isIntegratedServerRunning();
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
      if (widget == renameField) {
        // Rename field has its own visibility lifecycle driven by start/cancelInlineRename.
        widget.visible = open && renamingProfileId != null;
      } else {
        widget.visible = open;
      }
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

  /**
   * Returns true if the dropdown is currently open and the click was inside its frame. Routes the
   * click through the dropdown's own widgets so a row click does not leak through to the inventory
   * slots underneath. When the click is inside the frame but no widget claimed it, the dropdown
   * stays open. When the click is outside the frame, the dropdown closes and the click bubbles to
   * vanilla.
   */
  public boolean handleDropdownMouseClick(double mouseX, double mouseY, int button) {
    if (!open || !dropdownOpen) {
      return false;
    }
    boolean insideFrame =
        mouseX >= dropdownFrameX
            && mouseX < dropdownFrameX + dropdownFrameW
            && mouseY >= dropdownFrameY
            && mouseY < dropdownFrameY + dropdownFrameH;
    if (!insideFrame) {
      // Click anywhere off the popup dismisses the dropdown and lets the click bubble to vanilla
      // so the user can still operate the rest of the inventory in the same gesture. An in-flight
      // rename commits so the user does not lose pending edits when they click away.
      if (isInlineRenameActive()) {
        commitInlineRename();
      }
      closeDropdown();
      return false;
    }
    if (renameField != null
        && mouseX >= renameField.getX()
        && mouseX < renameField.getX() + renameField.getWidth()
        && mouseY >= renameField.getY()
        && mouseY < renameField.getY() + renameField.getHeight()) {
      renameField.mouseClicked(mouseX, mouseY, button);
      return true;
    }
    for (ClickableWidget widget : dropdownWidgets) {
      if (widget.visible && widget.mouseClicked(mouseX, mouseY, button)) {
        return true;
      }
    }
    return true;
  }

  /** Rebuilds dropdown widgets if the profile list signature has changed since the last build. */
  private void rebuildDropdownIfStale() {
    if (isInlineRenameActive()) {
      // Skip rebuild while the user is editing — the row layout would otherwise jump under their
      // cursor. Commit or cancel triggers the next rebuild via refresh().
      return;
    }
    Optional<LootLockPlayerData> snapshotOptional = LootLockClient.getState().getSnapshot();
    StringBuilder sigBuilder = new StringBuilder();
    List<LootLockProfile> profiles;
    UUID activeId;
    if (snapshotOptional.isEmpty()) {
      profiles = List.of();
      activeId = null;
      sigBuilder.append("empty");
    } else {
      LootLockPlayerData snapshot = snapshotOptional.get();
      profiles = snapshot.getProfiles();
      activeId = snapshot.getActiveProfileId();
      sigBuilder.append(activeId == null ? "-" : activeId.toString());
      for (LootLockProfile profile : profiles) {
        sigBuilder
            .append('|')
            .append(profile.getId())
            .append('=')
            .append(profile.getName())
            .append(':')
            .append(ruleCountLabel(profile));
      }
    }
    String newSignature = sigBuilder.toString();
    if (newSignature.equals(dropdownSignature) && !dropdownWidgets.isEmpty()) {
      return;
    }
    dropdownSignature = newSignature;
    dropdownWidgets.clear();
    newProfileButton = null;

    int headerStripHeight = 14;
    int rowHeight = ProfileDropdownRow.ROW_HEIGHT;
    int actionsWidth = ProfileDropdownRow.ACTIONS_WIDTH;
    int rowMainWidth = dropdownAnchorWidth - actionsWidth - 4;
    int rowsTop = dropdownAnchorY + headerStripHeight;
    int y = rowsTop;
    for (LootLockProfile profile : profiles) {
      boolean isActive = profile.getId().equals(activeId);
      ProfileDropdownRow rowMain =
          new ProfileDropdownRow(
              dropdownAnchorX,
              y,
              rowMainWidth,
              profile.getId(),
              colorForProfile(profile),
              profile.getName(),
              ruleCountLabel(profile),
              isActive,
              () -> activateProfile(profile.getId()));
      int actionsX = dropdownAnchorX + rowMainWidth + 2;
      int gap = (actionsWidth - MiniActionButton.SIZE * 3) / 4;
      MiniActionButton renameButton =
          new MiniActionButton(actionsX + gap, y + 2, "R", false, () -> renameProfile(profile));
      MiniActionButton duplicateButton =
          new MiniActionButton(
              actionsX + gap * 2 + MiniActionButton.SIZE,
              y + 2,
              "D",
              false,
              () -> duplicateProfile(profile));
      MiniActionButton deleteButton =
          new MiniActionButton(
              actionsX + gap * 3 + MiniActionButton.SIZE * 2,
              y + 2,
              "X",
              true,
              () -> deleteProfile(profile));
      deleteButton.active = ProfileUiController.canDelete(profiles);

      dropdownWidgets.add(rowMain);
      dropdownWidgets.add(renameButton);
      dropdownWidgets.add(duplicateButton);
      dropdownWidgets.add(deleteButton);
      y += rowHeight + 1;
    }
    newProfileButton =
        ButtonWidget.builder(
                Text.literal("+ New profile").formatted(Formatting.GREEN), b -> createProfile())
            .dimensions(dropdownAnchorX, y + 3, dropdownAnchorWidth, 16)
            .build();
    dropdownWidgets.add(newProfileButton);

    int framePad = 5;
    int frameTop = dropdownAnchorY - framePad;
    int frameBottom = y + 3 + 16 + framePad;
    dropdownFrameX = dropdownAnchorX - framePad;
    dropdownFrameY = frameTop;
    dropdownFrameW = dropdownAnchorWidth + framePad * 2;
    dropdownFrameH = frameBottom - frameTop;

    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = open && dropdownOpen;
    }
  }

  private void toggleDropdown() {
    dropdownOpen = !dropdownOpen;
    if (dropdownOpen) {
      rebuildDropdownIfStale();
    }
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
    if (action == RejectedItemAction.DELETE && shouldConfirmEnableDelete()) {
      MinecraftClient client = MinecraftClient.getInstance();
      net.minecraft.client.gui.screen.Screen current = client.currentScreen;
      client.setScreen(
          new net.minecraft.client.gui.screen.ConfirmScreen(
              confirmed -> {
                client.setScreen(current);
                if (confirmed) {
                  mutateActive(draft -> draft.setRejectedItemAction(RejectedItemAction.DELETE));
                }
              },
              Text.literal("Enable delete mode?"),
              Text.literal(
                  "Rejected dropped items are permanently deleted and cannot be recovered.")));
      return;
    }
    mutateActive(draft -> draft.setRejectedItemAction(action));
  }

  private boolean shouldConfirmEnableDelete() {
    Optional<LootLockProfile> active = activeProfile();
    if (active.isEmpty()) {
      return false;
    }
    if (active.get().getRejectedItemAction() == RejectedItemAction.DELETE) {
      return false;
    }
    return LootLockClient.getClientSettingsManager() != null
        && LootLockClient.getClientSettingsManager()
            .getSettingsCopy()
            .isConfirmBeforeEnablingDelete();
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
    startInlineRename(profile);
  }

  /**
   * Begins an inline rename for the supplied profile: creates a focused text field positioned over
   * the row's name baseline, pre-fills the existing name with all characters selected, and hides
   * the row's static name label so the field reads cleanly.
   */
  private void startInlineRename(LootLockProfile profile) {
    if (renameField == null) {
      return;
    }
    cancelInlineRename();
    ProfileDropdownRow row = findDropdownRow(profile.getId());
    if (row == null) {
      return;
    }
    renamingProfileId = profile.getId();
    int fieldX = row.nameRenderX() - 2;
    int fieldY = row.nameRenderY() - 2;
    int fieldWidth = row.getX() + row.getWidth() - fieldX - 2;
    renameField.setPosition(fieldX, fieldY);
    renameField.setWidth(fieldWidth);
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

  private void cancelInlineRename() {
    if (renamingProfileId == null) {
      return;
    }
    ProfileDropdownRow row = findDropdownRow(renamingProfileId);
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

  private ProfileDropdownRow findDropdownRow(UUID profileId) {
    if (profileId == null) {
      return null;
    }
    for (ClickableWidget widget : dropdownWidgets) {
      if (widget instanceof ProfileDropdownRow row && profileId.equals(row.getProfileId())) {
        return row;
      }
    }
    return null;
  }

  /** Returns true if an inline rename is currently in progress. */
  public boolean isInlineRenameActive() {
    return renamingProfileId != null && renameField != null && renameField.visible;
  }

  /** Routes a key press to the rename field when active. Enter commits, Escape aborts. */
  public boolean handleInlineRenameKey(int keyCode, int scanCode, int modifiers) {
    if (!isInlineRenameActive()) {
      return false;
    }
    if (keyCode == 257 || keyCode == 335) { // GLFW_KEY_ENTER, GLFW_KEY_KP_ENTER
      commitInlineRename();
      return true;
    }
    if (keyCode == 256) { // GLFW_KEY_ESCAPE
      cancelInlineRename();
      return true;
    }
    return renameField.keyPressed(keyCode, scanCode, modifiers);
  }

  /** Routes a char input to the rename field when active. */
  public boolean handleInlineRenameChar(char chr, int modifiers) {
    if (!isInlineRenameActive()) {
      return false;
    }
    return renameField.charTyped(chr, modifiers);
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
    // Leave the dropdown open so the newly added row appears in place and the user can immediately
    // rename / duplicate / delete it without re-opening the popup. The next refresh() picks up the
    // server-confirmed profile and rebuilds the dropdown rows.
    ClientMutationSync.sendCreateRequest(snapshot.getRevision(), name, null);
  }

  private void closeDropdown() {
    if (isInlineRenameActive()) {
      cancelInlineRename();
    }
    dropdownOpen = false;
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = false;
    }
  }
}
