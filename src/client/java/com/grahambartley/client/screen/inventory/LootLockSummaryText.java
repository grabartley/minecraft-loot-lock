package com.grahambartley.client.screen.inventory;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class LootLockSummaryText {
  private LootLockSummaryText() {}

  public static MutableText build(boolean enabled, LootLockProfile profile) {
    if (!enabled) {
      return Text.literal("Loot Lock is ")
          .append(Text.literal("off").formatted(Formatting.GRAY))
          .append(Text.literal(", every item is picked up normally for all profiles."));
    }
    if (profile == null) {
      return Text.literal("No active profile.").formatted(Formatting.GRAY);
    }

    int ruleCount = profile.getRules() == null ? 0 : profile.getRules().size();
    boolean deleteAction = profile.getRejectedItemAction() == RejectedItemAction.DELETE;
    FilterMode mode = profile.getMode() == null ? FilterMode.DENYLIST : profile.getMode();

    if (mode == FilterMode.DENYLIST) {
      if (ruleCount == 0) {
        return Text.literal("")
            .append(Text.literal("Denylist").formatted(Formatting.RED))
            .append(Text.literal(", nothing is filtered yet, add items below to skip them."));
      }
      MutableText tail =
          deleteAction
              ? Text.literal(" and ")
                  .append(Text.literal("deleted").formatted(Formatting.RED))
                  .append(Text.literal("."))
              : Text.literal(" and left on the ground.");
      return Text.literal("")
          .append(Text.literal("Denylist").formatted(Formatting.RED))
          .append(Text.literal(", "))
          .append(Text.literal(String.valueOf(ruleCount)).formatted(Formatting.WHITE))
          .append(Text.literal(" " + pluralItems(ruleCount) + " " + isAre(ruleCount) + " skipped"))
          .append(tail);
    }

    if (ruleCount == 0) {
      return Text.literal("")
          .append(Text.literal("Allowlist").formatted(Formatting.GREEN))
          .append(Text.literal(", no items allowed yet, you will keep nothing."));
    }
    MutableText tail =
        deleteAction
            ? Text.literal("everything else is ")
                .append(Text.literal("deleted").formatted(Formatting.RED))
                .append(Text.literal("."))
            : Text.literal("everything else is left on the ground.");
    return Text.literal("")
        .append(Text.literal("Allowlist").formatted(Formatting.GREEN))
        .append(Text.literal(", only these "))
        .append(Text.literal(String.valueOf(ruleCount)).formatted(Formatting.WHITE))
        .append(Text.literal(" " + pluralItems(ruleCount) + " are picked up, "))
        .append(tail);
  }

  private static String pluralItems(int n) {
    return n == 1 ? "item" : "items";
  }

  private static String isAre(int n) {
    return n == 1 ? "is" : "are";
  }
}
