package com.grahambartley.client.mixin;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockPanelHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Suppresses the vanilla status-effect HUD column that paints alongside the inventory whenever the
 * Loot Lock panel is mounted and open. The vanilla column would otherwise overlap the docked panel
 * since both want the same screen real estate next to the inventory background.
 */
@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin {

  @Inject(method = "drawStatusEffects", at = @At("HEAD"), cancellable = true)
  private void lootlock$suppressStatusEffectsWhenPanelOpen(
      DrawContext context, int mouseX, int mouseY, CallbackInfo info) {
    if (!(((Object) this) instanceof LootLockPanelHolder holder)) {
      return;
    }
    LootLockInventoryPanel panel = holder.lootlock$getPanel();
    if (panel != null && panel.isOpen()) {
      info.cancel();
    }
  }
}
