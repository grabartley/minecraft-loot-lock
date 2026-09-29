package com.grahambartley.lootlock.client.mixin;

import com.grahambartley.lootlock.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.lootlock.client.screen.inventory.LootLockPanelHolder;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
