package com.grahambartley.client.screen;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.text.LootLockLang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Mod Menu config screen that surfaces only the global client preferences (NOTIFICATIONS, SAFETY,
 * CONTROLS, ABOUT). Profiles, rules, and per-world server policy are intentionally absent because
 * Mod Menu can be opened from the title screen where no world is loaded. Users edit per-world state
 * by opening the Loot Lock panel from their inventory while in a world.
 */
public final class LootLockClientPrefsScreen extends Screen {
  private final Screen returnTo;
  private LootLockInventoryPanel panel;

  public LootLockClientPrefsScreen(Screen returnTo) {
    super(Text.translatable(LootLockLang.BRAND));
    this.returnTo = returnTo;
  }

  @Override
  protected void init() {
    super.init();
    panel = new LootLockInventoryPanel();
    panel.setClientPrefsMode(true);
    int anchorX = (width - LootLockInventoryPanel.WIDTH) / 2;
    panel.attach(this, anchorX, 0, this::addDrawableChild);
    panel.layout(anchorX, width, height);
    panel.setOpen(true);
  }

  // Screen.render already draws the background before the widgets, so the chrome is painted from
  // renderBackground rather than render; a manual renderBackground call here would blur and darken
  // the chrome a second time.
  @Override
  public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
    super.renderBackground(context, mouseX, mouseY, delta);
    if (panel != null) {
      int anchorX = (width - LootLockInventoryPanel.WIDTH) / 2;
      panel.layout(anchorX, width, height);
      panel.paintChrome(context);
    }
  }

  @Override
  public void render(DrawContext context, int mouseX, int mouseY, float delta) {
    super.render(context, mouseX, mouseY, delta);
    if (panel != null) {
      panel.paintForeground(context, mouseX, mouseY, delta);
    }
  }

  @Override
  public boolean mouseScrolled(
      double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
    if (panel != null && panel.handleMouseScroll(mouseX, mouseY, verticalAmount)) {
      return true;
    }
    return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
  }

  @Override
  public void close() {
    if (panel != null) {
      panel.setOpen(false);
    }
    MinecraftClient client = MinecraftClient.getInstance();
    client.setScreen(returnTo);
  }
}
