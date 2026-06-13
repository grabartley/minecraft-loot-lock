package com.grahambartley.client.mixin;

import com.grahambartley.client.screen.inventory.LootLockInventoryPanel;

/**
 * Marker interface implemented by the InventoryScreen mixin so other mixins targeting parent
 * classes (e.g. HandledScreen) can read the docked panel via a single safe cast.
 */
public interface LootLockPanelHolder {
  LootLockInventoryPanel lootlock$getPanel();
}
