package com.grahambartley.lootlock.client.modmenu;

import com.grahambartley.lootlock.client.screen.LootLockClientPrefsScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class LootLockModMenuIntegration implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return LootLockClientPrefsScreen::new;
  }
}
