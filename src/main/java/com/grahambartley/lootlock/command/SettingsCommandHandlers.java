package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import com.grahambartley.lootlock.server.ServerPolicyService;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

final class SettingsCommandHandlers {
  private SettingsCommandHandlers() {}

  static int policyStatus(CommandContext<ServerCommandSource> context) {
    boolean current = LootLock.SERVER_CONFIG.allowDeleteRejectedItems();
    context
        .getSource()
        .sendFeedback(() -> Text.translatable(LootLockLang.COMMAND_POLICY_STATUS, current), false);
    return 1;
  }

  static int setAllowDeleteRejectedItems(
      CommandContext<ServerCommandSource> context, boolean allowDeleteRejectedItems) {
    boolean updated =
        ServerPolicyService.updateAllowDeleteRejectedItems(
            context.getSource().getServer(), allowDeleteRejectedItems);
    if (!updated) {
      context.getSource().sendError(Text.translatable(LootLockLang.COMMAND_POLICY_ERROR_PERSIST));
      return 0;
    }
    for (ServerPlayerEntity player :
        context.getSource().getServer().getPlayerManager().getPlayerList()) {
      ServerToClientPackets.sendAuthoritativeSync(player);
    }
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.translatable(
                    LootLockLang.COMMAND_POLICY_FEEDBACK_SET, allowDeleteRejectedItems),
            true);
    return 1;
  }

  static int handleStatus(ServerCommandSource source, CommandStateContext state) {
    sendStatus(source, state);
    return 1;
  }

  static int handleEnable(ServerCommandSource source, CommandStateContext state, boolean enabled) {
    CommandProfileEdits.applyGlobalEnable(state.data(), enabled);
    state.markDirty(source);
    Text message;
    if (state.isSelfTargeted()) {
      message =
          Text.translatable(
              enabled ? LootLockLang.COMMAND_ENABLE_SELF : LootLockLang.COMMAND_DISABLE_SELF);
    } else {
      message =
          Text.translatable(
              enabled ? LootLockLang.COMMAND_ENABLE_TARGET : LootLockLang.COMMAND_DISABLE_TARGET,
              state.displayName());
    }
    source.sendFeedback(() -> message, false);
    sendStatus(source, state);
    state.syncIfOnline();
    return 1;
  }

  static int handleMode(ServerCommandSource source, CommandStateContext state, FilterMode mode) {
    state.profile().setMode(mode);
    state.markDirty(source);
    Text modeLabel = modeLabel(mode);
    String profileName = state.profile().getName();
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_MODE_SELF, modeLabel, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_MODE_TARGET, modeLabel, state.displayName(), profileName);
    source.sendFeedback(() -> message, false);
    sendStatus(source, state);
    state.syncIfOnline();
    return 1;
  }

  static int handleAction(
      ServerCommandSource source, CommandStateContext state, RejectedItemAction action) {
    RejectedItemAction normalizedAction =
        CommandProfileEdits.normalizeRejectedItemAction(
            action, LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
    if (action == RejectedItemAction.DELETE && normalizedAction != RejectedItemAction.DELETE) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_DELETE_POLICY_BLOCKED));
      return 0;
    }

    state.profile().setRejectedItemAction(normalizedAction);
    state.markDirty(source);
    Text actionLabel = actionLabel(normalizedAction);
    String profileName = state.profile().getName();
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_ACTION_SELF, actionLabel, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_ACTION_TARGET, actionLabel, state.displayName(), profileName);
    source.sendFeedback(() -> message, false);
    if (normalizedAction == RejectedItemAction.DELETE) {
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_ACTION_DELETE_WARNING), false);
    }
    sendStatus(source, state);
    state.syncIfOnline();
    return 1;
  }

  static void sendStatus(ServerCommandSource source, CommandStateContext state) {
    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_STATUS_HEADER_SELF)
            : Text.translatable(LootLockLang.COMMAND_STATUS_HEADER_TARGET, state.displayName());
    LootLockProfile profile = state.profile();
    source.sendFeedback(() -> header, false);
    String profileName = profile.getName();
    source.sendFeedback(
        () -> Text.translatable(LootLockLang.COMMAND_STATUS_LINE_ACTIVE, profileName), false);
    boolean enabled = profile.isEnabled();
    source.sendFeedback(
        () -> Text.translatable(LootLockLang.COMMAND_STATUS_LINE_ENABLED, enabled), false);
    Text modeLabel = modeLabel(profile.getMode());
    source.sendFeedback(
        () -> Text.translatable(LootLockLang.COMMAND_STATUS_LINE_MODE, modeLabel), false);
    Text actionLabel = actionLabel(profile.getRejectedItemAction());
    source.sendFeedback(
        () -> Text.translatable(LootLockLang.COMMAND_STATUS_LINE_ACTION, actionLabel), false);
    int ruleCount = profile.getRules().size();
    source.sendFeedback(
        () -> Text.translatable(LootLockLang.COMMAND_STATUS_LINE_RULE_COUNT, ruleCount), false);
  }

  static MutableText modeLabel(FilterMode mode) {
    return Text.translatable(
        mode == FilterMode.ALLOWLIST ? LootLockLang.MODE_ALLOWLIST : LootLockLang.MODE_DENYLIST);
  }

  static MutableText actionLabel(RejectedItemAction action) {
    return Text.translatable(
        action == RejectedItemAction.DELETE
            ? LootLockLang.ACTION_DELETE
            : LootLockLang.ACTION_LEAVE);
  }
}
