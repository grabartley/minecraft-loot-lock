package com.grahambartley.client.modmenu;

import com.grahambartley.client.screen.LootLockScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class LootLockModMenuIntegration implements ModMenuApi {
  @Override
  public ConfigScreenFactory<?> getModConfigScreenFactory() {
    return parent -> new LootLockScreen(parent);
  }
}
