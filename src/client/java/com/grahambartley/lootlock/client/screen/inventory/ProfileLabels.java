package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.text.LootLockLang;
import net.minecraft.text.Text;

final class ProfileLabels {
  private ProfileLabels() {}

  static String ruleCountLabel(LootLockProfile profile) {
    int n = profile.getRules() == null ? 0 : profile.getRules().size();
    String key;
    if (profile.getMode() == FilterMode.DENYLIST) {
      key = n == 1 ? LootLockLang.PROFILE_META_DENY_ONE : LootLockLang.PROFILE_META_DENY_MANY;
    } else {
      key = n == 1 ? LootLockLang.PROFILE_META_ALLOW_ONE : LootLockLang.PROFILE_META_ALLOW_MANY;
    }
    return Text.translatable(key, n).getString();
  }
}
