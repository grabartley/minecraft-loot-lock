package com.grahambartley.client.mixin;

import com.grahambartley.client.screen.inventory.DragToAddRouter;
import com.grahambartley.client.screen.inventory.LootLockIconButton;
import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.PanelTab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
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

    // Anchor everything to the vanilla inventory's actual top-left. Read via the HandledScreen
    // getX()/getY() accessors so the position tracks the recipe-book layout shift on every render.
    int invX = ((HandledScreenAccessor) self).lootlock$getInvX();
    int invY = ((HandledScreenAccessor) self).lootlock$getInvY();
    int entryX = invX + 124;
    int entryY = invY + 61;
    int panelX = invX + 176 + 4;
    int panelY = invY;

    ScreenAccessor accessor = (ScreenAccessor) self;
    lootlock$entryButton =
        accessor.lootlock$invokeAddDrawableChild(
            new LootLockIconButton(entryX, entryY, 20, 18, button -> lootlock$onEntryClicked()));

    lootlock$panel = new LootLockInventoryPanel();
    lootlock$panel.attach(self, panelX, panelY, accessor::lootlock$invokeAddDrawableChild);
    lootlock$panel.setOpen(false);
  }

  /**
   * Paint chrome (panel frame + dark wells + content well) BEFORE the host screen renders its
   * widget children, so the wells sit behind the actual buttons rather than covering them.
   */
  @Inject(method = "drawBackground", at = @At("TAIL"))
  private void lootlock$renderChromeBeforeWidgets(
      DrawContext context, float delta, int mouseX, int mouseY, CallbackInfo info) {
    InventoryScreen self = (InventoryScreen) (Object) this;
    int invX = ((HandledScreenAccessor) self).lootlock$getInvX();
    int invY = ((HandledScreenAccessor) self).lootlock$getInvY();
    if (lootlock$entryButton != null) {
      lootlock$entryButton.setPosition(invX + 128, invY + 61);
    }
    if (lootlock$panel != null) {
      lootlock$panel.relocate(invX + 176 + 4, invY);
      lootlock$panel.refresh();
      lootlock$panel.paintChrome(context);
    }
  }

  /** Paint foreground (labels, summary text, brand icon) AFTER widgets so labels read clearly. */
  @Inject(method = "render", at = @At("TAIL"))
  private void lootlock$renderForeground(
      DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo info) {
    if (lootlock$panel != null) {
      lootlock$panel.paintForeground(context, mouseX, mouseY, delta);
    }
  }

  /**
   * When the Rules tab search field is focused, swallow the inventory keybind so the user can
   * actually type the letter (e.g. 'e') without the vanilla inventory close-on-key handler firing.
   * The {@code charTyped} path still runs, so the char shows up in the field.
   */
  @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
  private void lootlock$keepSearchFieldFocusOnInventoryKey(
      int keyCode,
      int scanCode,
      int modifiers,
      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> info) {
    if (lootlock$panel == null || !lootlock$panel.isSearchFieldFocused()) {
      return;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    if (client != null
        && client.options != null
        && client.options.inventoryKey.matchesKey(keyCode, scanCode)) {
      info.setReturnValue(true);
    }
  }

  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void lootlock$interceptAltClickForRuleAdd(
      double mouseX,
      double mouseY,
      int button,
      org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> info) {
    if (button != 0 || !Screen.hasAltDown()) {
      return;
    }
    InventoryScreen self = (InventoryScreen) (Object) this;
    Slot slot = lootlock$slotAt(self, mouseX, mouseY);
    if (slot == null) {
      return;
    }
    ItemStack stack = slot.getStack();
    String added = DragToAddRouter.route(stack);
    if (added == null) {
      return;
    }
    if (lootlock$panel != null) {
      lootlock$panel.setOpen(true);
      lootlock$panel.setTab(PanelTab.RULES);
    }
    info.setReturnValue(true);
  }

  @Unique
  private static Slot lootlock$slotAt(InventoryScreen screen, double mouseX, double mouseY) {
    if (screen.getScreenHandler() == null) {
      return null;
    }
    int originX = ((HandledScreenAccessor) screen).lootlock$getInvX();
    int originY = ((HandledScreenAccessor) screen).lootlock$getInvY();
    for (Slot slot : screen.getScreenHandler().slots) {
      if (slot == null) {
        continue;
      }
      int slotX = originX + slot.x;
      int slotY = originY + slot.y;
      if (mouseX >= slotX && mouseX < slotX + 16 && mouseY >= slotY && mouseY < slotY + 16) {
        return slot;
      }
    }
    return null;
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
