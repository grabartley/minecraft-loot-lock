package com.grahambartley.client.mixin;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mounts the Loot Lock docked panel onto the survival inventory screen. Adds a small entry button
 * beside the existing controls and a side-docked widget cluster that opens / closes when the button
 * is pressed. The inventory itself remains fully interactive.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin {
  @Unique private LootLockInventoryPanel lootlock$panel;
  @Unique private ButtonWidget lootlock$entryButton;

  @Inject(method = "init", at = @At("TAIL"))
  private void lootlock$attachPanel(CallbackInfo info) {
    InventoryScreen self = (InventoryScreen) (Object) this;
    PlayerEntity player = playerOrNull();
    if (player == null) {
      return;
    }

    int panelX = self.width / 2 + 100;
    int panelY = (self.height - LootLockInventoryPanel.HEIGHT) / 2;
    int entryX = panelX - 24;
    int entryY = panelY;

    ScreenAccessor accessor = (ScreenAccessor) self;
    lootlock$entryButton =
        accessor.lootlock$invokeAddDrawableChild(
            ButtonWidget.builder(Text.literal("LL"), button -> lootlock$onEntryClicked())
                .dimensions(entryX, entryY, 20, 20)
                .build());

    lootlock$panel = new LootLockInventoryPanel();
    lootlock$panel.attach(self, panelX, panelY, accessor::lootlock$invokeAddDrawableChild);
    lootlock$panel.setOpen(false);
  }

  @Inject(method = "render", at = @At("TAIL"))
  private void lootlock$renderPanel(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
    if (lootlock$panel != null) {
      lootlock$panel.refresh();
      lootlock$panel.render(context, mouseX, mouseY, delta);
    }
  }

  @Unique
  private void lootlock$onEntryClicked() {
    if (lootlock$panel != null) {
      lootlock$panel.toggleOpen();
    }
  }

  @Unique
  private static PlayerEntity playerOrNull() {
    try {
      return MinecraftClient.getInstance().player;
    } catch (Throwable ignored) {
      return null;
    }
  }
}
