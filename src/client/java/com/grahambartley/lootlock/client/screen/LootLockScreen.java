package com.grahambartley.lootlock.client.screen;

import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.text.LootLockLang;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

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
    return false;
  }
}
