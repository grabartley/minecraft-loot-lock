package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.network.ServerToClientPackets;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.server.ServerPolicyService;
import com.grahambartley.lootlock.share.ProfileShareCodec;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
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
                    .executes(ctx -> withSelfState(ctx, LootLockCommand::handleStatus)))
            .then(
                CommandManager.literal("enable")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(
                        ctx -> withSelfState(ctx, (s, state) -> handleEnable(s, state, true))))
            .then(
                CommandManager.literal("disable")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .executes(
                        ctx -> withSelfState(ctx, (s, state) -> handleEnable(s, state, false))))
            .then(
                CommandManager.literal("mode")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("denylist")
                            .executes(
                                ctx ->
                                    withSelfState(
                                        ctx,
                                        (s, state) -> handleMode(s, state, FilterMode.DENYLIST))))
                    .then(
                        CommandManager.literal("allowlist")
                            .executes(
                                ctx ->
                                    withSelfState(
                                        ctx,
                                        (s, state) -> handleMode(s, state, FilterMode.ALLOWLIST)))))
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
                                            handleAction(
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
                                                    handleAction(
                                                        s, state, RejectedItemAction.DELETE))))))
            .then(
                CommandManager.literal("profile")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("list")
                            .executes(
                                ctx -> withSelfState(ctx, LootLockCommand::handleProfileList)))
                    .then(
                        CommandManager.literal("create")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(
                                        ctx ->
                                            withSelfState(
                                                ctx,
                                                (s, state) ->
                                                    handleProfileCreate(
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
                                                    handleProfileDelete(
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
                                                    handleProfileActivate(
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
                                                    handleProfileExport(
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
                                                    handleProfileImport(
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
                                                    handleRuleAdd(s, state, ruleIdentifier(ctx)))))
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
                                                            handleRuleAddTag(
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
                                                    handleRuleRemove(
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
                                                            handleRuleRemoveTag(
                                                                s, state, tagIdentifier(ctx)))))))
                    .then(
                        CommandManager.literal("list")
                            .executes(ctx -> withSelfState(ctx, LootLockCommand::handleRuleList)))
                    .then(
                        CommandManager.literal("clear")
                            .executes(LootLockCommand::ruleClearConfirmHelp)
                            .then(
                                CommandManager.literal("confirm")
                                    .executes(
                                        ctx ->
                                            withSelfState(ctx, LootLockCommand::handleRuleClear)))))
            .then(
                CommandManager.literal("player")
                    .requires(source -> source.hasPermissionLevel(2))
                    .then(playerTargetSubtree()))
            .then(
                CommandManager.literal("policy")
                    .requires(source -> source.hasPermissionLevel(2))
                    .executes(LootLockCommand::policyStatus)
                    .then(
                        CommandManager.literal("allowDeleteRejectedItems")
                            .then(
                                CommandManager.literal("true")
                                    .executes(
                                        context -> setAllowDeleteRejectedItems(context, true)))
                            .then(
                                CommandManager.literal("false")
                                    .executes(
                                        context -> setAllowDeleteRejectedItems(context, false))))));
  }

  private static RequiredArgumentBuilder<ServerCommandSource, String> playerTargetSubtree() {
    return CommandManager.argument("target", StringArgumentType.word())
        .suggests(PLAYER_NAME_SUGGESTIONS)
        .then(
            CommandManager.literal("status")
                .executes(ctx -> withTargetState(ctx, LootLockCommand::handleStatus)))
        .then(
            CommandManager.literal("enable")
                .executes(ctx -> withTargetState(ctx, (s, state) -> handleEnable(s, state, true))))
        .then(
            CommandManager.literal("disable")
                .executes(ctx -> withTargetState(ctx, (s, state) -> handleEnable(s, state, false))))
        .then(
            CommandManager.literal("mode")
                .then(
                    CommandManager.literal("denylist")
                        .executes(
                            ctx ->
                                withTargetState(
                                    ctx, (s, state) -> handleMode(s, state, FilterMode.DENYLIST))))
                .then(
                    CommandManager.literal("allowlist")
                        .executes(
                            ctx ->
                                withTargetState(
                                    ctx,
                                    (s, state) -> handleMode(s, state, FilterMode.ALLOWLIST)))))
        .then(
            CommandManager.literal("action")
                .then(
                    CommandManager.literal("leave")
                        .executes(
                            ctx ->
                                withTargetState(
                                    ctx,
                                    (s, state) ->
                                        handleAction(
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
                                                handleAction(
                                                    s, state, RejectedItemAction.DELETE))))))
        .then(
            CommandManager.literal("profile")
                .then(
                    CommandManager.literal("list")
                        .executes(ctx -> withTargetState(ctx, LootLockCommand::handleProfileList)))
                .then(
                    CommandManager.literal("create")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                handleProfileCreate(s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("delete")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                handleProfileDelete(s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("activate")
                        .then(
                            CommandManager.argument("name", StringArgumentType.string())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                handleProfileActivate(
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
                                                handleProfileExport(s, state, profileName(ctx))))))
                .then(
                    CommandManager.literal("import")
                        .then(
                            CommandManager.argument("code", StringArgumentType.greedyString())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                handleProfileImport(s, state, shareCode(ctx)))))))
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
                                                handleRuleAdd(s, state, ruleIdentifier(ctx)))))
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
                                                        handleRuleAddTag(
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
                                                handleRuleRemove(s, state, ruleIdentifier(ctx)))))
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
                                                        handleRuleRemoveTag(
                                                            s, state, tagIdentifier(ctx)))))))
                .then(
                    CommandManager.literal("list")
                        .executes(ctx -> withTargetState(ctx, LootLockCommand::handleRuleList)))
                .then(
                    CommandManager.literal("clear")
                        .executes(LootLockCommand::ruleClearConfirmHelp)
                        .then(
                            CommandManager.literal("confirm")
                                .executes(
                                    ctx ->
                                        withTargetState(ctx, LootLockCommand::handleRuleClear)))));
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

  private static int policyStatus(CommandContext<ServerCommandSource> context) {
    boolean current = LootLock.SERVER_CONFIG.allowDeleteRejectedItems();
    context
        .getSource()
        .sendFeedback(() -> Text.translatable(LootLockLang.COMMAND_POLICY_STATUS, current), false);
    return 1;
  }

  private static int setAllowDeleteRejectedItems(
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

  private static int deleteConfirmHelp(CommandContext<ServerCommandSource> context) {
    sendKey(context.getSource(), LootLockLang.COMMAND_DELETE_CONFIRM_HELP);
    return 1;
  }

  private static int handleStatus(ServerCommandSource source, StateContext state) {
    sendStatus(source, state);
    return 1;
  }

  private static int handleProfileList(ServerCommandSource source, StateContext state) {
    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_LIST_HEADER_SELF)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_LIST_HEADER_TARGET, state.displayName());
    source.sendFeedback(() -> header, false);
    for (LootLockProfile profile : state.data().getProfiles()) {
      if (profile == null) {
        continue;
      }
      String rowKey =
          profile.getId().equals(state.data().getActiveProfileId())
              ? LootLockLang.COMMAND_PROFILE_LIST_ROW_ACTIVE
              : LootLockLang.COMMAND_PROFILE_LIST_ROW_INACTIVE;
      String name = profile.getName();
      source.sendFeedback(() -> Text.translatable(rowKey, name), false);
    }
    return 1;
  }

  private static int handleProfileCreate(
      ServerCommandSource source, StateContext state, String requestedName) {
    String profileName = normalizeProfileName(requestedName);
    if (profileName == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NAME_LENGTH));
      return 0;
    }

    if (!canCreateProfile(state.data())) {
      Text error =
          state.isSelfTargeted()
              ? Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_SELF, PacketLimits.MAX_PROFILES)
              : Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_TARGET,
                  state.displayName(),
                  PacketLimits.MAX_PROFILES);
      source.sendError(error);
      return 0;
    }

    if (findProfileByName(state.data(), profileName).isPresent()) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_DUPLICATE));
      return 0;
    }

    LootLockProfile created = createProfileWithDefaults(profileName);
    appendProfile(state.data(), created);
    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_CREATE_SELF, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_CREATE_TARGET, profileName, state.displayName());
    source.sendFeedback(() -> message, false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleProfileExport(
      ServerCommandSource source, StateContext state, String requestedName) {
    Optional<LootLockProfile> found = findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(
          Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    String code = ProfileShareCodec.encode(target);
    String profileNameValue = target.getName();
    int ruleCount = target.getRules().size();
    int codeLength = code.length();

    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_EXPORT_HEADER_SELF, profileNameValue)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_EXPORT_HEADER_TARGET,
                profileNameValue,
                state.displayName());
    source.sendFeedback(() -> header, false);

    Style codeStyle =
        Style.EMPTY
            .withFormatting(Formatting.YELLOW)
            .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, code))
            .withHoverEvent(
                new HoverEvent(
                    HoverEvent.Action.SHOW_TEXT,
                    Text.translatable(LootLockLang.COMMAND_PROFILE_EXPORT_HOVER)));
    source.sendFeedback(() -> Text.literal(code).setStyle(codeStyle), false);

    String summaryKey =
        ruleCount == 1
            ? LootLockLang.COMMAND_PROFILE_EXPORT_SUMMARY_ONE
            : LootLockLang.COMMAND_PROFILE_EXPORT_SUMMARY_MANY;
    source.sendFeedback(() -> Text.translatable(summaryKey, ruleCount, codeLength), false);
    return 1;
  }

  private static int handleProfileImport(
      ServerCommandSource source, StateContext state, String rawCode) {
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(rawCode);
    if (result instanceof ProfileShareCodec.DecodeResult.Err err) {
      source.sendError(Text.translatable(shareCodeErrorKey(err.reason())));
      return 0;
    }
    ProfileShareCodec.DecodeResult.Ok ok = (ProfileShareCodec.DecodeResult.Ok) result;
    LootLockProfile decoded = ok.profile();

    if (!canCreateProfile(state.data())) {
      Text error =
          state.isSelfTargeted()
              ? Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_SELF, PacketLimits.MAX_PROFILES)
              : Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_MAX_TARGET,
                  state.displayName(),
                  PacketLimits.MAX_PROFILES);
      source.sendError(error);
      return 0;
    }

    String requestedName = decoded.getName();
    boolean renamed = findProfileByName(state.data(), requestedName).isPresent();
    String finalName = renamed ? nextAvailableName(state.data(), requestedName) : requestedName;
    String normalizedName = normalizeProfileName(finalName);
    if (normalizedName == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NAME_LENGTH));
      return 0;
    }

    LootLockProfile created =
        new LootLockProfile(
            UUID.randomUUID(),
            normalizedName,
            decoded.getMode(),
            decoded.getRejectedItemAction(),
            true,
            0,
            new ArrayList<>(decoded.getRules()));
    appendProfile(state.data(), created);
    markDirty(source, state);

    int ruleCount = created.getRules().size();
    if (renamed) {
      Text feedback =
          Text.translatable(
              LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_RENAMED,
              normalizedName,
              ruleCount,
              requestedName);
      source.sendFeedback(() -> feedback, false);
    } else {
      Text feedback =
          state.isSelfTargeted()
              ? Text.translatable(
                  LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_SELF, normalizedName, ruleCount)
              : Text.translatable(
                  LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_TARGET,
                  normalizedName,
                  ruleCount,
                  state.displayName());
      source.sendFeedback(() -> feedback, false);
    }
    syncIfOnline(state);
    return 1;
  }

  static String shareCodeErrorKey(String reason) {
    return switch (reason) {
      case "empty" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_EMPTY;
      case "too_long" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_TOO_LONG;
      case "bad_prefix" -> LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PREFIX;
      case "bad_base64", "bad_deflate", "bad_json", "bad_version" ->
          LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PAYLOAD;
      default -> LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_FIELD;
    };
  }

  private static int handleProfileDelete(
      ServerCommandSource source, StateContext state, String requestedName) {
    if (state.data().getProfiles().size() <= 1) {
      Text error =
          state.isSelfTargeted()
              ? Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_LAST_SELF)
              : Text.translatable(
                  LootLockLang.COMMAND_ERROR_PROFILE_LAST_TARGET, state.displayName());
      source.sendError(error);
      return 0;
    }

    Optional<LootLockProfile> found = findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(
          Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    String targetName = target.getName();
    removeProfileById(state.data(), target.getId());
    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_DELETE_SELF, targetName)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_DELETE_TARGET, targetName, state.displayName());
    source.sendFeedback(() -> message, false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleProfileActivate(
      ServerCommandSource source, StateContext state, String requestedName) {
    Optional<LootLockProfile> found = findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(
          Text.translatable(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    String targetName = target.getName();
    state.data().setActiveProfileId(target.getId());
    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_PROFILE_ACTIVATE_SELF, targetName)
            : Text.translatable(
                LootLockLang.COMMAND_PROFILE_ACTIVATE_TARGET, targetName, state.displayName());
    source.sendFeedback(() -> message, false);
    sendStatus(source, state.withProfile(target));
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleAdd(
      ServerCommandSource source, StateContext state, Identifier itemId) {
    if (!Registries.ITEM.containsId(itemId)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_UNKNOWN_ITEM, itemId));
      return 0;
    }

    String token = itemId.toString();
    if (!addRuleToProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_EXISTS, token));
      return 0;
    }

    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_ADD_SELF, token)
            : Text.translatable(LootLockLang.COMMAND_RULE_ADD_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleRemove(
      ServerCommandSource source, StateContext state, Identifier itemId) {
    String token = itemId.toString();
    if (!removeRuleFromProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_NOT_FOUND, token));
      return 0;
    }

    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_REMOVE_SELF, token)
            : Text.translatable(
                LootLockLang.COMMAND_RULE_REMOVE_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleAddTag(
      ServerCommandSource source, StateContext state, Identifier tagId) {
    String token = RuleEntry.TAG_PREFIX + tagId;
    if (!addRuleToProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_EXISTS, token));
      return 0;
    }
    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_ADD_SELF, token)
            : Text.translatable(LootLockLang.COMMAND_RULE_ADD_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    if (!tagExists(tagId)) {
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_RULE_TAG_UNKNOWN_WARNING, token), false);
    }
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleRemoveTag(
      ServerCommandSource source, StateContext state, Identifier tagId) {
    String token = RuleEntry.TAG_PREFIX + tagId;
    if (!removeRuleFromProfile(state.profile(), token)) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_RULE_NOT_FOUND, token));
      return 0;
    }
    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_REMOVE_SELF, token)
            : Text.translatable(
                LootLockLang.COMMAND_RULE_REMOVE_TARGET, token, state.displayName());
    source.sendFeedback(() -> message, false);
    syncIfOnline(state);
    return 1;
  }

  static boolean tagExists(Identifier tagId) {
    if (tagId == null) {
      return false;
    }
    return Registries.ITEM
        .getEntryList(net.minecraft.registry.tag.TagKey.of(RegistryKeys.ITEM, tagId))
        .isPresent();
  }

  private static int handleRuleList(ServerCommandSource source, StateContext state) {
    if (state.profile().getRules().isEmpty()) {
      Text message =
          state.isSelfTargeted()
              ? Text.translatable(LootLockLang.COMMAND_RULE_LIST_EMPTY_SELF)
              : Text.translatable(LootLockLang.COMMAND_RULE_LIST_EMPTY_TARGET, state.displayName());
      source.sendFeedback(() -> message, false);
      return 1;
    }

    String profileName = state.profile().getName();
    Text header =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_LIST_HEADER_SELF, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_RULE_LIST_HEADER_TARGET, state.displayName(), profileName);
    source.sendFeedback(() -> header, false);
    int invalidRules = 0;
    for (RuleEntry rule : state.profile().getRules()) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        invalidRules++;
        continue;
      }
      String ruleId = rule.itemId();
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_RULE_LIST_ROW, ruleId), false);
    }
    if (invalidRules > 0) {
      int invalidRuleCount = invalidRules;
      source.sendFeedback(
          () -> Text.translatable(LootLockLang.COMMAND_RULE_LIST_INVALID, invalidRuleCount), false);
    }
    return 1;
  }

  private static int ruleClearConfirmHelp(CommandContext<ServerCommandSource> context) {
    sendKey(context.getSource(), LootLockLang.COMMAND_RULE_CLEAR_HINT);
    return 1;
  }

  private static int handleRuleClear(ServerCommandSource source, StateContext state) {
    clearRulesOnProfile(state.profile());
    markDirty(source, state);
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_RULE_CLEAR_SELF)
            : Text.translatable(LootLockLang.COMMAND_RULE_CLEAR_TARGET, state.displayName());
    source.sendFeedback(() -> message, false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleEnable(ServerCommandSource source, StateContext state, boolean enabled) {
    applyGlobalEnable(state.data(), enabled);
    markDirty(source, state);
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
    syncIfOnline(state);
    return 1;
  }

  private static int handleMode(ServerCommandSource source, StateContext state, FilterMode mode) {
    state.profile().setMode(mode);
    markDirty(source, state);
    Text modeLabel = modeLabel(mode);
    String profileName = state.profile().getName();
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_MODE_SELF, modeLabel, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_MODE_TARGET, modeLabel, state.displayName(), profileName);
    source.sendFeedback(() -> message, false);
    sendStatus(source, state);
    syncIfOnline(state);
    return 1;
  }

  private static int handleAction(
      ServerCommandSource source, StateContext state, RejectedItemAction action) {
    RejectedItemAction normalizedAction =
        normalizeRejectedItemAction(action, LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
    if (action == RejectedItemAction.DELETE && normalizedAction != RejectedItemAction.DELETE) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_DELETE_POLICY_BLOCKED));
      return 0;
    }

    state.profile().setRejectedItemAction(normalizedAction);
    markDirty(source, state);
    Text actionLabel = actionLabel(normalizedAction);
    String profileName = state.profile().getName();
    Text message =
        state.isSelfTargeted()
            ? Text.translatable(LootLockLang.COMMAND_ACTION_SELF, actionLabel, profileName)
            : Text.translatable(
                LootLockLang.COMMAND_ACTION_TARGET, actionLabel, state.displayName(), profileName);
    source.sendFeedback(() -> message, false);
    if (normalizedAction == RejectedItemAction.DELETE) {
      sendKey(source, LootLockLang.COMMAND_ACTION_DELETE_WARNING);
    }
    sendStatus(source, state);
    syncIfOnline(state);
    return 1;
  }

  private static void sendStatus(ServerCommandSource source, StateContext state) {
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

  private static int withSelfState(CommandContext<ServerCommandSource> context, StateAction action)
      throws CommandSyntaxException {
    StateContext state = resolveSelfState(context.getSource());
    if (state == null) {
      return 0;
    }
    return action.apply(context.getSource(), state);
  }

  private static int withTargetState(
      CommandContext<ServerCommandSource> context, StateAction action)
      throws CommandSyntaxException {
    StateContext state = resolveTargetState(context);
    if (state == null) {
      return 0;
    }
    return action.apply(context.getSource(), state);
  }

  private static StateContext resolveSelfState(ServerCommandSource source) {
    ServerPlayerEntity player;
    try {
      player = source.getPlayerOrThrow();
    } catch (CommandSyntaxException ex) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PLAYER_ONLY));
      return null;
    }
    return buildState(source, player.getUuid(), player.getGameProfile().getName(), player, true);
  }

  private static StateContext resolveTargetState(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String input = StringArgumentType.getString(context, "target");
    TargetContext target = resolveTarget(source, input);
    if (target == null) {
      return null;
    }
    return buildState(source, target.uuid(), target.displayName(), target.online(), false);
  }

  private static StateContext buildState(
      ServerCommandSource source,
      UUID uuid,
      String displayName,
      ServerPlayerEntity online,
      boolean isSelfTargeted) {
    ServerPlayerDataManager dataManager = LootLock.PLAYER_DATA_MANAGER;
    if (dataManager == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_NOT_READY));
      return null;
    }

    LootLockPlayerData data = dataManager.getOrLoad(uuid);
    LootLockProfile profile = data.getActiveProfile().orElse(null);
    if (profile == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_NO_ACTIVE_PROFILE));
      return null;
    }

    return new StateContext(uuid, displayName, online, isSelfTargeted, dataManager, data, profile);
  }

  static TargetContext resolveTarget(ServerCommandSource source, String input) {
    MinecraftServer server = source.getServer();
    if (server == null) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_SERVER_NOT_READY));
      return null;
    }

    ServerPlayerEntity online = server.getPlayerManager().getPlayer(input);
    if (online != null) {
      return new TargetContext(online.getUuid(), online.getGameProfile().getName(), online);
    }

    Optional<GameProfile> cached =
        server.getUserCache() == null ? Optional.empty() : server.getUserCache().findByName(input);
    if (cached.isPresent() && cached.get().getId() != null) {
      return new TargetContext(cached.get().getId(), cached.get().getName(), null);
    }

    Optional<UUID> parsed = tryParseUuid(input);
    if (parsed.isPresent()) {
      UUID uuid = parsed.get();
      String displayName =
          server.getUserCache() == null
              ? input
              : server.getUserCache().getByUuid(uuid).map(GameProfile::getName).orElse(input);
      ServerPlayerEntity onlineByUuid = server.getPlayerManager().getPlayer(uuid);
      return new TargetContext(uuid, displayName, onlineByUuid);
    }

    source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_UNKNOWN_PLAYER, input));
    return null;
  }

  static Optional<UUID> tryParseUuid(String input) {
    if (input == null || input.isBlank()) {
      return Optional.empty();
    }
    try {
      return Optional.of(UUID.fromString(input));
    } catch (IllegalArgumentException ex) {
      return Optional.empty();
    }
  }

  private static void markDirty(ServerCommandSource source, StateContext state) {
    if (state.onlineTarget() != null) {
      state.dataManager().markDirty(state.onlineTarget());
    } else {
      MinecraftServer server = source.getServer();
      long tick = server != null ? server.getTicks() : 0L;
      state.dataManager().markDirty(state.targetUuid(), tick);
    }
  }

  private static void syncIfOnline(StateContext state) {
    if (state.onlineTarget() != null) {
      ServerToClientPackets.sendAuthoritativeSync(state.onlineTarget());
    }
  }

  static boolean canCreateProfile(LootLockPlayerData data) {
    return data != null && data.getProfiles().size() < PacketLimits.MAX_PROFILES;
  }

  static String normalizeProfileName(String raw) {
    if (raw == null) {
      return null;
    }

    String normalized = raw.trim();
    if (normalized.isEmpty() || normalized.length() > 32) {
      return null;
    }

    return normalized;
  }

  static String nextAvailableName(LootLockPlayerData data, String sourceName) {
    String base = normalizeProfileName(sourceName);
    if (base == null) {
      base = "Profile";
    }
    if (findProfileByName(data, base).isEmpty()) {
      return base;
    }
    for (int suffix = 2; suffix <= 999; suffix++) {
      String tail = " (" + suffix + ")";
      String prefix =
          base.length() + tail.length() <= 32 ? base : base.substring(0, 32 - tail.length());
      String candidate = prefix + tail;
      if (findProfileByName(data, candidate).isEmpty()) {
        return candidate;
      }
    }
    String tail = " copy";
    String prefix =
        base.length() + tail.length() <= 32 ? base : base.substring(0, 32 - tail.length());
    return prefix + tail;
  }

  static Optional<LootLockProfile> findProfileByName(LootLockPlayerData data, String name) {
    String normalized = normalizeProfileName(name);
    if (normalized == null) {
      return Optional.empty();
    }

    for (LootLockProfile profile : data.getProfiles()) {
      if (profile != null && normalized.equalsIgnoreCase(profile.getName())) {
        return Optional.of(profile);
      }
    }

    return Optional.empty();
  }

  static boolean containsRule(LootLockProfile profile, String itemId) {
    for (RuleEntry rule : profile.getRules()) {
      if (rule != null && itemId.equals(rule.itemId())) {
        return true;
      }
    }
    return false;
  }

  static RejectedItemAction normalizeRejectedItemAction(
      RejectedItemAction action, boolean allowDeleteRejectedItems) {
    if (!allowDeleteRejectedItems && action == RejectedItemAction.DELETE) {
      return RejectedItemAction.LEAVE_ON_GROUND;
    }
    return action == null ? RejectedItemAction.LEAVE_ON_GROUND : action;
  }

  static void applyGlobalEnable(LootLockPlayerData data, boolean enabled) {
    data.setEnabledForAll(enabled);
  }

  static LootLockProfile createProfileWithDefaults(String profileName) {
    return new LootLockProfile(
        UUID.randomUUID(),
        profileName,
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        List.of());
  }

  static void appendProfile(LootLockPlayerData data, LootLockProfile profile) {
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.add(profile);
    data.setProfiles(profiles);
  }

  static void removeProfileById(LootLockPlayerData data, UUID profileId) {
    List<LootLockProfile> profiles = new ArrayList<>(data.getProfiles());
    profiles.removeIf(profile -> profile != null && profile.getId().equals(profileId));
    data.setProfiles(profiles);
    if (profileId.equals(data.getActiveProfileId()) && !profiles.isEmpty()) {
      data.setActiveProfileId(profiles.get(0).getId());
    }
  }

  static boolean addRuleToProfile(LootLockProfile profile, String itemId) {
    if (containsRule(profile, itemId)) {
      return false;
    }
    List<RuleEntry> rules = new ArrayList<>(profile.getRules());
    rules.add(new RuleEntry(itemId));
    profile.setRules(rules);
    return true;
  }

  static boolean removeRuleFromProfile(LootLockProfile profile, String itemId) {
    List<RuleEntry> rules = new ArrayList<>(profile.getRules());
    boolean removed = rules.removeIf(rule -> rule != null && itemId.equals(rule.itemId()));
    if (removed) {
      profile.setRules(rules);
    }
    return removed;
  }

  static void clearRulesOnProfile(LootLockProfile profile) {
    profile.setRules(List.of());
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
    int apply(ServerCommandSource source, StateContext state) throws CommandSyntaxException;
  }

  record TargetContext(UUID uuid, String displayName, ServerPlayerEntity online) {}

  record StateContext(
      UUID targetUuid,
      String displayName,
      ServerPlayerEntity onlineTarget,
      boolean isSelfTargeted,
      ServerPlayerDataManager dataManager,
      LootLockPlayerData data,
      LootLockProfile profile) {

    StateContext withProfile(LootLockProfile newProfile) {
      return new StateContext(
          targetUuid, displayName, onlineTarget, isSelfTargeted, dataManager, data, newProfile);
    }
  }
}
