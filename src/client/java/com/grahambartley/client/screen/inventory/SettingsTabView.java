package com.grahambartley.client.screen.inventory;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.config.ClientSettings;
import com.grahambartley.client.config.ClientSettingsManager;
import com.grahambartley.client.keybind.LootLockKeybinds;
import com.grahambartley.network.ClientMutationSync;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.text.Text;

/**
 * Settings tab content rendered inside the docked Loot Lock panel. Hosts five sections that mirror
 * {@code ux_redesign_2/Loot Lock.html}'s {@code renderSettingsView}:
 *
 * <ul>
 *   <li>NOTIFICATIONS: three toggles backed by {@link ClientSettings}.
 *   <li>SAFETY: confirm-before-delete toggle.
 *   <li>SERVER POLICY OPERATOR: allow-delete-rejected-items switch, hidden for non-operators.
 *   <li>CONTROLS: read-only Toggle Loot Lock and Cycle Loot Profile keybind summary.
 *   <li>ABOUT: per-player storage + operator command blurb.
 * </ul>
 *
 * <p>The content scrolls vertically when it overflows the panel's content well; out-of-view widgets
 * are parked off-screen so stale hover/click cannot reach them.
 */
public final class SettingsTabView {
  private static final int SECTION_HEADER_HEIGHT = 12;
  private static final int SECTION_HEADER_TOP_PADDING = 6;
  private static final int SECTION_HEADER_BOTTOM_PADDING = 4;
  private static final int SETTING_ROW_HEIGHT = 38;
  private static final int KEYBIND_ROW_HEIGHT = 14;
  private static final int ABOUT_ROW_HEIGHT = 40;
  private static final int ROW_DIVIDER_HEIGHT = 1;
  private static final int SWITCH_WIDTH = 42;
  private static final int SWITCH_HEIGHT = 16;
  private static final int KBD_HEIGHT = 10;
  private static final int KBD_PADDING_X = 4;
  private static final int LABEL_GAP = 6;
  private static final int NOTE_PADDING = 2;

  /** Hard cap so a missing-permission player still gets a stable layout. */
  static final int OPERATOR_PERMISSION_LEVEL = 2;

  private final List<ClickableWidget> widgets = new ArrayList<>();
  private final List<Row> rows = new ArrayList<>();

  private LootLockInventoryPanel panel;
  private boolean visible;
  private int scrollOffset;

  private VanillaSwitch blockedHudSwitch;
  private VanillaSwitch profileCycleToastSwitch;
  private VanillaSwitch toggleToastSwitch;
  private VanillaSwitch confirmBeforeDeleteSwitch;
  private VanillaSwitch policySwitch;

  public void attach(LootLockInventoryPanel panel, Consumer<ClickableWidget> addDrawableChild) {
    this.panel = panel;
    widgets.clear();
    rows.clear();

    blockedHudSwitch =
        notificationSwitch(
            () -> settingsCopy().isShowBlockedHudNotification(),
            () ->
                mutateSettings(
                    s -> s.setShowBlockedHudNotification(!s.isShowBlockedHudNotification())));
    profileCycleToastSwitch =
        notificationSwitch(
            () -> settingsCopy().isEnableProfileCycleToast(),
            () ->
                mutateSettings(s -> s.setEnableProfileCycleToast(!s.isEnableProfileCycleToast())));
    toggleToastSwitch =
        notificationSwitch(
            () -> settingsCopy().isEnableToggleToast(),
            () -> mutateSettings(s -> s.setEnableToggleToast(!s.isEnableToggleToast())));
    confirmBeforeDeleteSwitch =
        notificationSwitch(
            () -> settingsCopy().isConfirmBeforeEnablingDelete(),
            () ->
                mutateSettings(
                    s -> s.setConfirmBeforeEnablingDelete(!s.isConfirmBeforeEnablingDelete())));
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

    addDrawableChild.accept(blockedHudSwitch);
    addDrawableChild.accept(profileCycleToastSwitch);
    addDrawableChild.accept(toggleToastSwitch);
    addDrawableChild.accept(confirmBeforeDeleteSwitch);
    addDrawableChild.accept(policySwitch);
    widgets.add(blockedHudSwitch);
    widgets.add(profileCycleToastSwitch);
    widgets.add(toggleToastSwitch);
    widgets.add(confirmBeforeDeleteSwitch);
    widgets.add(policySwitch);

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
    if (maxScroll == 0) {
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
    if (newOffset != scrollOffset) {
      scrollOffset = newOffset;
      rebuildRows();
    }
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
    boolean isOperator = isOperator(MinecraftClient.getInstance());

    cursorY = addSectionHeader(cursorY, "NOTIFICATIONS");
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            "Blocked-item toast",
            "Pop a small toast in-world when an item is filtered out.",
            blockedHudSwitch);
    cursorY = addDivider(cursorY);
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            "Profile-switch toast",
            "Confirm with a toast each time you change profile (also when cycling with P).",
            profileCycleToastSwitch);
    cursorY = addDivider(cursorY);
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            "Loot Lock toggle toast",
            "Pop a toast each time Loot Lock is turned on or off.",
            toggleToastSwitch);

    cursorY = addSectionHeader(cursorY, "SAFETY");
    cursorY =
        addToggleRow(
            cursorY,
            viewX,
            viewWidth,
            "Confirm before deleting",
            "Require a confirmation before turning on Delete mode, so loot is never destroyed by"
                + " accident.",
            confirmBeforeDeleteSwitch);

    if (isOperator) {
      cursorY = addSectionHeader(cursorY, "SERVER POLICY · OPERATOR");
      cursorY =
          addToggleRow(
              cursorY,
              viewX,
              viewWidth,
              "Allow delete mode",
              "Set for everyone with /lootlock policy allowDeleteRejectedItems. When off, Delete is"
                  + " blocked and every profile leaves rejected items on the ground.",
              policySwitch);
    } else {
      policySwitch.setPosition(-9999, -9999);
      policySwitch.visible = false;
    }

    cursorY = addSectionHeader(cursorY, "CONTROLS");
    cursorY =
        addKeybindRow(
            cursorY, viewX, viewWidth, "Toggle Loot Lock", LootLockKeybinds.getToggleEnabled());
    cursorY = addDivider(cursorY);
    cursorY =
        addKeybindRow(
            cursorY, viewX, viewWidth, "Cycle Loot Profile", LootLockKeybinds.getCycleProfile());

    cursorY = addSectionHeader(cursorY, "ABOUT");
    addAboutRow(cursorY, viewX, viewWidth);
  }

  private int addSectionHeader(int cursorY, String label) {
    int top = cursorY + SECTION_HEADER_TOP_PADDING;
    int totalH = SECTION_HEADER_TOP_PADDING + SECTION_HEADER_HEIGHT + SECTION_HEADER_BOTTOM_PADDING;
    rows.add(
        new Row(
            top,
            totalH,
            (context, client, rowY, viewX, viewWidth) ->
                context.drawText(
                    client.textRenderer,
                    Text.literal(label),
                    viewX,
                    rowY + SECTION_HEADER_TOP_PADDING,
                    Palette.GOLD,
                    false)));
    return cursorY + totalH;
  }

  private int addToggleRow(
      int cursorY, int viewX, int viewWidth, String name, String desc, VanillaSwitch switchWidget) {
    int height = SETTING_ROW_HEIGHT;
    int rowY = cursorY;
    int switchX = viewX + viewWidth - SWITCH_WIDTH;
    int switchY = cursorY + (height - SWITCH_HEIGHT) / 2;
    switchWidget.setPosition(switchX, switchY);
    switchWidget.visible = visible;

    int textWidth = viewWidth - SWITCH_WIDTH - LABEL_GAP;
    rows.add(
        new Row(
            rowY,
            height,
            (context, client, y, vx, vw) -> {
              context.drawText(
                  client.textRenderer, Text.literal(name), vx, y + 4, Palette.ON_WELL, false);
              context.drawTextWrapped(
                  client.textRenderer,
                  Text.literal(desc),
                  vx,
                  y + 4 + 10,
                  textWidth,
                  Palette.ON_WELL_DIM);
            }));
    return cursorY + height;
  }

  private int addKeybindRow(
      int cursorY, int viewX, int viewWidth, String label, KeyBinding binding) {
    String keyLabel = keyLabel(binding);
    int height = KEYBIND_ROW_HEIGHT;
    rows.add(
        new Row(
            cursorY,
            height,
            (context, client, y, vx, vw) -> {
              context.drawText(
                  client.textRenderer, Text.literal(label), vx, y + 3, Palette.ON_WELL, false);
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
            (context, client, y, vx, vw) -> context.fill(vx, y, vx + vw, y + 1, 0xFF1E1E22)));
    return cursorY + height;
  }

  private void addAboutRow(int cursorY, int viewX, int viewWidth) {
    int height = ABOUT_ROW_HEIGHT;
    String body =
        "Rules are stored per player and synced from the server. Operators can manage any"
            + " player's rules with /lootlock commands, even for vanilla clients.";
    rows.add(
        new Row(
            cursorY,
            height,
            (context, client, y, vx, vw) ->
                context.drawTextWrapped(
                    client.textRenderer,
                    Text.literal(body),
                    vx,
                    y + NOTE_PADDING,
                    vw,
                    Palette.ON_WELL_DIM)));
  }

  private static void paintKbd(
      DrawContext context, MinecraftClient client, String text, int x, int y, int width) {
    context.fill(x, y, x + width, y + KBD_HEIGHT, 0xFF3A3A42);
    context.drawText(
        client.textRenderer, Text.literal(text), x + KBD_PADDING_X, y + 1, Palette.ON_WELL, false);
  }

  /** Returns "Unbound" when the binding is unset, otherwise the key's localized name. */
  static String keyLabel(KeyBinding binding) {
    if (binding == null || binding.isUnbound()) {
      return "Unbound";
    }
    return binding.getBoundKeyLocalizedText().getString();
  }

  /** Visible only to operators (permission level >= 2). */
  public static boolean isOperator(MinecraftClient client) {
    if (client == null || client.player == null) {
      return false;
    }
    return client.player.hasPermissionLevel(OPERATOR_PERMISSION_LEVEL);
  }

  private static ClientSettings settingsCopy() {
    ClientSettingsManager manager = LootLockClient.getClientSettingsManager();
    return manager == null ? ClientSettings.defaults() : manager.getSettingsCopy();
  }

  private static void mutateSettings(Consumer<ClientSettings> mutator) {
    ClientSettingsManager manager = LootLockClient.getClientSettingsManager();
    if (manager == null) {
      return;
    }
    ClientSettings copy = manager.getSettingsCopy();
    mutator.accept(copy);
    manager.replaceAndSave(copy);
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
