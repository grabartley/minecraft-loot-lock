package com.grahambartley.client.mixin;

import com.grahambartley.client.keybind.LootLockKeybinds;
import com.grahambartley.client.screen.inventory.DragToAddRouter;
import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockPanelHolder;
import com.grahambartley.client.screen.inventory.PanelTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Inputs that are declared on {@link HandledScreen} but not overridden in {@link InventoryScreen}.
 * Bundles the inventory-keybind swallow (so typing into the Rules tab search field doesn't close
 * the screen) and the drag-to-add release handler (so dropping a cursor stack on the panel adds the
 * item rather than throwing it into the world).
 */
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
    // Inline-rename field has top priority while active: Enter / Escape / typing belong to it.
    if (panel.handleInlineRenameKey(keyCode, scanCode, modifiers)) {
      info.setReturnValue(true);
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    boolean searchFocused = panel.isSearchFieldFocused();
    if (searchFocused) {
      if (client != null
          && client.options != null
          && client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
        info.setReturnValue(true);
      }
      return;
    }
    // Vanilla freezes in-game keybinds while a Screen is open; manually fire the Loot Lock cycle
    // binding when the panel is open and the user is not typing into the search field so the
    // hotkey is consistent inside and outside the inventory.
    if (!panel.isOpen() || client == null) {
      return;
    }
    if (LootLockKeybinds.matchesCycleProfile(keyCode, scanCode)) {
      LootLockKeybinds.cycleProfileNow(client);
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
    // Keep the stack on the cursor so the user can put it back; vanilla would otherwise drop the
    // whole stack into the world from this release.
    panel.setTab(PanelTab.RULES);
    panel.flashDropSuccess();
    panel.setDropArmed(false);
    info.setReturnValue(true);
  }
}
