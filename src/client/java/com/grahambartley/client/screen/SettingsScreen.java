package com.grahambartley.client.screen;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.config.ClientSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public final class SettingsScreen extends Screen {
  private final Screen parent;
  private final ClientSettings draft;

  public SettingsScreen(Screen parent) {
    super(Text.literal("LootLock Settings"));
    this.parent = parent;
    this.draft = LootLockClient.getClientSettingsManager().getSettingsCopy();
  }

  @Override
  protected void init() {
    int left = this.width / 2 - 100;
    int top = this.height / 4;

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal(blockedHudLabel()),
                button -> {
                  draft.setShowBlockedHudNotification(!draft.isShowBlockedHudNotification());
                  button.setMessage(Text.literal(blockedHudLabel()));
                })
            .dimensions(left, top, 200, 20)
            .build());

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal(actionbarLabel()),
                button -> {
                  draft.setShowActionbarFallback(!draft.isShowActionbarFallback());
                  button.setMessage(Text.literal(actionbarLabel()));
                })
            .dimensions(left, top + 24, 200, 20)
            .build());

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal(deleteConfirmLabel()),
                button -> {
                  draft.setConfirmBeforeEnablingDelete(!draft.isConfirmBeforeEnablingDelete());
                  button.setMessage(Text.literal(deleteConfirmLabel()));
                })
            .dimensions(left, top + 48, 200, 20)
            .build());

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal(profileToastLabel()),
                button -> {
                  draft.setEnableProfileCycleToast(!draft.isEnableProfileCycleToast());
                  button.setMessage(Text.literal(profileToastLabel()));
                })
            .dimensions(left, top + 72, 200, 20)
            .build());

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal(scaleLabel()),
                button -> {
                  int next = draft.getUiScalePercent() >= 140 ? 80 : draft.getUiScalePercent() + 10;
                  draft.setUiScalePercent(next);
                  button.setMessage(Text.literal(scaleLabel()));
                })
            .dimensions(left, top + 96, 200, 20)
            .build());

    addDrawableChild(
        ButtonWidget.builder(
                Text.literal("Save"),
                button -> {
                  LootLockClient.getClientSettingsManager().replaceAndSave(draft);
                  close();
                })
            .dimensions(left, top + 128, 97, 20)
            .build());
    addDrawableChild(
        ButtonWidget.builder(Text.literal("Back"), button -> close())
            .dimensions(left + 103, top + 128, 97, 20)
            .build());
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    renderBackground(context);
    super.render(context, mouseX, mouseY, delta);
    context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 18, 0xFFFFFF);
  }

  @Override
  public void close() {
    if (this.client != null) {
      this.client.setScreen(parent);
    }
  }

  private String blockedHudLabel() {
    return "Show blocked HUD: " + onOff(draft.isShowBlockedHudNotification());
  }

  private String actionbarLabel() {
    return "Show actionbar fallback: " + onOff(draft.isShowActionbarFallback());
  }

  private String deleteConfirmLabel() {
    return "Confirm delete enable: " + onOff(draft.isConfirmBeforeEnablingDelete());
  }

  private String profileToastLabel() {
    return "Profile cycle toast: " + onOff(draft.isEnableProfileCycleToast());
  }

  private String scaleLabel() {
    return "UI scale: " + draft.getUiScalePercent() + "%";
  }

  private static String onOff(boolean enabled) {
    return enabled ? "On" : "Off";
  }
}
