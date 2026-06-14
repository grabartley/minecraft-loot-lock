package com.grahambartley.client.screen.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Placeholder content for the Settings tab. Story 4 wires the full settings UI; for now the tab
 * exists as a switchable view so the tab strip works end-to-end and the user can verify routing.
 */
public final class SettingsTabView {
  private final List<ClickableWidget> widgets = new ArrayList<>();
  private LootLockInventoryPanel panel;
  private boolean visible;

  public void attach(LootLockInventoryPanel panel, Consumer<ClickableWidget> addDrawableChild) {
    this.panel = panel;
    widgets.clear();
    setVisible(false);
  }

  public void setVisible(boolean visible) {
    this.visible = visible;
    for (ClickableWidget widget : widgets) {
      widget.visible = visible;
    }
  }

  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    if (!visible || panel == null) {
      return;
    }
    context.drawText(
        MinecraftClient.getInstance().textRenderer,
        Text.literal("Settings come online in the next story.").formatted(Formatting.GRAY),
        panel.getContentInsetX(),
        panel.getContentInsetY() + 8,
        0x6E6E6E,
        false);
  }
}
