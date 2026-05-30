package com.grahambartley.command;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.server.ServerPlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

public final class LootLockCommand {
  private LootLockCommand() {}

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
        CommandManager.literal("lootlock")
            .executes(LootLockCommand::help)
            .then(
                CommandManager.literal("status")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(LootLockCommand::status))
            .then(
                CommandManager.literal("enable")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(context -> setEnabled(context, true)))
            .then(
                CommandManager.literal("disable")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(context -> setEnabled(context, false)))
            .then(
                CommandManager.literal("mode")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("denylist")
                            .executes(context -> setMode(context, FilterMode.DENYLIST)))
                    .then(
                        CommandManager.literal("allowlist")
                            .executes(context -> setMode(context, FilterMode.ALLOWLIST))))
            .then(
                CommandManager.literal("action")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("leave")
                            .executes(
                                context -> setAction(context, RejectedItemAction.LEAVE_ON_GROUND)))
                    .then(
                        CommandManager.literal("delete")
                            .executes(LootLockCommand::deleteConfirmHelp)
                            .then(
                                CommandManager.literal("confirm")
                                    .executes(
                                        context ->
                                            setAction(context, RejectedItemAction.DELETE))))));
  }

  static String modeToken(FilterMode mode) {
    return mode == FilterMode.ALLOWLIST ? "allowlist" : "denylist";
  }

  static String actionToken(RejectedItemAction action) {
    return action == RejectedItemAction.DELETE ? "delete" : "leave";
  }

  private static int help(CommandContext<ServerCommandSource> context) {
    context.getSource().sendFeedback(() -> Text.literal("LootLock commands:"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock status"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock enable"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock disable"), false);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("- /lootlock mode denylist|allowlist"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock action leave"), false);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("- /lootlock action delete confirm"), false);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Note: profile commands are player-only."), false);
    return 1;
  }

  private static int deleteConfirmHelp(CommandContext<ServerCommandSource> context) {
    context
        .getSource()
        .sendFeedback(
            () -> Text.literal("Add 'confirm' to set rejected-item action to delete."), false);
    return 1;
  }

  private static int status(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    sendStatus(context.getSource(), state.profile);
    return 1;
  }

  private static int setEnabled(CommandContext<ServerCommandSource> context, boolean enabled) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    state.profile.setEnabled(enabled);
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.literal(
                    "LootLock "
                        + (enabled ? "enabled" : "disabled")
                        + " for profile '"
                        + state.profile.getName()
                        + "'."),
            false);
    sendStatus(context.getSource(), state.profile);
    return 1;
  }

  private static int setMode(CommandContext<ServerCommandSource> context, FilterMode mode) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    state.profile.setMode(mode);
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.literal(
                    "LootLock mode set to "
                        + modeToken(mode)
                        + " for profile '"
                        + state.profile.getName()
                        + "'."),
            false);
    sendStatus(context.getSource(), state.profile);
    return 1;
  }

  private static int setAction(
      CommandContext<ServerCommandSource> context, RejectedItemAction action) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    state.profile.setRejectedItemAction(action);
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.literal(
                    "LootLock rejected-item action set to "
                        + actionToken(action)
                        + " for profile '"
                        + state.profile.getName()
                        + "'."),
            false);
    sendStatus(context.getSource(), state.profile);
    return 1;
  }

  private static void sendStatus(ServerCommandSource source, LootLockProfile profile) {
    source.sendFeedback(() -> Text.literal("LootLock status:"), false);
    source.sendFeedback(() -> Text.literal("- Active profile: " + profile.getName()), false);
    source.sendFeedback(() -> Text.literal("- Enabled: " + profile.isEnabled()), false);
    source.sendFeedback(() -> Text.literal("- Mode: " + modeToken(profile.getMode())), false);
    source.sendFeedback(
        () -> Text.literal("- Action: " + actionToken(profile.getRejectedItemAction())), false);
    source.sendFeedback(() -> Text.literal("- Rule count: " + profile.getRules().size()), false);
  }

  private static StateContext resolveStateContext(ServerCommandSource source) {
    ServerPlayerEntity player;
    try {
      player = source.getPlayerOrThrow();
    } catch (CommandSyntaxException ex) {
      source.sendError(Text.literal("This command can only be used by a player."));
      return null;
    }

    ServerPlayerDataManager dataManager = LootLock.PLAYER_DATA_MANAGER;
    if (dataManager == null) {
      source.sendError(Text.literal("LootLock is not ready yet."));
      return null;
    }

    LootLockPlayerData playerData = dataManager.get(player);
    LootLockProfile activeProfile = playerData.getActiveProfile().orElse(null);
    if (activeProfile == null) {
      source.sendError(Text.literal("No active LootLock profile found."));
      return null;
    }

    return new StateContext(player, dataManager, activeProfile);
  }

  private record StateContext(
      ServerPlayerEntity player, ServerPlayerDataManager dataManager, LootLockProfile profile) {}
}
