package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.client.config.ClientSettings;
import com.grahambartley.lootlock.client.config.ClientSettingsManager;
import com.grahambartley.lootlock.client.keybind.LootLockKeybinds;
import com.grahambartley.lootlock.network.ClientMutationSync;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

/**
 * Settings tab content rendered inside the docked Loot Lock panel. Hosts five sections that mirror
 * {@code ux_redesign_2/Loot Lock.html}'s {@code renderSettingsView}:
 *
 * <ul>
 *   <li>NOTIFICATIONS: three toggles backed by {@link ClientSettings}.
 *   <li>SAFETY: confirm-before-delete toggle.
 *   <li>SERVER POLICY: allow-delete-rejected-items switch, read-only for non-operators on dedicated
 *       servers.
 *   <li>CONTROLS: read-only Toggle Loot Lock and Cycle Loot Profile keybind summary.
 *   <li>ABOUT: per-player storage + operator command blurb.
 * </ul>
 *
 * <p>The content scrolls vertically when it overflows the panel's content well; out-of-view widgets
 * are parked off-screen so stale hover/click cannot reach them.
 */
public final class SettingsTabView {
  private static final int SECTION_HEADER_HEIGHT = 12;
  private static final int SECTION_HEADER_TOP_PADDING = 10;
  private static final int SECTION_HEADER_BOTTOM_PADDING = 4;
  private static final int KEYBIND_ROW_HEIGHT = 14;
  private static final int ROW_DIVIDER_HEIGHT = 1;
  private static final int SWITCH_WIDTH = 42;
  private static final int SWITCH_HEIGHT = 16;
  private static final int KBD_HEIGHT = 10;
  private static final int KBD_PADDING_X = 4;
  private static final int LABEL_GAP = 6;
  private static final int NOTE_PADDING = 2;
  private static final int LINE_HEIGHT = 9;
  private static final int TOGGLE_ROW_TOP_PADDING = 4;
  private static final int TOGGLE_ROW_NAME_GAP = 2;
  private static final int TOGGLE_ROW_BOTTOM_PADDING = 4;

  /** Operator permission level required to see and use the SERVER POLICY OPERATOR section. */
  static final int OPERATOR_PERMISSION_LEVEL = 2;

  static final String IN_WORLD_ABOUT_BODY = LootLockLang.SETTINGS_ABOUT_IN_WORLD;

  static final String CLIENT_PREFS_ABOUT_BODY = LootLockLang.SETTINGS_ABOUT_CLIENT_PREFS;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final List<Row> rows = new ArrayList<>();

  private LootLockInventoryPanel panel;
  private boolean visible;
  private int scrollOffset;
  private boolean showServerPolicy = true;

  private VanillaSwitch blockedHudSwitch;
  private VanillaSwitch profileCycleToastSwitch;
  private VanillaSwitch toggleToastSwitch;
  private VanillaSwitch confirmBeforeDeleteSwitch;
  private VanillaSwitch policySwitch;

  /**
   * Hides the SERVER POLICY section and skips constructing its switch. Used by the Mod Menu client
   * prefs screen, where the per-world server policy has no current world to target. Must be set
   * before {@link #attach(LootLockInventoryPanel, Consumer)} since attach is what builds the
   * widgets.
   */
  public void setShowServerPolicy(boolean showServerPolicy) {
    this.showServerPolicy = showServerPolicy;
  }

  public void attach(LootLockInventoryPanel panel, Consumer<ClickableWidget> addDrawableChild) {
    this.panel = panel;
    widgets.clear();
    rows.clear();

    blockedHudSwitch =
        notificationSwitch(
            () -> settingsCopy().isShowBlockedHudNotification(),
            () -> toggleBlockedHud(LootLockClient.getClientSettingsManager()));
    profileCycleToastSwitch =
        notificationSwitch(
            () -> settingsCopy().isEnableProfileCycleToast(),
            () -> toggleProfileCycleToast(LootLockClient.getClientSettingsManager()));
    toggleToastSwitch =
        notificationSwitch(
            () -> settingsCopy().isEnableToggleToast(),
            () -> toggleToggleToast(LootLockClient.getClientSettingsManager()));
    confirmBeforeDeleteSwitch =
        notificationSwitch(
            () -> settingsCopy().isConfirmBeforeEnablingDelete(),
            () -> toggleConfirmBeforeDelete(LootLockClient.getClientSettingsManager()));

    addDrawableChild.accept(blockedHudSwitch);
    addDrawableChild.accept(profileCycleToastSwitch);
    addDrawableChild.accept(toggleToastSwitch);
    addDrawableChild.accept(confirmBeforeDeleteSwitch);
    widgets.add(blockedHudSwitch);
    widgets.add(profileCycleToastSwitch);
    widgets.add(toggleToastSwitch);
    widgets.add(confirmBeforeDeleteSwitch);

    if (showServerPolicy) {
      policySwitch =
          new VanillaSwitch(
              0,
              0,
              SWITCH_WIDTH,
              SWITCH_HEIGHT,
              () -> LootLockClient.getState().isAllowDeleteRejectedItems(),
              this::togglePolicy,
              false,
              false);
      addDrawableChild.accept(policySwitch);
      widgets.add(policySwitch);
    }

    setVisible(false);
    rebuildRows();
  }

  private VanillaSwitch notificationSwitch(BooleanSupplier state, Runnable onToggle) {
    return new VanillaSwitch(0, 0, SWITCH_WIDTH, SWITCH_HEIGHT, state, onToggle, false, false);
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
    for (ClickableWidget widget : widgets) {
      widget.visible = visible;
    }
    if (!visible) {
      scrollOffset = 0;
    } else {
      rebuildRows();
    }
  }

  /** Re-derives row positions when the panel content area moves. */
  public void relayout() {
    if (visible) {
      rebuildRows();
    }
  }

  /** Forwards a mouse-wheel event to the settings list, scrolling when inside the content well. */
  public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
    if (!visible || panel == null) {
      return false;
    }
    int viewX = panel.getContentInsetX();
    int viewY = panel.getContentInsetY();
    int viewWidth = panel.getContentInsetWidth();
    int viewHeight = panel.getContentInsetHeight();
    if (mouseX < viewX
        || mouseX > viewX + viewWidth
        || mouseY < viewY
        || mouseY > viewY + viewHeight) {
      return false;
    }
    int maxScroll = Math.max(0, totalContentHeight() - viewHeight);
    if (maxScroll == 0 || amount == 0) {
      return false;
    }
    int step = 12;
    int newOffset = scrollOffset - (int) Math.signum(amount) * step;
    if (newOffset < 0) {
      newOffset = 0;
    }
    if (newOffset > maxScroll) {
      newOffset = maxScroll;
    }
    if (newOffset == scrollOffset) {
      return false;
    }
    scrollOffset = newOffset;
    rebuildRows();
    return true;
  }

  private int totalContentHeight() {
    int total = 0;
    for (Row row : rows) {
      total += row.height;
    }
    return total;
  }

  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!visible || panel == null) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    int viewX = panel.getContentInsetX();
    int viewY = panel.getContentInsetY();
    int viewWidth = panel.getContentInsetWidth();
    int viewHeight = panel.getContentInsetHeight();

    context.enableScissor(viewX, viewY, viewX + viewWidth, viewY + viewHeight);
    for (Row row : rows) {
      if (row.y + row.height < viewY || row.y > viewY + viewHeight) {
        continue;
      }
      row.paint.paint(context, client, row.y, viewX, viewWidth);
    }
    context.disableScissor();
  }

  private void rebuildRows() {
    rows.clear();
    if (panel == null) {
      return;
    }
    int viewX = panel.getContentInsetX();
    int viewY = panel.getContentInsetY();
    int viewWidth = panel.getContentInsetWidth();

    int cursorY = viewY - scrollOffset;

    cursorY = addSectionHeader(cursorY, LootLockLang.SETTINGS_SECTION_NOTIFICATIONS);
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            LootLockLang.SETTINGS_BLOCKED_TOAST_NAME,
            LootLockLang.SETTINGS_BLOCKED_TOAST_DESC,
            blockedHudSwitch);
    cursorY = addDivider(cursorY);
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            LootLockLang.SETTINGS_PROFILE_SWITCH_TOAST_NAME,
            LootLockLang.SETTINGS_PROFILE_SWITCH_TOAST_DESC,
            profileCycleToastSwitch);
    cursorY = addDivider(cursorY);
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            LootLockLang.SETTINGS_TOGGLE_TOAST_NAME,
            LootLockLang.SETTINGS_TOGGLE_TOAST_DESC,
            toggleToastSwitch);

    cursorY = addSectionHeader(cursorY, LootLockLang.SETTINGS_SECTION_SAFETY);
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            LootLockLang.SETTINGS_CONFIRM_DELETE_NAME,
            LootLockLang.SETTINGS_CONFIRM_DELETE_DESC,
            confirmBeforeDeleteSwitch);

    if (showServerPolicy) {
      policySwitch.setReadOnly(isPolicySwitchReadOnly(MinecraftClient.getInstance()));
      cursorY = addSectionHeader(cursorY, LootLockLang.SETTINGS_SECTION_SERVER_POLICY);
      cursorY =
          addToggleRow(
              cursorY,
              viewX,
              viewWidth,
              LootLockLang.SETTINGS_ALLOW_DELETE_NAME,
              LootLockLang.SETTINGS_ALLOW_DELETE_DESC,
              policySwitch);
    }

    cursorY = addSectionHeader(cursorY, LootLockLang.SETTINGS_SECTION_CONTROLS);
    cursorY =
        addKeybindRow(
            cursorY,
            viewX,
            viewWidth,
            LootLockLang.SETTINGS_CONTROLS_TOGGLE_ENABLED,
            LootLockKeybinds.getToggleEnabled());
    cursorY = addDivider(cursorY);
    cursorY =
        addKeybindRow(
            cursorY,
            viewX,
            viewWidth,
            LootLockLang.SETTINGS_CONTROLS_CYCLE_PROFILE,
            LootLockKeybinds.getCycleProfile());

    cursorY = addSectionHeader(cursorY, LootLockLang.SETTINGS_SECTION_ABOUT);
    addAboutRow(cursorY, viewX, viewWidth);
  }

  private int addSectionHeader(int cursorY, String labelKey) {
    int top = cursorY + SECTION_HEADER_TOP_PADDING;
    int totalH = SECTION_HEADER_TOP_PADDING + SECTION_HEADER_HEIGHT + SECTION_HEADER_BOTTOM_PADDING;
    rows.add(
        new Row(
            top,
            totalH,
            (context, client, rowY, viewX, viewWidth) ->
                context.drawText(
                    client.textRenderer,
                    Text.translatable(labelKey),
                    viewX,
                    rowY + SECTION_HEADER_TOP_PADDING,
                    Palette.GOLD,
                    false)));
    return cursorY + totalH;
  }

  private int addToggleRow(
      int cursorY,
      int viewX,
      int viewWidth,
      String nameKey,
      String descKey,
      VanillaSwitch switchWidget) {
    int textWidth = viewWidth - SWITCH_WIDTH - LABEL_GAP;
    Text desc = Text.translatable(descKey);
    int descLines = wrappedLineCount(desc, textWidth);
    int height =
        TOGGLE_ROW_TOP_PADDING
            + LINE_HEIGHT
            + TOGGLE_ROW_NAME_GAP
            + descLines * LINE_HEIGHT
            + TOGGLE_ROW_BOTTOM_PADDING;
    int rowY = cursorY;
    int switchX = viewX + viewWidth - SWITCH_WIDTH;
    int switchY = cursorY + (height - SWITCH_HEIGHT) / 2;
    switchWidget.setPosition(switchX, switchY);
    applyWidgetClipping(switchWidget, switchY);

    rows.add(
        new Row(
            rowY,
            height,
            (context, client, y, vx, vw) -> {
              context.drawText(
                  client.textRenderer,
                  Text.translatable(nameKey),
                  vx,
                  y + TOGGLE_ROW_TOP_PADDING,
                  Palette.ON_WELL,
                  false);
              context.drawTextWrapped(
                  client.textRenderer,
                  desc,
                  vx,
                  y + TOGGLE_ROW_TOP_PADDING + LINE_HEIGHT + TOGGLE_ROW_NAME_GAP,
                  textWidth,
                  Palette.ON_WELL_DIM);
            }));
    return cursorY + height;
  }

  /** Hides the widget when it scrolls outside the panel's content well so it stops painting. */
  private void applyWidgetClipping(ClickableWidget widget, int widgetY) {
    if (!visible || panel == null) {
      widget.visible = false;
      return;
    }
    int viewTop = panel.getContentInsetY();
    int viewBottom = viewTop + panel.getContentInsetHeight();
    int widgetBottom = widgetY + widget.getHeight();
    boolean fullyVisible = widgetY >= viewTop && widgetBottom <= viewBottom;
    widget.visible = fullyVisible;
  }

  private int wrappedLineCount(Text text, int maxWidth) {
    TextRenderer renderer =
        MinecraftClient.getInstance() == null ? null : MinecraftClient.getInstance().textRenderer;
    if (renderer == null || maxWidth <= 0) {
      return 1;
    }
    List<OrderedText> lines = renderer.wrapLines(text, maxWidth);
    return Math.max(1, lines.size());
  }

  private int addKeybindRow(
      int cursorY, int viewX, int viewWidth, String labelKey, KeyBinding binding) {
    Text keyLabel = keyLabel(binding);
    int height = KEYBIND_ROW_HEIGHT;
    rows.add(
        new Row(
            cursorY,
            height,
            (context, client, y, vx, vw) -> {
              context.drawText(
                  client.textRenderer,
                  Text.translatable(labelKey),
                  vx,
                  y + 3,
                  Palette.ON_WELL,
                  false);
              int kbdWidth = client.textRenderer.getWidth(keyLabel) + KBD_PADDING_X * 2;
              int kbdX = vx + vw - kbdWidth;
              int kbdY = y + 2;
              paintKbd(context, client, keyLabel, kbdX, kbdY, kbdWidth);
            }));
    return cursorY + height;
  }

  private int addDivider(int cursorY) {
    int height = ROW_DIVIDER_HEIGHT;
    rows.add(
        new Row(
            cursorY,
            height,
            (context, client, y, vx, vw) ->
                context.fill(vx, y, vx + vw, y + 1, Palette.ROW_DIVIDER)));
    return cursorY + height;
  }

  private void addAboutRow(int cursorY, int viewX, int viewWidth) {
    String bodyKey = aboutBody(showServerPolicy);
    Text body = Text.translatable(bodyKey);
    int lines = wrappedLineCount(body, viewWidth);
    int height = NOTE_PADDING * 2 + lines * LINE_HEIGHT;
    rows.add(
        new Row(
            cursorY,
            height,
            (context, client, y, vx, vw) ->
                context.drawTextWrapped(
                    client.textRenderer, body, vx, y + NOTE_PADDING, vw, Palette.ON_WELL_DIM)));
  }

  private static void paintKbd(
      DrawContext context, MinecraftClient client, Text text, int x, int y, int width) {
    context.fill(x, y, x + width, y + KBD_HEIGHT, Palette.KBD_FILL);
    context.drawText(client.textRenderer, text, x + KBD_PADDING_X, y + 1, Palette.ON_WELL, false);
  }

  static Text keyLabel(KeyBinding binding) {
    if (binding == null || binding.isUnbound()) {
      return Text.translatable(LootLockLang.SETTINGS_CONTROLS_UNBOUND);
    }
    return binding.getBoundKeyLocalizedText();
  }

  /** True when the local player has permission level >= 2. */
  public static boolean isOperator(MinecraftClient client) {
    if (client == null || client.player == null) {
      return false;
    }
    return client.player.hasPermissionLevel(OPERATOR_PERMISSION_LEVEL);
  }

  /**
   * The SERVER POLICY switch is read-only when the client is connected to a dedicated server and
   * the local player is not an operator. On integrated singleplayer or as an operator on a
   * dedicated server, the switch is interactive.
   */
  public static boolean isPolicySwitchReadOnly(MinecraftClient client) {
    if (client == null) {
      return true;
    }
    return isPolicySwitchReadOnly(client.isIntegratedServerRunning(), isOperator(client));
  }

  /** Pure decision used by {@link #isPolicySwitchReadOnly(MinecraftClient)} and unit tests. */
  static boolean isPolicySwitchReadOnly(boolean integratedServer, boolean operator) {
    if (integratedServer) {
      return false;
    }
    return !operator;
  }

  /**
   * Returns the ordered list of section labels that {@link #rebuildRows()} will render given the
   * current visibility flag. Pure helper used by unit tests so the section presence decision is
   * verifiable without standing up the full panel. Keep this in sync with {@link #rebuildRows()}:
   * the {@code addSectionHeader(...)} calls there are the source of truth for what actually paints.
   */
  static List<String> sectionLabels(boolean showServerPolicy) {
    if (showServerPolicy) {
      return List.of(
          LootLockLang.SETTINGS_SECTION_NOTIFICATIONS,
          LootLockLang.SETTINGS_SECTION_SAFETY,
          LootLockLang.SETTINGS_SECTION_SERVER_POLICY,
          LootLockLang.SETTINGS_SECTION_CONTROLS,
          LootLockLang.SETTINGS_SECTION_ABOUT);
    }
    return List.of(
        LootLockLang.SETTINGS_SECTION_NOTIFICATIONS,
        LootLockLang.SETTINGS_SECTION_SAFETY,
        LootLockLang.SETTINGS_SECTION_CONTROLS,
        LootLockLang.SETTINGS_SECTION_ABOUT);
  }

  /** Returns the ABOUT section body text appropriate for the current visibility mode. */
  static String aboutBody(boolean showServerPolicy) {
    return showServerPolicy ? IN_WORLD_ABOUT_BODY : CLIENT_PREFS_ABOUT_BODY;
  }

  VanillaSwitch policySwitchForTest() {
    return policySwitch;
  }

  private static ClientSettings settingsCopy() {
    ClientSettingsManager manager = LootLockClient.getClientSettingsManager();
    return manager == null ? ClientSettings.defaults() : manager.getSettingsCopy();
  }

  private static void mutateSettings(
      ClientSettingsManager manager, Consumer<ClientSettings> mutator) {
    if (manager == null) {
      return;
    }
    ClientSettings copy = manager.getSettingsCopy();
    mutator.accept(copy);
    manager.replaceAndSave(copy);
  }

  /** Flips the blocked-item toast setting and persists. Wired to the NOTIFICATIONS row switch. */
  static void toggleBlockedHud(ClientSettingsManager manager) {
    mutateSettings(
        manager, s -> s.setShowBlockedHudNotification(!s.isShowBlockedHudNotification()));
  }

  /** Flips the profile-switch toast setting and persists. Wired to the NOTIFICATIONS row switch. */
  static void toggleProfileCycleToast(ClientSettingsManager manager) {
    mutateSettings(manager, s -> s.setEnableProfileCycleToast(!s.isEnableProfileCycleToast()));
  }

  /**
   * Flips the Loot Lock toggle toast setting and persists. Wired to the NOTIFICATIONS row switch.
   */
  static void toggleToggleToast(ClientSettingsManager manager) {
    mutateSettings(manager, s -> s.setEnableToggleToast(!s.isEnableToggleToast()));
  }

  /** Flips the confirm-before-delete setting and persists. Wired to the SAFETY row switch. */
  static void toggleConfirmBeforeDelete(ClientSettingsManager manager) {
    mutateSettings(
        manager, s -> s.setConfirmBeforeEnablingDelete(!s.isConfirmBeforeEnablingDelete()));
  }

  private void togglePolicy() {
    boolean next = !LootLockClient.getState().isAllowDeleteRejectedItems();
    ClientMutationSync.sendServerPolicyUpdateRequest(next);
  }

  // Test-only accessors -----------------------------------------------------
  List<Row> rowsForTest() {
    return rows;
  }

  @FunctionalInterface
  interface PaintFn {
    void paint(DrawContext context, MinecraftClient client, int rowY, int viewX, int viewWidth);
  }

  static final class Row {
    final int y;
    final int height;
    final PaintFn paint;

    Row(int y, int height, PaintFn paint) {
      this.y = y;
      this.height = height;
      this.paint = paint;
    }
  }
}
