package com.grahambartley.lootlock.client.mixin;

import com.grahambartley.lootlock.client.compat.RecipeViewerBridge;
import com.grahambartley.lootlock.client.keybind.LootLockKeybinds;
import com.grahambartley.lootlock.client.screen.inventory.DragToAddRouter;
import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.lootlock.client.screen.inventory.PanelTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(HandledScreen.class)
public abstract class HandledScreenInputMixin {

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void lootlock$keepSearchFieldFocusOnInventoryKey(
      int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
    HandledScreen<?> self = (HandledScreen<?>) (Object) this;
    if (!(self instanceof InventoryScreen) || !(self instanceof LootLockPanelHolder)) {
      return;
    }
    LootLockInventoryPanel panel = ((LootLockPanelHolder) self).lootlock$getPanel();
    if (panel == null) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    if (panel.isInlineRenameActive()) {
      if (client != null
          && client.options != null
          && client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
        info.setReturnValue(true);
        return;
      }
      if (panel.handleInlineRenameKey(keyCode, scanCode, modifiers)) {
        info.setReturnValue(true);
      }
      return;
    }
    boolean searchFocused = panel.isSearchFieldFocused();
    if (searchFocused) {
      if (client != null
          && client.options != null
          && client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
        info.setReturnValue(true);
      }
      return;
    }
    if (!panel.isOpen() || client == null) {
      return;
    }
    if (LootLockKeybinds.matchesCycleProfile(keyCode, scanCode)) {
      LootLockKeybinds.cycleProfileNow(client);
      info.setReturnValue(true);
      return;
    }
    if (LootLockKeybinds.matchesToggleEnabled(keyCode, scanCode)) {
      LootLockKeybinds.toggleEnabledNow(client);
      info.setReturnValue(true);
    }
  }

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void lootlock$addHoveredRecipeViewerItem(
      int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
    if (info.isCancelled() || !LootLockKeybinds.matchesAddHovered(keyCode, scanCode)) {
      return;
    }
    HandledScreen<?> self = (HandledScreen<?>) (Object) this;
    if (self instanceof LootLockPanelHolder holder) {
      LootLockInventoryPanel panel = holder.lootlock$getPanel();
      if (panel != null && (panel.isInlineRenameActive() || panel.isSearchFieldFocused())) {
        return;
      }
    }
    if (RecipeViewerBridge.isTyping()) {
      return;
    }
    if (RecipeViewerBridge.addHovered(MinecraftClient.getInstance())) {
      info.setReturnValue(true);
    }
  }

  @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
  private void lootlock$catchDragReleaseOverPanel(
      double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
    if (button != 0) {
      return;
    }
    HandledScreen<?> self = (HandledScreen<?>) (Object) this;
    if (!(self instanceof InventoryScreen) || !(self instanceof LootLockPanelHolder)) {
      return;
    }
    LootLockInventoryPanel panel = ((LootLockPanelHolder) self).lootlock$getPanel();
    if (panel == null || !panel.isOpen() || !panel.containsPoint(mouseX, mouseY)) {
      return;
    }
    if (self.getScreenHandler() == null) {
      return;
    }
    ItemStack cursorStack = self.getScreenHandler().getCursorStack();
    if (cursorStack == null || cursorStack.isEmpty()) {
      return;
    }
    String itemId = DragToAddRouter.route(cursorStack);
    if (itemId == null) {
      return;
    }
    panel.setTab(PanelTab.RULES);
    panel.flashDropSuccess();
    panel.setDropArmed(false);
    info.setReturnValue(true);
  }
}
