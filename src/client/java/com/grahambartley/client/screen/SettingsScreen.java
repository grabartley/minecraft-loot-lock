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

    ButtonWidget blockedHudButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal(blockedHudLabel()),
                    button -> {
                      draft.setShowBlockedHudNotification(!draft.isShowBlockedHudNotification());
                      button.setMessage(Text.literal(blockedHudLabel()));
                    })
                .dimensions(left, top, 200, 20)
                .build());

    ButtonWidget actionbarButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal(actionbarLabel()),
                    button -> {
                      draft.setShowActionbarFallback(!draft.isShowActionbarFallback());
                      button.setMessage(Text.literal(actionbarLabel()));
                    })
                .dimensions(left, top + 24, 200, 20)
                .build());

    ButtonWidget deleteConfirmButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal(deleteConfirmLabel()),
                    button -> {
                      draft.setConfirmBeforeEnablingDelete(!draft.isConfirmBeforeEnablingDelete());
                      button.setMessage(Text.literal(deleteConfirmLabel()));
                    })
                .dimensions(left, top + 48, 200, 20)
                .build());

    ButtonWidget profileToastButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal(profileToastLabel()),
                    button -> {
                      draft.setEnableProfileCycleToast(!draft.isEnableProfileCycleToast());
                      button.setMessage(Text.literal(profileToastLabel()));
                    })
                .dimensions(left, top + 72, 200, 20)
                .build());

    ButtonWidget uiScaleButton =
        addDrawableChild(
            ButtonWidget.builder(
                    Text.literal(scaleLabel()),
                    button -> {
                      int next =
                          draft.getUiScalePercent() >= 140 ? 80 : draft.getUiScalePercent() + 10;
                      draft.setUiScalePercent(next);
                      button.setMessage(Text.literal(scaleLabel()));
                    })
                .dimensions(left, top + 96, 200, 20)
                .build());

    blockedHudButton.active = false;
    actionbarButton.active = false;
    deleteConfirmButton.active = false;
    profileToastButton.active = false;
    uiScaleButton.active = false;

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
        ButtonWidget.builder(Text.literal("Cancel"), button -> close())
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
    // Settings is a local draft, ESC and Cancel both discard unsaved draft changes.
    if (this.client != null) {
      this.client.setScreen(parent);
    }
  }

  private String blockedHudLabel() {
    return "Show blocked HUD (coming in #35): " + onOff(draft.isShowBlockedHudNotification());
  }

  private String actionbarLabel() {
    return "Actionbar fallback (coming in #35): " + onOff(draft.isShowActionbarFallback());
  }

  private String deleteConfirmLabel() {
    return "Confirm delete enable (coming in #36): " + onOff(draft.isConfirmBeforeEnablingDelete());
  }

  private String profileToastLabel() {
    return "Profile cycle toast (coming soon): " + onOff(draft.isEnableProfileCycleToast());
  }

  private String scaleLabel() {
    return "UI scale (coming soon): " + draft.getUiScalePercent() + "%";
  }

  private static String onOff(boolean enabled) {
    return enabled ? "On" : "Off";
  }
}
