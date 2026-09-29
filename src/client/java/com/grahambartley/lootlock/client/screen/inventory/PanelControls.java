package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.client.LootLockClient;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

final class PanelControls {
  static final int PROFILE_ROW_HEIGHT = 20;
  static final int CONTROL_ROW_HEIGHT = 18;
  static final int TAB_HEIGHT = 20;
  static final int SWITCH_WIDTH = 42;
  static final int SWITCH_HEIGHT = 16;
  private static final int SWITCH_LABEL_GAP = 36;
  private static final int CLIENT_SWITCH_OFFSET = SWITCH_WIDTH + 40;
  private static final int NAV_WIDTH = 14;
  private static final int NAV_GAP = 3;
  private static final int CTL_LABEL_WIDTH = 48;

  private final OnOffButton serverSwitch;
  private final OnOffButton clientSwitch;
  private final ButtonWidget prevProfileButton;
  private final ProfilePill profilePill;
  private final ButtonWidget nextProfileButton;
  private final SegmentedButton modeAllowButton;
  private final SegmentedButton modeDenyButton;
  private final SegmentedButton actionLeaveButton;
  private final SegmentedButton actionDeleteButton;
  private final PanelTabButton rulesTabButton;
  private final PanelTabButton settingsTabButton;
  private final List<ClickableWidget> lockable;

  PanelControls(
      Runnable onClientToggle,
      Runnable onProfilePillPress,
      Supplier<PanelTab> activeTab,
      Consumer<PanelTab> onTabPress,
      Consumer<ClickableWidget> addWidget) {
    serverSwitch =
        new OnOffButton(
            0,
            0,
            SWITCH_WIDTH,
            SWITCH_HEIGHT,
            () -> LootLockClient.getState().isServerSupportsLootLock(),
            null,
            true,
            true);
    clientSwitch =
        new OnOffButton(
            0,
            0,
            SWITCH_WIDTH,
            SWITCH_HEIGHT,
            ActiveProfileActions::globallyEnabled,
            onClientToggle,
            false,
            false);
    prevProfileButton = navButton("<", -1);
    profilePill =
        new ProfilePill(
            0,
            0,
            10,
            PROFILE_ROW_HEIGHT,
            () ->
                ActiveProfileActions.activeProfile()
                    .map(ProfileColors::colorForProfile)
                    .orElse(Palette.SLOT),
            () ->
                ActiveProfileActions.activeProfile()
                    .map(LootLockProfile::getName)
                    .orElseGet(
                        () -> Text.translatable(LootLockLang.PROFILE_PLACEHOLDER).getString()),
            () ->
                ActiveProfileActions.activeProfile().map(ProfileLabels::ruleCountLabel).orElse(""),
            onProfilePillPress);
    nextProfileButton = navButton(">", 1);
    modeAllowButton =
        segment(
            LootLockLang.MODE_ALLOWLIST,
            Palette.ALLOW,
            Palette.ALLOW_ON_PRESSED,
            () -> modeIs(FilterMode.ALLOWLIST),
            () -> ActiveProfileActions.setMode(FilterMode.ALLOWLIST));
    modeDenyButton =
        segment(
            LootLockLang.MODE_DENYLIST,
            Palette.DENY,
            Palette.DENY_ON_PRESSED,
            () -> modeIs(FilterMode.DENYLIST),
            () -> ActiveProfileActions.setMode(FilterMode.DENYLIST));
    actionLeaveButton =
        segment(
            LootLockLang.ACTION_LEAVE,
            Palette.LEAVE,
            Palette.LEAVE_ON_PRESSED,
            () -> actionIs(RejectedItemAction.LEAVE_ON_GROUND),
            () -> ActiveProfileActions.setAction(RejectedItemAction.LEAVE_ON_GROUND));
    actionDeleteButton =
        segment(
            LootLockLang.ACTION_DELETE,
            Palette.DENY,
            Palette.DENY_ON_PRESSED,
            () -> actionIs(RejectedItemAction.DELETE),
            () -> ActiveProfileActions.setAction(RejectedItemAction.DELETE));
    rulesTabButton = tabButton(LootLockLang.TAB_RULES, PanelTab.RULES, activeTab, onTabPress);
    settingsTabButton =
        tabButton(LootLockLang.TAB_SETTINGS, PanelTab.SETTINGS, activeTab, onTabPress);

    addWidget.accept(serverSwitch);
    addWidget.accept(clientSwitch);
    lockable =
        List.of(
            prevProfileButton,
            profilePill,
            nextProfileButton,
            modeAllowButton,
            modeDenyButton,
            actionLeaveButton,
            actionDeleteButton,
            rulesTabButton,
            settingsTabButton);
    lockable.forEach(addWidget);
  }

  private static ButtonWidget navButton(String glyph, int direction) {
    return ButtonWidget.builder(Text.literal(glyph), b -> ActiveProfileActions.cycle(direction))
        .dimensions(0, 0, NAV_WIDTH, PROFILE_ROW_HEIGHT)
        .build();
  }

  private static SegmentedButton segment(
      String labelKey,
      int accentColor,
      int selectedLabelColor,
      BooleanSupplier selected,
      Runnable onPress) {
    return new SegmentedButton(
        0,
        0,
        10,
        CONTROL_ROW_HEIGHT,
        Text.translatable(labelKey),
        accentColor,
        selectedLabelColor,
        selected,
        onPress);
  }

  private static PanelTabButton tabButton(
      String labelKey, PanelTab tab, Supplier<PanelTab> activeTab, Consumer<PanelTab> onPress) {
    return new PanelTabButton(
        0,
        0,
        10,
        TAB_HEIGHT,
        Text.translatable(labelKey),
        () -> activeTab.get() == tab,
        () -> onPress.accept(tab));
  }

  private static boolean modeIs(FilterMode mode) {
    return ActiveProfileActions.activeProfile().map(p -> p.getMode() == mode).orElse(false);
  }

  private static boolean actionIs(RejectedItemAction action) {
    return ActiveProfileActions.activeProfile()
        .map(p -> p.getRejectedItemAction() == action)
        .orElse(false);
  }

  ClickableWidget profilePill() {
    return profilePill;
  }

  void placeHeader(int innerRight, int switchY, boolean integrated) {
    serverSwitch.setPosition(innerRight - SWITCH_WIDTH, switchY);
    clientSwitch.setPosition(clientSwitchX(innerRight, integrated), switchY);
  }

  void placeProfileRow(int innerLeft, int innerWidth, int y) {
    int pillX = innerLeft + NAV_WIDTH + NAV_GAP;
    int pillWidth = innerWidth - NAV_WIDTH * 2 - NAV_GAP * 2;
    prevProfileButton.setPosition(innerLeft, y);
    profilePill.setPosition(pillX, y);
    profilePill.setWidth(pillWidth);
    nextProfileButton.setPosition(pillX + pillWidth + NAV_GAP, y);
  }

  void placeSegments(int innerLeft, int innerRight, int modeY, int actionY) {
    int segLeft = innerLeft + CTL_LABEL_WIDTH;
    int segWidth = (innerRight - segLeft) / 2;
    placePair(modeAllowButton, modeDenyButton, segLeft, segWidth, modeY);
    placePair(actionLeaveButton, actionDeleteButton, segLeft, segWidth, actionY);
  }

  private static void placePair(
      SegmentedButton left, SegmentedButton right, int segLeft, int segWidth, int y) {
    left.setPosition(segLeft, y);
    left.setWidth(segWidth);
    right.setPosition(segLeft + segWidth, y);
    right.setWidth(segWidth);
  }

  void placeTabs(int innerLeft, int innerWidth, int y) {
    int tabWidth = innerWidth / 2;
    rulesTabButton.setPosition(innerLeft, y);
    rulesTabButton.setWidth(tabWidth);
    settingsTabButton.setPosition(innerLeft + tabWidth, y);
    settingsTabButton.setWidth(tabWidth);
  }

  static int clientSwitchX(int innerRight, boolean integrated) {
    int rightAnchor = innerRight - SWITCH_WIDTH;
    return integrated ? rightAnchor : rightAnchor - CLIENT_SWITCH_OFFSET;
  }

  static boolean deleteSegmentActive(boolean open, boolean globallyEnabled, boolean canDelete) {
    return open && globallyEnabled && canDelete;
  }

  void refresh(boolean open, boolean globallyEnabled, int innerRight, boolean integrated) {
    for (ClickableWidget widget : lockable) {
      widget.active = open && globallyEnabled;
    }
    actionDeleteButton.active =
        deleteSegmentActive(
            open, globallyEnabled, LootLockClient.getState().isAllowDeleteRejectedItems());
    int ruleCount =
        ActiveProfileActions.activeProfile()
            .map(p -> p.getRules() == null ? 0 : p.getRules().size())
            .orElse(0);
    rulesTabButton.setMessage(Text.translatable(LootLockLang.TAB_RULES_COUNT, ruleCount));
    serverSwitch.visible = open && !integrated;
    int clientX = clientSwitchX(innerRight, integrated);
    if (clientSwitch.getX() != clientX) {
      clientSwitch.setX(clientX);
    }
  }

  void paintLabels(DrawContext context, int headerTextY, int labelX, int modeY, int actionY) {
    if (serverSwitch.visible) {
      drawText(
          context,
          LootLockLang.PANEL_HEADER_SERVER,
          serverSwitch.getX() - SWITCH_LABEL_GAP,
          headerTextY,
          Palette.TITLE);
    }
    drawText(
        context,
        LootLockLang.PANEL_HEADER_PLAYER,
        clientSwitch.getX() - SWITCH_LABEL_GAP,
        headerTextY,
        Palette.TITLE);
    int labelOffset = (CONTROL_ROW_HEIGHT - 8) / 2;
    drawText(context, LootLockLang.PANEL_LABEL_MODE, labelX, modeY + labelOffset, Palette.INK);
    drawText(context, LootLockLang.PANEL_LABEL_ACTION, labelX, actionY + labelOffset, Palette.INK);
  }

  private static void drawText(DrawContext context, String key, int x, int y, int color) {
    context.drawText(
        MinecraftClient.getInstance().textRenderer, Text.translatable(key), x, y, color, false);
  }
}
