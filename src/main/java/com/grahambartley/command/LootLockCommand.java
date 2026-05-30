package com.grahambartley.command;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.server.ServerPlayerDataManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.Locale;

public final class LootLockCommand {
    private LootLockCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("lootlock")
            .then(CommandManager.literal("status")
                .executes(LootLockCommand::status))
            .then(CommandManager.literal("enable")
                .executes(context -> setEnabled(context, true)))
            .then(CommandManager.literal("disable")
                .executes(context -> setEnabled(context, false)))
            .then(CommandManager.literal("mode")
                .then(CommandManager.literal("denylist")
                    .executes(context -> setMode(context, FilterMode.DENYLIST)))
                .then(CommandManager.literal("allowlist")
                    .executes(context -> setMode(context, FilterMode.ALLOWLIST))))
            .then(CommandManager.literal("action")
                .then(CommandManager.literal("leave")
                    .executes(context -> setAction(context, RejectedItemAction.LEAVE_ON_GROUND)))
                .then(CommandManager.literal("delete")
                    .then(CommandManager.literal("confirm")
                        .executes(context -> setAction(context, RejectedItemAction.DELETE))))));
    }

    static String modeToken(FilterMode mode) {
        return mode == FilterMode.ALLOWLIST ? "allowlist" : "denylist";
    }

    static String actionToken(RejectedItemAction action) {
        return action == RejectedItemAction.DELETE ? "delete" : "leave";
    }

    static FilterMode parseMode(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "denylist" -> FilterMode.DENYLIST;
            case "allowlist" -> FilterMode.ALLOWLIST;
            default -> null;
        };
    }

    static RejectedItemAction parseAction(String value) {
        if (value == null) {
            return null;
        }
        return switch (value.toLowerCase(Locale.ROOT)) {
            case "leave" -> RejectedItemAction.LEAVE_ON_GROUND;
            case "delete" -> RejectedItemAction.DELETE;
            default -> null;
        };
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
        context.getSource().sendFeedback(() -> Text.literal("LootLock " + (enabled ? "enabled" : "disabled") + " for profile '" + state.profile.getName() + "'."), false);
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
        context.getSource().sendFeedback(() -> Text.literal("LootLock mode set to " + modeToken(mode) + " for profile '" + state.profile.getName() + "'."), false);
        sendStatus(context.getSource(), state.profile);
        return 1;
    }

    private static int setAction(CommandContext<ServerCommandSource> context, RejectedItemAction action) {
        StateContext state = resolveStateContext(context.getSource());
        if (state == null) {
            return 0;
        }

        state.profile.setRejectedItemAction(action);
        state.dataManager.markDirty(state.player);
        context.getSource().sendFeedback(() -> Text.literal("LootLock rejected-item action set to " + actionToken(action) + " for profile '" + state.profile.getName() + "'."), false);
        sendStatus(context.getSource(), state.profile);
        return 1;
    }

    private static void sendStatus(ServerCommandSource source, LootLockProfile profile) {
        source.sendFeedback(() -> Text.literal("LootLock status:"), false);
        source.sendFeedback(() -> Text.literal("- Active profile: " + profile.getName()), false);
        source.sendFeedback(() -> Text.literal("- Enabled: " + profile.isEnabled()), false);
        source.sendFeedback(() -> Text.literal("- Mode: " + modeToken(profile.getMode())), false);
        source.sendFeedback(() -> Text.literal("- Action: " + actionToken(profile.getRejectedItemAction())), false);
        source.sendFeedback(() -> Text.literal("- Rule count: " + profile.getRules().size()), false);
    }

    private static StateContext resolveStateContext(ServerCommandSource source) {
        ServerPlayerEntity player;
        try {
            player = source.getPlayerOrThrow();
        } catch (Exception ex) {
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

    private record StateContext(ServerPlayerEntity player, ServerPlayerDataManager dataManager, LootLockProfile profile) {
    }
}
