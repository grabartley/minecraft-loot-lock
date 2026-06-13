package com.grahambartley.client.screen.inventory;

/**
 * Marker interface implemented (via mixin) by the inventory screen so other mixins targeting parent
 * classes (e.g. {@code HandledScreen}) can read the docked panel through a single safe cast.
 */
public interface LootLockPanelHolder {
  LootLockInventoryPanel lootlock$getPanel();
}
