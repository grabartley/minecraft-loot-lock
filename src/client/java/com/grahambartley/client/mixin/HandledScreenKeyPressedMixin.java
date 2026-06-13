package com.grahambartley.client.mixin;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixed into {@link HandledScreen#keyPressed} (where the vanilla inventory-key close-on-press
 * lives) so the Loot Lock search field can swallow the inventory keybind while it's focused. The
 * key still reaches {@code charTyped} so the character types into the field.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenKeyPressedMixin {

  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void lootlock$keepSearchFieldFocusOnInventoryKey(
      int keyCode, int scanCode, int modifiers, CallbackInfoReturnable<Boolean> info) {
    HandledScreen<?> self = (HandledScreen<?>) (Object) this;
    if (!(self instanceof InventoryScreen) || !(self instanceof LootLockPanelHolder)) {
      return;
    }
    LootLockInventoryPanel panel = ((LootLockPanelHolder) self).lootlock$getPanel();
    if (panel == null || !panel.isSearchFieldFocused()) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    if (client != null
        && client.options != null
        && client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
      info.setReturnValue(true);
    }
  }
}
