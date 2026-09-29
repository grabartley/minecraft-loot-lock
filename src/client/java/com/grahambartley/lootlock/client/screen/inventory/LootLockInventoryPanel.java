package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
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

  static final int FLASH_START_COLOR = 0xFF9FD08F;

  static final long FLASH_DURATION_MILLIS = 500L;

  private static final int ARMED_BORDER_THICKNESS = 3;

  static LongSupplier clockMillis = System::currentTimeMillis;

  private static final int SIDE_PADDING = 8;
  private static final int HEADER_HEIGHT = 26;
  private static final int CONTROLS_PAD = 5;
  private static final int SUMMARY_HEIGHT = 38;
  private static final int SUMMARY_ACCENT_WIDTH = 3;
  private static final int CONTENT_PADDING = 6;
  private static final int SECTION_GAP = 6;
  private static final Identifier ICON_TEXTURE = LootLockIconButton.ICON_TEXTURE;

  private final List<ClickableWidget> allWidgets = new ArrayList<>();
  private final List<ClickableWidget> lockableWidgets = new ArrayList<>();

  private final RulesTabView rulesView = new RulesTabView();
  private final SettingsTabView settingsView = new SettingsTabView();
  private final ProfileDropdown dropdown =
      new ProfileDropdown(() -> this.open, this::onDropdownOpenChanged);
  private PanelTab activeTab = PanelTab.RULES;

  private int panelX;
  private int panelY;
  private int currentHeight = HEIGHT;

  private boolean fitsOnScreen = true;

  private boolean open;
  private boolean dropArmed;
  private long flashStartMillis = -1L;

  private boolean clientPrefsMode;
  private PanelControls controls;

  private int headerY;
  private int profileWellY;
  private int profileWellH;
  private int modeY;
  private int actionY;
  private int controlsWellY;
  private int controlsWellH;
  private int summaryY;
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
      dropdown.closeSilently();
    } else if (PENDING_DROPDOWN_REOPEN && !clientPrefsMode) {
      PENDING_DROPDOWN_REOPEN = false;
      dropdown.open();
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
    this.panelX = panelX;
    this.panelY = panelY;
    allWidgets.clear();
    lockableWidgets.clear();
    Consumer<ClickableWidget> addUnlocked =
        widget -> {
          addDrawableChild.accept(widget);
          allWidgets.add(widget);
        };
    Consumer<ClickableWidget> addLockable =
        widget -> {
          addUnlocked.accept(widget);
          lockableWidgets.add(widget);
        };

    if (!clientPrefsMode) {
      controls =
          new PanelControls(
              this::handleClientToggle,
              dropdown::toggle,
              () -> activeTab,
              this::setTab,
              addUnlocked,
              addLockable);
      TextFieldWidget renameField = dropdown.attach(host, controls.profilePill());
      applyLayout();
      rulesView.attach(this, addLockable);
      settingsView.setShowServerPolicy(true);
      settingsView.attach(this, addLockable);
      addUnlocked.accept(renameField);
    } else {
      applyLayout();
      settingsView.setShowServerPolicy(false);
      settingsView.attach(this, addLockable);
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
    cursorY += HEADER_HEIGHT + SECTION_GAP;

    if (!clientPrefsMode) {
      controls.placeHeader(
          innerRight,
          headerY + (HEADER_HEIGHT - PanelControls.SWITCH_HEIGHT) / 2,
          isIntegratedSingleplayer());

      profileWellY = cursorY;
      profileWellH = PanelControls.PROFILE_ROW_HEIGHT + 8;
      controls.placeProfileRow(innerLeft, innerWidth, cursorY + 4);
      cursorY = profileWellY + profileWellH + SECTION_GAP;

      controlsWellY = cursorY;
      controlsWellH = CONTROLS_PAD * 2 + PanelControls.CONTROL_ROW_HEIGHT * 2 + 2;
      modeY = controlsWellY + CONTROLS_PAD;
      actionY = modeY + PanelControls.CONTROL_ROW_HEIGHT + 2;
      controls.placeSegments(innerLeft, innerRight, modeY, actionY);
      cursorY = controlsWellY + controlsWellH + SECTION_GAP;

      summaryY = cursorY;
      cursorY += SUMMARY_HEIGHT + SECTION_GAP;

      controls.placeTabs(innerLeft, innerWidth, cursorY);
      cursorY += PanelControls.TAB_HEIGHT;
    }

    contentY = cursorY;
    contentHeight =
        Math.max(CONTENT_PADDING * 2 + 20, (panelY + currentHeight - SIDE_PADDING) - cursorY);

    if (!clientPrefsMode) {
      dropdown.setAnchor(
          innerLeft + ProfileDropdown.FRAME_PAD,
          profileWellY + profileWellH + ProfileDropdown.FRAME_PAD,
          innerWidth - ProfileDropdown.FRAME_PAD * 2);
      rulesView.relayout();
    }
    settingsView.relayout();
  }

  public void paintChrome(DrawContext context) {
    if (!open || !fitsOnScreen) {
      return;
    }
    Chrome.panel(context, panelX, panelY, WIDTH, currentHeight);
    int wellX = panelX + SIDE_PADDING;
    int wellWidth = WIDTH - SIDE_PADDING * 2;
    if (!clientPrefsMode) {
      Chrome.inset(context, wellX, profileWellY, wellWidth, profileWellH);
      Chrome.inset(context, wellX, controlsWellY, wellWidth, controlsWellH);
      Chrome.inset(context, wellX, summaryY, wellWidth, SUMMARY_HEIGHT);
      context.fill(
          wellX + 1,
          summaryY + 1,
          wellX + 1 + SUMMARY_ACCENT_WIDTH,
          summaryY + SUMMARY_HEIGHT - 1,
          summaryAccent(
              ActiveProfileActions.globallyEnabled(),
              ActiveProfileActions.activeProfile().orElse(null)));
    }
    Chrome.inset(context, wellX, contentY, wellWidth, contentHeight);
    if (!clientPrefsMode && activeTab == PanelTab.RULES) {
      paintRulesWellOverlays(context, wellX, wellWidth);
    }
  }

  static int summaryAccent(boolean globallyEnabled, LootLockProfile activeProfile) {
    if (!globallyEnabled) {
      return Palette.LEAVE;
    }
    if (activeProfile == null) {
      return Palette.SLOT_LO;
    }
    return activeProfile.getMode() == FilterMode.ALLOWLIST ? Palette.ALLOW : Palette.DENY;
  }

  private void paintRulesWellOverlays(DrawContext context, int wellX, int wellWidth) {
    int wellX2 = wellX + wellWidth;
    int wellY2 = contentY + contentHeight;
    if (isFlashActive()) {
      int blended = blendArgb(FLASH_START_COLOR, Palette.SLOT, flashProgress());
      context.fill(wellX + 1, contentY + 1, wellX2 - 1, wellY2 - 1, blended);
    }
    if (dropArmed) {
      int t = ARMED_BORDER_THICKNESS;
      context.fill(wellX, contentY, wellX2, contentY + t, Palette.SLOT_HI);
      context.fill(wellX, wellY2 - t, wellX2, wellY2, Palette.SLOT_HI);
      context.fill(wellX, contentY + t, wellX + t, wellY2 - t, Palette.SLOT_HI);
      context.fill(wellX2 - t, contentY + t, wellX2, wellY2 - t, Palette.SLOT_HI);
    }
  }

  static int blendArgb(int from, int to, float t) {
    if (t <= 0f) {
      return from;
    }
    if (t >= 1f) {
      return to;
    }
    int a = blendChannel(from >>> 24, to >>> 24, t);
    int r = blendChannel(from >> 16, to >> 16, t);
    int g = blendChannel(from >> 8, to >> 8, t);
    int b = blendChannel(from, to, t);
    return (a << 24) | (r << 16) | (g << 8) | b;
  }

  private static int blendChannel(int from, int to, float t) {
    int f = from & 0xFF;
    int d = to & 0xFF;
    return (int) (f + (d - f) * t);
  }

  public void paintForeground(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!open || !fitsOnScreen) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();

    int iconSize = 22;
    int iconX = panelX + SIDE_PADDING + 1;
    int iconY = headerY + (HEADER_HEIGHT - iconSize) / 2;
    int headerTextY = headerY + (HEADER_HEIGHT - 8) / 2;
    context.drawTexture(ICON_TEXTURE, iconX, iconY, 0f, 0f, iconSize, iconSize, iconSize, iconSize);
    context.drawText(
        client.textRenderer,
        Text.translatable(LootLockLang.BRAND),
        iconX + iconSize + 4,
        headerTextY,
        Palette.TITLE,
        false);

    if (clientPrefsMode) {
      settingsView.render(context, mouseX, mouseY, delta);
      return;
    }

    controls.paintLabels(context, headerTextY, panelX + SIDE_PADDING + 4, modeY, actionY);

    context.drawTextWrapped(
        client.textRenderer,
        LootLockSummaryText.build(
            ActiveProfileActions.globallyEnabled(),
            ActiveProfileActions.activeProfile().orElse(null)),
        panelX + SIDE_PADDING + SUMMARY_ACCENT_WIDTH + 6,
        summaryY + 5,
        WIDTH - SIDE_PADDING * 2 - SUMMARY_ACCENT_WIDTH - 10,
        Palette.INK);

    if (activeTab == PanelTab.RULES) {
      rulesView.render(context, mouseX, mouseY, delta);
    } else {
      settingsView.render(context, mouseX, mouseY, delta);
    }

    StatusEffectStrip.paint(context, panelX, panelY, WIDTH, mouseX, mouseY);

    dropdown.render(context, mouseX, mouseY, delta);
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
    if (clientPrefsMode || controls == null) {
      return;
    }
    boolean globallyEnabled = ActiveProfileActions.globallyEnabled();
    applyLock(globallyEnabled);
    controls.refresh(
        open, globallyEnabled, panelX + WIDTH - SIDE_PADDING, isIntegratedSingleplayer());
    rulesView.refresh();
    dropdown.rebuildIfStale();
  }

  static boolean isIntegratedSingleplayer() {
    MinecraftClient client = MinecraftClient.getInstance();
    return client != null && client.isIntegratedServerRunning();
  }

  public void handleClientToggle() {
    GlobalEnableController.toggle(MinecraftClient.getInstance());
  }

  private void applyVisibility() {
    for (ClickableWidget widget : allWidgets) {
      widget.visible = dropdown.isRenameField(widget) ? open && dropdown.isRenaming() : open;
    }
    rulesView.setVisible(!clientPrefsMode && open && activeTab == PanelTab.RULES);
    dropdown.applyVisibility();
    settingsView.setVisible(open && activeTab == PanelTab.SETTINGS);
  }

  private void onDropdownOpenChanged() {
    rulesView.setOverlayHidden(open && dropdown.isOpen(), dropdown.frameBottom());
  }

  private void applyLock(boolean enabled) {
    for (ClickableWidget widget : lockableWidgets) {
      widget.active = open && enabled;
    }
  }

  public boolean handleDropdownMouseClick(double mouseX, double mouseY, int button) {
    return dropdown.handleMouseClick(mouseX, mouseY, button);
  }

  public boolean isInlineRenameActive() {
    return dropdown.isInlineRenameActive();
  }

  public boolean handleInlineRenameKey(int keyCode, int scanCode, int modifiers) {
    return dropdown.handleInlineRenameKey(keyCode, scanCode, modifiers);
  }

  public boolean handleInlineRenameChar(char chr, int modifiers) {
    return dropdown.handleInlineRenameChar(chr, modifiers);
  }
}
