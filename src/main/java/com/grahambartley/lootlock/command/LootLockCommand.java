package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class LootLockCommand {
  private LootLockCommand() {}

  private static final SuggestionProvider<ServerCommandSource> PLAYER_NAME_SUGGESTIONS =
      (context, builder) ->
          CommandSource.suggestMatching(context.getSource().getPlayerNames(), builder);

  public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
    dispatcher.register(
        CommandManager.literal("lootlock")
            .executes(LootLockCommand::help)
            .then(
                CommandManager.literal("status")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(ctx -> withSelfState(ctx, SettingsCommandHandlers::handleStatus)))
            .then(
                CommandManager.literal("enable")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(
                        ctx ->
                            withSelfState(
                                ctx,
                                (s, state) ->
                                    SettingsCommandHandlers.handleEnable(s, state, true))))
            .then(
                CommandManager.literal("disable")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(
                        ctx ->
                            withSelfState(
                                ctx,
                                (s, state) ->
                                    SettingsCommandHandlers.handleEnable(s, state, false))))
            .then(
                CommandManager.literal("mode")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("denylist")
                            .executes(
                                ctx ->
                                    withSelfState(
                                        ctx,
                                        (s, state) ->
                                            SettingsCommandHandlers.handleMode(
                                                s, state, FilterMode.DENYLIST))))
                    .then(
                        CommandManager.literal("allowlist")
                            .executes(
                                ctx ->
                                    withSelfState(
                                        ctx,
                                        (s, state) ->
                                            SettingsCommandHandlers.handleMode(
                                                s, state, FilterMode.ALLOWLIST)))))
            .then(
                CommandManager.literal("action")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("leave")
                            .executes(
                                ctx ->
                                    withSelfState(
                                        ctx,
                                        (s, state) ->
                                            SettingsCommandHandlers.handleAction(
                                                s, state, RejectedItemAction.LEAVE_ON_GROUND))))
                    .then(
                        CommandManager.literal("delete")
                            .executes(LootLockCommand::deleteConfirmHelp)
                            .then(
                                CommandManager.literal("confirm")
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    SettingsCommandHandlers.handleAction(
                                                        s, state, RejectedItemAction.DELETE))))))
            .then(
                CommandManager.literal("profile")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("list")
                            .executes(
                                ctx ->
                                    withSelfState(ctx, ProfileCommandHandlers::handleProfileList)))
                    .then(
                        CommandManager.literal("create")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    ProfileCommandHandlers.handleProfileCreate(
                                                        s, state, profileName(ctx))))))
                    .then(
                        CommandManager.literal("delete")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    ProfileCommandHandlers.handleProfileDelete(
                                                        s, state, profileName(ctx))))))
                    .then(
                        CommandManager.literal("activate")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    ProfileCommandHandlers.handleProfileActivate(
                                                        s, state, profileName(ctx))))))
                    .then(
                        CommandManager.literal("export")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    ProfileCommandHandlers.handleProfileExport(
                                                        s, state, profileName(ctx))))))
                    .then(
                        CommandManager.literal("import")
                            .then(
                                CommandManager.argument("code", StringArgumentType.greedyString())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    ProfileCommandHandlers.handleProfileImport(
                                                        s, state, shareCode(ctx)))))))
            .then(
                CommandManager.literal("rule")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("add")
                            .then(
                                CommandManager.argument("item", IdentifierArgumentType.identifier())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    RuleCommandHandlers.handleRuleAdd(
                                                        s, state, ruleIdentifier(ctx)))))
                            .then(
                                CommandManager.literal("tag")
                                    .then(
                                        CommandManager.argument(
                                                "tag", IdentifierArgumentType.identifier())
                                            .executes(
                                                ctx ->
                                                    withSelfState(
                                                        ctx,
                                                        (s, state) ->
                                                            RuleCommandHandlers.handleRuleAddTag(
                                                                s, state, tagIdentifier(ctx)))))))
                    .then(
                        CommandManager.literal("remove")
                            .then(
                                CommandManager.argument("item", IdentifierArgumentType.identifier())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    RuleCommandHandlers.handleRuleRemove(
                                                        s, state, ruleIdentifier(ctx)))))
                            .then(
                                CommandManager.literal("tag")
                                    .then(
                                        CommandManager.argument(
                                                "tag", IdentifierArgumentType.identifier())
                                            .executes(
                                                ctx ->
                                                    withSelfState(
                                                        ctx,
                                                        (s, state) ->
                                                            RuleCommandHandlers.handleRuleRemoveTag(
                                                                s, state, tagIdentifier(ctx)))))))
                    .then(
                        CommandManager.literal("list")
                            .executes(
                                ctx -> withSelfState(ctx, RuleCommandHandlers::handleRuleList)))
                    .then(
                        CommandManager.literal("clear")
                            .executes(LootLockCommand::ruleClearConfirmHelp)
                            .then(
                                CommandManager.literal("confirm")
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx, RuleCommandHandlers::handleRuleClear)))))
            .then(
                CommandManager.literal("player")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(playerTargetSubtree()))
            .then(
                CommandManager.literal("policy")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(SettingsCommandHandlers::policyStatus)
                    .then(
                        CommandManager.literal("allowDeleteRejectedItems")
                            .then(
                                CommandManager.literal("true")
                                    .executes(
                                        context ->
                                            SettingsCommandHandlers.setAllowDeleteRejectedItems(
                                                context, true)))
                            .then(
                                CommandManager.literal("false")
                                    .executes(
                                        context ->
                                            SettingsCommandHandlers.setAllowDeleteRejectedItems(
                                                context, false))))));
  }

  private static RequiredArgumentBuilder<ServerCommandSource, String> playerTargetSubtree() {
    return CommandManager.argument("target", StringArgumentType.word())
        .suggests(PLAYER_NAME_SUGGESTIONS)
        .then(
            CommandManager.literal("status")
                .executes(ctx -> withTargetState(ctx, SettingsCommandHandlers::handleStatus)))
        .then(
            CommandManager.literal("enable")
                .executes(
                    ctx ->
                        withTargetState(
                            ctx,
                            (s, state) -> SettingsCommandHandlers.handleEnable(s, state, true))))
        .then(
            CommandManager.literal("disable")
                .executes(
                    ctx ->
                        withTargetState(
                            ctx,
                            (s, state) -> SettingsCommandHandlers.handleEnable(s, state, false))))
        .then(
            CommandManager.literal("mode")
                .then(
                    CommandManager.literal("denylist")
                        .executes(
                            ctx ->
                                withTargetState(
                                    ctx,
                                    (s, state) ->
                                        SettingsCommandHandlers.handleMode(
                                            s, state, FilterMode.DENYLIST))))
                .then(
                    CommandManager.literal("allowlist")
                        .executes(
                            ctx ->
                                withTargetState(
                                    ctx,
                                    (s, state) ->
                                        SettingsCommandHandlers.handleMode(
                                            s, state, FilterMode.ALLOWLIST)))))
        .then(
            CommandManager.literal("action")
                .then(
                    CommandManager.literal("leave")
                        .executes(
                            ctx ->
                                withTargetState(
                                    ctx,
                                    (s, state) ->
                                        SettingsCommandHandlers.handleAction(
                                            s, state, RejectedItemAction.LEAVE_ON_GROUND))))
                .then(
                    CommandManager.literal("delete")
                        .executes(LootLockCommand::deleteConfirmHelp)
                        .then(
                            CommandManager.literal("confirm")
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                SettingsCommandHandlers.handleAction(
                                                    s, state, RejectedItemAction.DELETE))))))
        .then(
            CommandManager.literal("profile")
                .then(
                    CommandManager.literal("list")
                        .executes(
                            ctx -> withTargetState(ctx, ProfileCommandHandlers::handleProfileList)))
                .then(
                    CommandManager.literal("create")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                ProfileCommandHandlers.handleProfileCreate(
                                                    s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("delete")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                ProfileCommandHandlers.handleProfileDelete(
                                                    s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("activate")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                ProfileCommandHandlers.handleProfileActivate(
                                                    s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("export")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                ProfileCommandHandlers.handleProfileExport(
                                                    s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("import")
                        .then(
                            CommandManager.argument("code", StringArgumentType.greedyString())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                ProfileCommandHandlers.handleProfileImport(
                                                    s, state, shareCode(ctx)))))))
        .then(
            CommandManager.literal("rule")
                .then(
                    CommandManager.literal("add")
                        .then(
                            CommandManager.argument("item", IdentifierArgumentType.identifier())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                RuleCommandHandlers.handleRuleAdd(
                                                    s, state, ruleIdentifier(ctx)))))
                        .then(
                            CommandManager.literal("tag")
                                .then(
                                    CommandManager.argument(
                                            "tag", IdentifierArgumentType.identifier())
                                        .executes(
                                            ctx ->
                                                withTargetState(
                                                    ctx,
                                                    (s, state) ->
                                                        RuleCommandHandlers.handleRuleAddTag(
                                                            s, state, tagIdentifier(ctx)))))))
                .then(
                    CommandManager.literal("remove")
                        .then(
                            CommandManager.argument("item", IdentifierArgumentType.identifier())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                RuleCommandHandlers.handleRuleRemove(
                                                    s, state, ruleIdentifier(ctx)))))
                        .then(
                            CommandManager.literal("tag")
                                .then(
                                    CommandManager.argument(
                                            "tag", IdentifierArgumentType.identifier())
                                        .executes(
                                            ctx ->
                                                withTargetState(
                                                    ctx,
                                                    (s, state) ->
                                                        RuleCommandHandlers.handleRuleRemoveTag(
                                                            s, state, tagIdentifier(ctx)))))))
                .then(
                    CommandManager.literal("list")
                        .executes(ctx -> withTargetState(ctx, RuleCommandHandlers::handleRuleList)))
                .then(
                    CommandManager.literal("clear")
                        .executes(LootLockCommand::ruleClearConfirmHelp)
                        .then(
                            CommandManager.literal("confirm")
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx, RuleCommandHandlers::handleRuleClear)))));
  }

  static String modeToken(FilterMode mode) {
    return mode == FilterMode.ALLOWLIST ? "allowlist" : "denylist";
  }

  static String actionToken(RejectedItemAction action) {
    return action == RejectedItemAction.DELETE ? "delete" : "leave";
  }

  private static int help(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    sendKey(source, LootLockLang.COMMAND_HELP_HEADER);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_STATUS);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_ENABLE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_DISABLE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_PROFILE_LIST);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_PROFILE_CREATE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_PROFILE_DELETE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_PROFILE_ACTIVATE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_PROFILE_EXPORT);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_PROFILE_IMPORT);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_MODE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_ACTION_LEAVE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_ACTION_DELETE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_RULE_ADD);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_RULE_ADD_TAG);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_RULE_REMOVE);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_RULE_REMOVE_TAG);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_RULE_LIST);
    sendKey(source, LootLockLang.COMMAND_HELP_LINE_RULE_CLEAR);
    if (source.hasPermissionLevel(2)) {
      sendKey(source, LootLockLang.COMMAND_HELP_LINE_PLAYER);
      sendKey(source, LootLockLang.COMMAND_HELP_LINE_POLICY);
    }
    return 1;
  }

  private static void sendKey(ServerCommandSource source, String key) {
    source.sendFeedback(() -> Text.translatable(key), false);
  }

  private static int deleteConfirmHelp(CommandContext<ServerCommandSource> context) {
    sendKey(context.getSource(), LootLockLang.COMMAND_DELETE_CONFIRM_HELP);
    return 1;
  }

  private static int ruleClearConfirmHelp(CommandContext<ServerCommandSource> context) {
    sendKey(context.getSource(), LootLockLang.COMMAND_RULE_CLEAR_HINT);
    return 1;
  }

  private static int withSelfState(CommandContext<ServerCommandSource> context, StateAction action)
      throws CommandSyntaxException {
    CommandStateContext state = CommandTargetResolver.resolveSelfState(context.getSource());
    if (state == null) {
      return 0;
    }
    return action.apply(context.getSource(), state);
  }

  private static int withTargetState(
      CommandContext<ServerCommandSource> context, StateAction action)
      throws CommandSyntaxException {
    CommandStateContext state = CommandTargetResolver.resolveTargetState(context);
    if (state == null) {
      return 0;
    }
    return action.apply(context.getSource(), state);
  }

  private static String profileName(CommandContext<ServerCommandSource> context) {
    return StringArgumentType.getString(context, "name");
  }

  private static String shareCode(CommandContext<ServerCommandSource> context) {
    return StringArgumentType.getString(context, "code");
  }

  private static Identifier ruleIdentifier(CommandContext<ServerCommandSource> context)
      throws CommandSyntaxException {
    return IdentifierArgumentType.getIdentifier(context, "item");
  }

  private static Identifier tagIdentifier(CommandContext<ServerCommandSource> context)
      throws CommandSyntaxException {
    return IdentifierArgumentType.getIdentifier(context, "tag");
  }

  @FunctionalInterface
  private interface StateAction {
    int apply(ServerCommandSource source, CommandStateContext state) throws CommandSyntaxException;
  }
}
