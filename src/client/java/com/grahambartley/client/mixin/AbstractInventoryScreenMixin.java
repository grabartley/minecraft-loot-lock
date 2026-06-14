package com.grahambartley.client.mixin;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;
import com.grahambartley.client.screen.inventory.LootLockPanelHolder;
import net.minecraft.client.gui.screen.ingame.AbstractInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Vanilla {@code AbstractInventoryScreen.drawStatusEffects} renders the active potion-effect column
 * just to the right of the inventory background, which is exactly where the Loot Lock docked panel
 * lives. Without intervention the icons land directly under the panel and can never be hovered.
 *
 * <p>This mixin reroutes the column to the left of the inventory (matching the EffectsLeft mod's
 * pattern) whenever the panel is open, so the effects stay visible without overlapping the panel.
 * The vanilla layout is left untouched when the panel is closed.
 */
@Mixin(AbstractInventoryScreen.class)
public abstract class AbstractInventoryScreenMixin {

  @ModifyVariable(method = "drawStatusEffects", at = @At("STORE"), ordinal = 0)
  private int lootlock$flipStatusEffectsToLeftWhilePanelOpen(int originalRightX) {
    if (!(((Object) this) instanceof LootLockPanelHolder holder)) {
      return originalRightX;
    }
    LootLockInventoryPanel panel = holder.lootlock$getPanel();
    if (panel == null || !panel.isOpen()) {
      return originalRightX;
    }
    HandledScreen<?> self = (HandledScreen<?>) (Object) this;
    // Mirror the vanilla column to the left side of the inventory background. 124 matches the
    // wide-card width vanilla uses on the right; we reserve the same space on the left and let
    // vanilla's narrow detection take over if the screen is too small.
    return self.getX() - 124;
  }
}
