package com.grahambartley.client.screen;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.text.LootLockLang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Dedicated full-screen surface used when the docked panel cannot fit alongside the inventory at
 * the current GUI scale or window size. The screen centers a {@link LootLockInventoryPanel} on top
 * of a dimmed backdrop and routes mouse and key input through the same handlers the docked panel
 * uses, so dropdown rows, inline rename, and the rules list scroll behaviour all work unchanged.
 *
 * <p>Drag-from-inventory is unavailable here by design — that affordance only makes sense while the
 * vanilla inventory slots are visible, which is why the docked-panel mode is preserved for low GUI
 * scales.
 */
public final class LootLockScreen extends Screen {
  private final Screen returnTo;
  private LootLockInventoryPanel panel;

  public LootLockScreen(Screen returnTo) {
    super(Text.translatable(LootLockLang.BRAND));
    this.returnTo = returnTo;
  }

  @Override
  protected void init() {
    super.init();
    panel = new LootLockInventoryPanel();
    int anchorX = (width - LootLockInventoryPanel.WIDTH) / 2;
    panel.attach(this, anchorX, 0, this::addDrawableChild);
    panel.setTab(LootLockInventoryPanel.getStickyActiveTab());
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
      panel.refresh();
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
  public boolean mouseClicked(double mouseX, double mouseY, int button) {
    if (panel != null && panel.handleDropdownMouseClick(mouseX, mouseY, button)) {
      return true;
    }
    return super.mouseClicked(mouseX, mouseY, button);
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
  public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
    if (panel != null && panel.handleInlineRenameKey(keyCode, scanCode, modifiers)) {
      return true;
    }
    return super.keyPressed(keyCode, scanCode, modifiers);
  }

  @Override
  public void close() {
    MinecraftClient client = MinecraftClient.getInstance();
    client.setScreen(returnTo);
  }

  @Override
  public boolean shouldPause() {
    // Keep the world ticking like the inventory does so this screen feels like an inventory peer.
    return false;
  }
}
