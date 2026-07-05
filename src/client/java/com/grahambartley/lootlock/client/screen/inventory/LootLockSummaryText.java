package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.text.LootLockLang;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LootLockSummaryText {
  private LootLockSummaryText() {}

  public static MutableText build(boolean enabled, LootLockProfile profile) {
    if (!enabled) {
      return Text.translatable(LootLockLang.SUMMARY_OFF_PREFIX)
          .append(Text.translatable(LootLockLang.SUMMARY_OFF_WORD).formatted(Formatting.GRAY))
          .append(Text.translatable(LootLockLang.SUMMARY_OFF_SUFFIX));
    }
    if (profile == null) {
      return Text.translatable(LootLockLang.SUMMARY_NO_ACTIVE).formatted(Formatting.GRAY);
    }

    int ruleCount = profile.getRules() == null ? 0 : profile.getRules().size();
    boolean deleteAction = profile.getRejectedItemAction() == RejectedItemAction.DELETE;
    FilterMode mode = profile.getMode() == null ? FilterMode.DENYLIST : profile.getMode();

    if (mode == FilterMode.DENYLIST) {
      if (ruleCount == 0) {
        return Text.empty()
            .append(Text.translatable(LootLockLang.MODE_DENYLIST).formatted(Formatting.RED))
            .append(Text.translatable(LootLockLang.SUMMARY_DENYLIST_EMPTY));
      }
      MutableText tail =
          deleteAction
              ? Text.translatable(LootLockLang.SUMMARY_TAIL_DELETE_PREFIX_SHORT)
                  .append(
                      Text.translatable(LootLockLang.SUMMARY_DELETE_WORD).formatted(Formatting.RED))
                  .append(Text.translatable(LootLockLang.SUMMARY_TAIL_DELETE_SUFFIX))
              : Text.translatable(LootLockLang.SUMMARY_TAIL_LEAVE_SHORT);
      return Text.empty()
          .append(Text.translatable(LootLockLang.MODE_DENYLIST).formatted(Formatting.RED))
          .append(Text.translatable(LootLockLang.SUMMARY_SEPARATOR))
          .append(Text.literal(String.valueOf(ruleCount)).formatted(Formatting.WHITE))
          .append(
              Text.translatable(
                  ruleCount == 1
                      ? LootLockLang.SUMMARY_DENYLIST_BODY_ONE
                      : LootLockLang.SUMMARY_DENYLIST_BODY_MANY))
          .append(tail);
    }

    if (ruleCount == 0) {
      return Text.empty()
          .append(Text.translatable(LootLockLang.MODE_ALLOWLIST).formatted(Formatting.GREEN))
          .append(Text.translatable(LootLockLang.SUMMARY_ALLOWLIST_EMPTY));
    }
    MutableText tail =
        deleteAction
            ? Text.translatable(LootLockLang.SUMMARY_TAIL_DELETE_PREFIX_LONG)
                .append(
                    Text.translatable(LootLockLang.SUMMARY_DELETE_WORD).formatted(Formatting.RED))
                .append(Text.translatable(LootLockLang.SUMMARY_TAIL_DELETE_SUFFIX))
            : Text.translatable(LootLockLang.SUMMARY_TAIL_LEAVE_LONG);
    return Text.empty()
        .append(Text.translatable(LootLockLang.MODE_ALLOWLIST).formatted(Formatting.GREEN))
        .append(Text.translatable(LootLockLang.SUMMARY_ALLOWLIST_INTRO))
        .append(Text.literal(String.valueOf(ruleCount)).formatted(Formatting.WHITE))
        .append(
            Text.translatable(
                ruleCount == 1
                    ? LootLockLang.SUMMARY_ALLOWLIST_BODY_ONE
                    : LootLockLang.SUMMARY_ALLOWLIST_BODY_MANY))
        .append(tail);
  }
}
