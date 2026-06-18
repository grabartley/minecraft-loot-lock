package com.grahambartley.client.mixin;

import com.grahambartley.client.LootLockClient;
import com.grahambartley.client.screen.LootLockScreen;
import com.grahambartley.client.screen.inventory.DragToAddRouter;
import com.grahambartley.client.screen.inventory.InventoryOnboardingController;
import com.grahambartley.client.screen.inventory.LootLockIconButton;
import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockPanelHolder;
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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mounts the Loot Lock docked panel onto the survival inventory screen. Adds a small entry button
 * beside the existing controls and a side-docked widget cluster that opens / closes when the button
 * is pressed. The inventory itself remains fully interactive.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenMixin implements LootLockPanelHolder {
  @Unique private LootLockInventoryPanel lootlock$panel;
  @Unique private ButtonWidget lootlock$entryButton;

  @Override
  public LootLockInventoryPanel lootlock$getPanel() {
    return lootlock$panel;
  }

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
            new LootLockIconButton(
                entryX,
                entryY,
                20,
                18,
                () -> lootlock$panel != null && lootlock$panel.isOpen(),
                button -> lootlock$onEntryClicked()));

    lootlock$panel = new LootLockInventoryPanel();
    lootlock$panel.attach(self, panelX, panelY, accessor::lootlock$invokeAddDrawableChild);
    // Restore sticky state so closing the inventory or detouring through a ConfirmScreen does not
    // force the user to re-open the panel each time.
    lootlock$panel.setTab(LootLockInventoryPanel.getStickyActiveTab());
    lootlock$panel.setOpen(LootLockInventoryPanel.getStickyOpenState());

    InventoryOnboardingController.maybeShow(LootLockClient.getClientSettingsManager());
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
      MinecraftClient client = MinecraftClient.getInstance();
      int scaledWidth = client.getWindow().getScaledWidth();
      int scaledHeight = client.getWindow().getScaledHeight();
      int anchorX = invX + 176 + 4;
      if (!LootLockInventoryPanel.canDock(anchorX, scaledWidth, scaledHeight)) {
        // Inline docking can't fit on this screen; collapse the docked panel and let the entry
        // button open the dedicated screen instead. Suppressing isOpen also keeps the status
        // effect HUD visible and avoids any phantom chrome.
        if (lootlock$panel.isOpen()) {
          lootlock$panel.setOpen(false);
        }
        return;
      }
      lootlock$panel.layout(anchorX, scaledWidth, scaledHeight);
      lootlock$panel.refresh();
      lootlock$updateDropArmedState(self, mouseX, mouseY);
      lootlock$panel.paintChrome(context);
    }
  }

  /**
   * Per-frame: arm the rules content well when the player is dragging a cursor stack over the open
   * panel on the Rules tab. The flag clears the moment the cursor leaves the panel rectangle or the
   * stack returns to a slot, so the gold inset reads as a live drop target.
   */
  @Unique
  private void lootlock$updateDropArmedState(InventoryScreen self, int mouseX, int mouseY) {
    if (lootlock$panel == null) {
      return;
    }
    boolean armed =
        lootlock$panel.isOpen()
            && lootlock$panel.getActiveTab() == PanelTab.RULES
            && self.getScreenHandler() != null
            && !self.getScreenHandler().getCursorStack().isEmpty()
            && lootlock$panel.containsPoint(mouseX, mouseY);
    lootlock$panel.setDropArmed(armed);
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
   * Drag-to-closed-button shortcut: when the panel is closed and the player releases a non-empty
   * cursor stack over the brand entry button, open the panel, switch to Rules, clear the search,
   * and route the stack through the same {@link DragToAddRouter} as the open-panel drop path.
   * Flashes the success animation so the player sees the rule landed.
   */
  @Inject(method = "mouseReleased", at = @At("HEAD"), cancellable = true)
  private void lootlock$catchDragReleaseOverEntryButton(
      double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
    if (button != 0 || lootlock$panel == null || lootlock$entryButton == null) {
      return;
    }
    if (lootlock$panel.isOpen() || !lootlock$isOverEntryButton(mouseX, mouseY)) {
      return;
    }
    InventoryScreen self = (InventoryScreen) (Object) this;
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
    lootlock$panel.setOpen(true);
    lootlock$panel.setTab(PanelTab.RULES);
    lootlock$panel.clearRulesSearch();
    lootlock$panel.flashDropSuccess();
    info.setReturnValue(true);
  }

  @Unique
  private boolean lootlock$isOverEntryButton(double mouseX, double mouseY) {
    ButtonWidget btn = lootlock$entryButton;
    return mouseX >= btn.getX()
        && mouseX < btn.getX() + btn.getWidth()
        && mouseY >= btn.getY()
        && mouseY < btn.getY() + btn.getHeight();
  }

  @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
  private void lootlock$routeDropdownOrAltClick(
      double mouseX, double mouseY, int button, CallbackInfoReturnable<Boolean> info) {
    if (lootlock$panel != null && lootlock$panel.handleDropdownMouseClick(mouseX, mouseY, button)) {
      info.setReturnValue(true);
      return;
    }
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
    InventoryScreen self = (InventoryScreen) (Object) this;
    MinecraftClient client = MinecraftClient.getInstance();
    int scaledWidth = client.getWindow().getScaledWidth();
    int scaledHeight = client.getWindow().getScaledHeight();
    int invX = ((HandledScreenAccessor) self).lootlock$getInvX();
    int anchorX = invX + 176 + 4;
    if (LootLockInventoryPanel.canDock(anchorX, scaledWidth, scaledHeight)) {
      if (lootlock$panel != null) {
        lootlock$panel.toggleOpen();
      }
      return;
    }
    // No room to dock; open the dedicated screen which scales to any window. The current inventory
    // is captured as the return target so closing the screen drops the player back into it.
    client.setScreen(new LootLockScreen(self));
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
