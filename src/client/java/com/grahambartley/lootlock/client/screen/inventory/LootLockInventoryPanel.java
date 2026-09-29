package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.network.ClientMutationSync;
import com.grahambartley.lootlock.client.screen.ProfileImportScreen;
import com.grahambartley.lootlock.client.screen.ProfileUiController;
import com.grahambartley.lootlock.client.state.ClientDraftProfile;
import com.grahambartley.lootlock.client.state.ClientLootLockState;
import com.grahambartley.lootlock.client.state.ClientLootLockState.ClientDraftSaveRequest;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
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

public final class LootLockInventoryPanel {
  public static final int WIDTH = 270;

  public static final int HEIGHT = 360;

  private static final int MIN_HEIGHT = 200;

  private static boolean STICKY_OPEN_STATE = false;

  private static PanelTab STICKY_ACTIVE_TAB = PanelTab.RULES;

  private static boolean PENDING_DROPDOWN_REOPEN = false;

  public static boolean getStickyOpenState() {
    return STICKY_OPEN_STATE;
  }

  public static PanelTab getStickyActiveTab() {
    return STICKY_ACTIVE_TAB;
  }

  public static void requestDropdownReopen() {
    PENDING_DROPDOWN_REOPEN = true;
  }

  static final int FLASH_START_COLOR = 0xFF3F5A3A;

  static final long FLASH_DURATION_MILLIS = 500L;

  private static final int ARMED_BORDER_THICKNESS = 3;

  static LongSupplier clockMillis = System::currentTimeMillis;

  static Consumer<ClientLootLockState.ClientDraftSaveRequest> saveRequestDispatcher =
      ClientMutationSync::sendSaveRequest;

  private static final int SIDE_PADDING = 8;
  private static final int DROPDOWN_FRAME_PAD = 5;
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

  private boolean fitsOnScreen = true;

  private boolean open;
  private boolean dropdownOpen;
  private boolean dropArmed;
  private long flashStartMillis = -1L;

  private boolean clientPrefsMode;

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

  private int dropdownAnchorX;
  private int dropdownAnchorY;
  private int dropdownAnchorWidth;
  private String dropdownSignature = "";
  private int dropdownFrameX;
  private int dropdownFrameY;
  private int dropdownFrameW;
  private int dropdownFrameH;

  private UUID renamingProfileId;
  private TextFieldWidget renameField;

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

  public boolean isSearchFieldFocused() {
    return open && activeTab == PanelTab.RULES && rulesView.isSearchFieldFocused();
  }

  public boolean handleMouseScroll(double mouseX, double mouseY, double amount) {
    if (!open) {
      return false;
    }
    if (activeTab == PanelTab.RULES) {
      return rulesView.mouseScrolledInRows(mouseX, mouseY, amount);
    }
    return settingsView.mouseScrolled(mouseX, mouseY, amount);
  }

  public boolean containsPoint(double mouseX, double mouseY) {
    return open
        && fitsOnScreen
        && mouseX >= panelX
        && mouseX < panelX + WIDTH
        && mouseY >= panelY
        && mouseY < panelY + currentHeight;
  }

  public int getCurrentHeight() {
    return currentHeight;
  }

  public boolean fitsOnScreen() {
    return fitsOnScreen;
  }

  public void setOpen(boolean open) {
    this.open = open;
    if (!clientPrefsMode) {
      STICKY_OPEN_STATE = open;
    }
    if (!open) {
      cancelInlineRename();
      dropdownOpen = false;
    } else if (PENDING_DROPDOWN_REOPEN && !clientPrefsMode) {
      PENDING_DROPDOWN_REOPEN = false;
      dropdownOpen = true;
      rebuildDropdownIfStale();
    }
    applyVisibility();
  }

  public void setClientPrefsMode(boolean clientPrefsMode) {
    if (!allWidgets.isEmpty()) {
      throw new IllegalStateException(
          "setClientPrefsMode must be called before attach; the panel has already materialized its"
              + " widgets.");
    }
    this.clientPrefsMode = clientPrefsMode;
    if (clientPrefsMode) {
      activeTab = PanelTab.SETTINGS;
    }
  }

  public void toggleOpen() {
    setOpen(!open);
  }

  public void setDropArmed(boolean armed) {
    this.dropArmed = armed;
  }

  public boolean isDropArmed() {
    return dropArmed;
  }

  public void flashDropSuccess() {
    this.flashStartMillis = clockMillis.getAsLong();
  }

  public boolean isFlashActive() {
    return flashProgress() < 1f;
  }

  public float flashProgress() {
    if (flashStartMillis < 0L) {
      return 1f;
    }
    long elapsed = clockMillis.getAsLong() - flashStartMillis;
    if (elapsed < 0L || elapsed >= FLASH_DURATION_MILLIS) {
      flashStartMillis = -1L;
      return 1f;
    }
    return (float) elapsed / (float) FLASH_DURATION_MILLIS;
  }

  public void clearRulesSearch() {
    rulesView.clearSearch();
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

    headerY = cursorY;
    if (!clientPrefsMode) {
      int switchY = cursorY + (HEADER_HEIGHT - SWITCH_HEIGHT) / 2;
      int serverSwitchX = innerRight - SWITCH_WIDTH;
      int serverLabelX = serverSwitchX - 32;
      int clientSwitchX = serverLabelX - SWITCH_WIDTH - 4;
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
    }
    cursorY += HEADER_HEIGHT + 6;

    if (!clientPrefsMode) {
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
              innerLeft,
              cursorY,
              navWidth,
              PROFILE_ROW_HEIGHT,
              false,
              () -> cycleActiveProfile(-1));
      addDrawableChild.accept(prevProfileButton);
      allWidgets.add(prevProfileButton);
      lockableWidgets.add(prevProfileButton);
      profilePill =
          new ProfilePill(
              pillX,
              cursorY,
              pillWidth,
              PROFILE_ROW_HEIGHT,
              () ->
                  activeProfile().map(LootLockInventoryPanel::colorForProfile).orElse(Palette.SLOT),
              () ->
                  activeProfile()
                      .map(LootLockProfile::getName)
                      .orElseGet(
                          () -> Text.translatable(LootLockLang.PROFILE_PLACEHOLDER).getString()),
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

      int controlsPad = 5;
      controlsWellY = cursorY;
      controlsWellH = controlsPad * 2 + CONTROL_ROW_HEIGHT * 2 + 2;
      cursorY = controlsWellY + controlsPad;

      modeY = cursorY;
      int segLeft = innerLeft + CTL_LABEL_WIDTH;
      int segWidth = (innerRight - segLeft) / 2;
      modeAllowButton =
          new SegmentedButton(
              segLeft,
              cursorY,
              segWidth,
              CONTROL_ROW_HEIGHT,
              Text.translatable(LootLockLang.MODE_ALLOWLIST),
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
              Text.translatable(LootLockLang.MODE_DENYLIST),
              Palette.DENY,
              () -> activeProfile().map(p -> p.getMode() == FilterMode.DENYLIST).orElse(false),
              () -> setMode(FilterMode.DENYLIST));
      addDrawableChild.accept(modeDenyButton);
      allWidgets.add(modeDenyButton);
      lockableWidgets.add(modeDenyButton);
      cursorY += CONTROL_ROW_HEIGHT + 2;

      actionY = cursorY;
      actionLeaveButton =
          new SegmentedButton(
              segLeft,
              cursorY,
              segWidth,
              CONTROL_ROW_HEIGHT,
              Text.translatable(LootLockLang.ACTION_LEAVE),
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
              Text.translatable(LootLockLang.ACTION_DELETE),
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

      summaryY = cursorY;
      cursorY += SUMMARY_HEIGHT + 6;

      tabsY = cursorY;
      int tabWidth = innerWidth / 2;
      rulesTabButton =
          new VanillaTab(
              innerLeft,
              cursorY,
              tabWidth,
              TAB_HEIGHT,
              Text.translatable(LootLockLang.TAB_RULES),
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
              Text.translatable(LootLockLang.TAB_SETTINGS),
              () -> activeTab == PanelTab.SETTINGS,
              () -> setTab(PanelTab.SETTINGS));
      addDrawableChild.accept(settingsTabButton);
      allWidgets.add(settingsTabButton);
      lockableWidgets.add(settingsTabButton);
      cursorY += TAB_HEIGHT;
    }

    contentY = cursorY;
    contentHeight = (panelY + HEIGHT - SIDE_PADDING) - cursorY;
    if (!clientPrefsMode) {
      rulesView.attach(
          this,
          widget -> {
            addDrawableChild.accept(widget);
            allWidgets.add(widget);
            lockableWidgets.add(widget);
          });
    }
    settingsView.setShowServerPolicy(!clientPrefsMode);
    settingsView.attach(
        this,
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
          lockableWidgets.add(widget);
        });

    if (!clientPrefsMode) {
      dropdownAnchorX = innerLeft + DROPDOWN_FRAME_PAD;
      dropdownAnchorY = profileWellY + profileWellH + DROPDOWN_FRAME_PAD;
      dropdownAnchorWidth = innerWidth - DROPDOWN_FRAME_PAD * 2;
      dropdownSignature = "";
      rebuildDropdownIfStale();

      renameField =
          new TextFieldWidget(
              MinecraftClient.getInstance().textRenderer,
              panelX,
              panelY,
              16,
              12,
              Text.translatable(LootLockLang.PROFILE_RENAME_FIELD));
      renameField.setMaxLength(32);
      renameField.setDrawsBackground(true);
      renameField.visible = false;
      addDrawableChild.accept(renameField);
      allWidgets.add(renameField);
    }

    applyVisibility();
    refresh();
  }

  public int getPanelX() {
    return panelX;
  }

  public int getPanelY() {
    return panelY;
  }

  public int getContentInsetX() {
    return panelX + SIDE_PADDING + CONTENT_PADDING;
  }

  public int getContentInsetY() {
    return contentY + CONTENT_PADDING;
  }

  public int getContentInsetWidth() {
    return WIDTH - SIDE_PADDING * 2 - CONTENT_PADDING * 2;
  }

  public int getContentInsetHeight() {
    return contentHeight - CONTENT_PADDING * 2;
  }

  public static boolean canDock(int anchorX, int scaledWidth, int scaledHeight) {
    int margin = 2;
    return anchorX + WIDTH + margin <= scaledWidth && scaledHeight >= MIN_HEIGHT + margin * 2;
  }

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

  private void applyLayout() {
    int innerLeft = panelX + SIDE_PADDING;
    int innerRight = panelX + WIDTH - SIDE_PADDING;
    int innerWidth = innerRight - innerLeft;
    int cursorY = panelY + SIDE_PADDING;

    headerY = cursorY;
    if (!clientPrefsMode) {
      int switchY = cursorY + (HEADER_HEIGHT - SWITCH_HEIGHT) / 2;
      int serverSwitchX = innerRight - SWITCH_WIDTH;
      int clientSwitchX = serverSwitchX - SWITCH_WIDTH - 40 - 4;
      if (serverSwitch != null) {
        serverSwitch.setPosition(serverSwitchX, switchY);
      }
      if (clientSwitch != null) {
        clientSwitch.setPosition(clientSwitchX, switchY);
      }
    }
    cursorY += HEADER_HEIGHT + 6;

    if (!clientPrefsMode) {
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
    }

    contentY = cursorY;
    contentHeight = (panelY + currentHeight - SIDE_PADDING) - cursorY;
    if (contentHeight < CONTENT_PADDING * 2 + 20) {
      contentHeight = CONTENT_PADDING * 2 + 20;
    }

    if (!clientPrefsMode) {
      dropdownAnchorX = innerLeft + DROPDOWN_FRAME_PAD;
      dropdownAnchorY = profileWellY + profileWellH + DROPDOWN_FRAME_PAD;
      dropdownAnchorWidth = innerWidth - DROPDOWN_FRAME_PAD * 2;
      dropdownSignature = "";
      rebuildDropdownIfStale();

      rulesView.relayout();
    }
    settingsView.relayout();
  }

  public void paintChrome(DrawContext context) {
    if (!open || !fitsOnScreen) {
      return;
    }
    Chrome.guiWindow(context, panelX, panelY, WIDTH, currentHeight);
    if (clientPrefsMode) {
      Chrome.well(
          context, panelX + SIDE_PADDING, contentY, WIDTH - SIDE_PADDING * 2, contentHeight);
      return;
    }
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
    paintRulesWellOverlays(context);
  }

  private void paintRulesWellOverlays(DrawContext context) {
    if (activeTab != PanelTab.RULES) {
      return;
    }
    int wellX = panelX + SIDE_PADDING;
    int wellY = contentY;
    int wellX2 = wellX + WIDTH - SIDE_PADDING * 2;
    int wellY2 = wellY + contentHeight;
    if (isFlashActive()) {
      int blended = blendArgb(FLASH_START_COLOR, Palette.WELL, flashProgress());
      context.fill(wellX + 1, wellY + 1, wellX2 - 1, wellY2 - 1, blended);
    }
    if (dropArmed) {
      int t = ARMED_BORDER_THICKNESS;
      context.fill(wellX, wellY, wellX2, wellY + t, Palette.GOLD);
      context.fill(wellX, wellY2 - t, wellX2, wellY2, Palette.GOLD);
      context.fill(wellX, wellY + t, wellX + t, wellY2 - t, Palette.GOLD);
      context.fill(wellX2 - t, wellY + t, wellX2, wellY2 - t, Palette.GOLD);
    }
  }

  private static int blendArgb(int from, int to, float t) {
    if (t <= 0f) {
      return from;
    }
    if (t >= 1f) {
      return to;
    }
    int fa = (from >>> 24) & 0xFF;
    int fr = (from >> 16) & 0xFF;
    int fg = (from >> 8) & 0xFF;
    int fb = from & 0xFF;
    int ta = (to >>> 24) & 0xFF;
    int tr = (to >> 16) & 0xFF;
    int tg = (to >> 8) & 0xFF;
    int tb = to & 0xFF;
    int a = (int) (fa + (ta - fa) * t);
    int r = (int) (fr + (tr - fr) * t);
    int g = (int) (fg + (tg - fg) * t);
    int b = (int) (fb + (tb - fb) * t);
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  public void paintForeground(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!open || !fitsOnScreen) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();

    int iconSize = 22;
    int iconX = panelX + SIDE_PADDING + 1;
    int iconY = headerY + (HEADER_HEIGHT - iconSize) / 2;
    context.drawTexture(ICON_TEXTURE, iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
    context.drawText(
        client.textRenderer,
        Text.translatable(LootLockLang.BRAND),
        iconX + iconSize + 4,
        headerY + (HEADER_HEIGHT - 8) / 2,
        0xFF2F2F2F,
        false);

    if (clientPrefsMode) {
      settingsView.render(context, mouseX, mouseY, delta);
      return;
    }

    int switchY = headerY + (HEADER_HEIGHT - 8) / 2;
    boolean showServer = serverSwitch != null && serverSwitch.visible;
    if (showServer) {
      context.drawText(
          client.textRenderer,
          Text.translatable(LootLockLang.PANEL_HEADER_SERVER),
          serverSwitch.getX() - 36,
          switchY,
          0xFF2F2F2F,
          false);
    }
    if (clientSwitch != null) {
      context.drawText(
          client.textRenderer,
          Text.translatable(LootLockLang.PANEL_HEADER_PLAYER),
          clientSwitch.getX() - 36,
          switchY,
          0xFF2F2F2F,
          false);
    }

    context.drawText(
        client.textRenderer,
        Text.translatable(LootLockLang.PANEL_LABEL_MODE),
        panelX + SIDE_PADDING + 4,
        modeY + (CONTROL_ROW_HEIGHT - 8) / 2,
        0xFFD2D2D8,
        false);
    context.drawText(
        client.textRenderer,
        Text.translatable(LootLockLang.PANEL_LABEL_ACTION),
        panelX + SIDE_PADDING + 4,
        actionY + (CONTROL_ROW_HEIGHT - 8) / 2,
        0xFFD2D2D8,
        false);

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
      Text duration =
          StatusEffectUtil.getDurationText(
              hoveredEffect,
              1.0f,
              client.world == null ? 20.0f : client.world.getTickManager().getTickRate());
      context.drawTooltip(
          client.textRenderer,
          List.of(name, duration.copy().formatted(Formatting.GRAY)),
          mouseX,
          mouseY);
    }
  }

  private void renderDropdown(DrawContext context, int mouseX, int mouseY, float delta) {
    if (dropdownWidgets.isEmpty()) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
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
        Text.translatable(LootLockLang.DROPDOWN_SWITCH_PROFILE),
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
    paintChipHoverTooltip(context, mouseX, mouseY);
  }

  private void paintChipHoverTooltip(DrawContext context, int mouseX, int mouseY) {
    if (isInlineRenameActive()) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    for (ClickableWidget widget : dropdownWidgets) {
      if (!(widget instanceof ProfileDropdownRow row) || !row.visible) {
        continue;
      }
      if (row.isMouseOverChip(mouseX, mouseY)) {
        context.drawTooltip(
            client.textRenderer,
            List.of(Text.translatable(LootLockLang.DROPDOWN_CHANGE_COLOR)),
            mouseX,
            mouseY);
        return;
      }
    }
  }

  public void setTab(PanelTab tab) {
    if (tab == null || tab == activeTab || clientPrefsMode) {
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
    if (clientPrefsMode) {
      return;
    }
    boolean globallyEnabled = currentGloballyEnabled();
    boolean canDelete = LootLockClient.getState().isAllowDeleteRejectedItems();
    if (actionDeleteButton != null) {
      actionDeleteButton.active = open && globallyEnabled && canDelete;
    }
    if (rulesTabButton != null) {
      int ruleCount =
          activeProfile().map(p -> p.getRules() == null ? 0 : p.getRules().size()).orElse(0);
      rulesTabButton.setMessage(Text.translatable(LootLockLang.TAB_RULES_COUNT, ruleCount));
    }
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

  public static int colorForProfile(LootLockProfile profile) {
    int color = profile.getColor();
    return color == 0 ? Palette.PROFILE_COLORS[0] : color;
  }

  public void cycleProfileColor(UUID profileId) {
    if (profileId == null) {
      return;
    }
    ClientLootLockState state = LootLockClient.getState();
    Optional<LootLockPlayerData> snapshotOptional = state.getSnapshot();
    if (snapshotOptional.isEmpty()) {
      return;
    }
    LootLockProfile profile = null;
    for (LootLockProfile candidate : snapshotOptional.get().getProfiles()) {
      if (profileId.equals(candidate.getId())) {
        profile = candidate;
        break;
      }
    }
    if (profile == null) {
      return;
    }
    int nextColor = nextProfileColor(profile.getColor());
    state
        .beginDraft(profileId)
        .ifPresent(
            draft -> {
              draft.setColor(nextColor);
              state.buildSaveRequest().ifPresent(saveRequestDispatcher);
            });
  }

  static int nextProfileColor(int currentColor) {
    int[] palette = Palette.PROFILE_COLORS;
    int currentIndex = 0;
    for (int i = 0; i < palette.length; i++) {
      if (palette[i] == currentColor) {
        currentIndex = i;
        break;
      }
    }
    return palette[(currentIndex + 1) % palette.length];
  }

  static String ruleCountLabel(LootLockProfile profile) {
    int n = profile.getRules() == null ? 0 : profile.getRules().size();
    String key;
    if (profile.getMode() == FilterMode.DENYLIST) {
      key = n == 1 ? LootLockLang.PROFILE_META_DENY_ONE : LootLockLang.PROFILE_META_DENY_MANY;
    } else {
      key = n == 1 ? LootLockLang.PROFILE_META_ALLOW_ONE : LootLockLang.PROFILE_META_ALLOW_MANY;
    }
    return Text.translatable(key, n).getString();
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
        widget.visible = open && renamingProfileId != null;
      } else {
        widget.visible = open;
      }
    }
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = open && dropdownOpen;
    }
    rulesView.setVisible(!clientPrefsMode && open && activeTab == PanelTab.RULES);
    rulesView.setOverlayHidden(open && dropdownOpen, dropdownFrameY + dropdownFrameH);
    settingsView.setVisible(open && activeTab == PanelTab.SETTINGS);
  }

  private void applyLock(boolean enabled) {
    for (ClickableWidget widget : lockableWidgets) {
      widget.active = open && enabled;
    }
  }

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
      if (profilePill != null
          && mouseX >= profilePill.getX()
          && mouseX < profilePill.getX() + profilePill.getWidth()
          && mouseY >= profilePill.getY()
          && mouseY < profilePill.getY() + profilePill.getHeight()) {
        if (isInlineRenameActive()) {
          commitInlineRename();
        }
        return false;
      }
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

  private void rebuildDropdownIfStale() {
    if (isInlineRenameActive()) {
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
            .append(ruleCountLabel(profile))
            .append(':')
            .append(profile.getColor());
      }
    }
    String newSignature = sigBuilder.toString();
    if (newSignature.equals(dropdownSignature) && !dropdownWidgets.isEmpty()) {
      return;
    }
    dropdownSignature = newSignature;
    dropdownWidgets.clear();
    newProfileButton = null;

    boolean canCreate = ProfileUiController.canCreateProfile(profiles);
    Tooltip atCapacityTooltip =
        Tooltip.of(Text.translatable(LootLockLang.DROPDOWN_AT_CAPACITY, PacketLimits.MAX_PROFILES));

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
              () -> activateProfile(profile.getId()),
              () -> cycleProfileColor(profile.getId()));
      int actionsX = dropdownAnchorX + rowMainWidth + 2;
      int gap = (actionsWidth - MiniActionButton.SIZE * 4) / 5;
      MiniActionButton renameButton =
          new MiniActionButton(
              actionsX + gap,
              y + 2,
              Text.translatable(LootLockLang.BUTTON_MINI_RENAME),
              false,
              () -> renameProfile(profile));
      MiniActionButton duplicateButton =
          new MiniActionButton(
              actionsX + gap * 2 + MiniActionButton.SIZE,
              y + 2,
              Text.translatable(LootLockLang.BUTTON_MINI_DUPLICATE),
              false,
              () -> duplicateProfile(profile));
      duplicateButton.active = canCreate;
      if (!canCreate) {
        duplicateButton.setTooltip(atCapacityTooltip);
      }
      MiniActionButton exportButton =
          new MiniActionButton(
              actionsX + gap * 3 + MiniActionButton.SIZE * 2,
              y + 2,
              Text.translatable(LootLockLang.BUTTON_MINI_EXPORT),
              false,
              () -> exportProfile(profile));
      exportButton.setTooltip(
          Tooltip.of(Text.translatable(LootLockLang.BUTTON_MINI_EXPORT_TOOLTIP)));
      MiniActionButton deleteButton =
          new MiniActionButton(
              actionsX + gap * 4 + MiniActionButton.SIZE * 3,
              y + 2,
              Text.translatable(LootLockLang.BUTTON_MINI_DELETE),
              true,
              () -> deleteProfile(profile));
      deleteButton.active = ProfileUiController.canDelete(profiles);

      dropdownWidgets.add(rowMain);
      dropdownWidgets.add(renameButton);
      dropdownWidgets.add(duplicateButton);
      dropdownWidgets.add(exportButton);
      dropdownWidgets.add(deleteButton);
      y += rowHeight + 1;
    }
    newProfileButton =
        ButtonWidget.builder(
                Text.translatable(LootLockLang.DROPDOWN_NEW_PROFILE).formatted(Formatting.GREEN),
                b -> createProfile())
            .dimensions(dropdownAnchorX, y + 3, dropdownAnchorWidth, 16)
            .build();
    newProfileButton.active = canCreate;
    if (!canCreate) {
      newProfileButton.setTooltip(atCapacityTooltip);
    }
    dropdownWidgets.add(newProfileButton);

    int importButtonY = y + 3 + 16 + 3;
    ButtonWidget importButton =
        ButtonWidget.builder(
                Text.translatable(LootLockLang.DROPDOWN_IMPORT_PROFILE).formatted(Formatting.AQUA),
                b -> openImportModal())
            .dimensions(dropdownAnchorX, importButtonY, dropdownAnchorWidth, 16)
            .build();
    importButton.active = canCreate;
    if (!canCreate) {
      importButton.setTooltip(atCapacityTooltip);
    }
    dropdownWidgets.add(importButton);

    int frameTop = dropdownAnchorY - DROPDOWN_FRAME_PAD;
    int frameBottom = importButtonY + 16 + DROPDOWN_FRAME_PAD;
    dropdownFrameX = dropdownAnchorX - DROPDOWN_FRAME_PAD;
    dropdownFrameY = frameTop;
    dropdownFrameW = dropdownAnchorWidth + DROPDOWN_FRAME_PAD * 2;
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
    rulesView.setOverlayHidden(open && dropdownOpen, dropdownFrameY + dropdownFrameH);
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
      openDeleteConfirmScreen();
      return;
    }
    mutateActive(draft -> draft.setRejectedItemAction(action));
  }

  private void openDeleteConfirmScreen() {
    MinecraftClient client = MinecraftClient.getInstance();
    Screen current = client == null ? null : client.currentScreen;
    if (client == null || current == null) {
      return;
    }
    client.setScreen(
        new net.minecraft.client.gui.screen.ConfirmScreen(
            confirmed -> {
              if (confirmed) {
                mutateActive(draft -> draft.setRejectedItemAction(RejectedItemAction.DELETE));
              }
              client.setScreen(current);
            },
            Text.translatable(LootLockLang.CONFIRM_ENABLE_DELETE_TITLE),
            Text.translatable(LootLockLang.CONFIRM_ENABLE_DELETE_BODY)));
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

  public boolean isInlineRenameActive() {
    return renamingProfileId != null && renameField != null && renameField.visible;
  }

  public boolean handleInlineRenameKey(int keyCode, int scanCode, int modifiers) {
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

  public boolean handleInlineRenameChar(char chr, int modifiers) {
    if (!isInlineRenameActive()) {
      return false;
    }
    return renameField.charTyped(chr, modifiers);
  }

  private void exportProfile(LootLockProfile profile) {
    if (profile == null) {
      return;
    }
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
    closeDropdown();
    client.setScreen(new ProfileImportScreen(current));
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
  }

  private void closeDropdown() {
    if (isInlineRenameActive()) {
      cancelInlineRename();
    }
    dropdownOpen = false;
    for (ClickableWidget widget : dropdownWidgets) {
      widget.visible = false;
    }
    rulesView.setOverlayHidden(false, 0);
  }
}
