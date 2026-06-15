package com.grahambartley.command;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import com.grahambartley.network.PacketLimits;
import com.grahambartley.network.ServerToClientPackets;
import com.grahambartley.server.ServerPlayerDataManager;
import com.grahambartley.server.ServerPolicyService;
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
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
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
                                                        s, state, profileName(ctx)))))))
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
                                                    handleRuleAdd(s, state, ruleIdentifier(ctx))))))
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
                                                        s, state, ruleIdentifier(ctx))))))
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
                                                    s, state, profileName(ctx)))))))
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
                                                handleRuleAdd(s, state, ruleIdentifier(ctx))))))
                .then(
                    CommandManager.literal("remove")
                        .then(
                            CommandManager.argument("item", IdentifierArgumentType.identifier())
                                .executes(
                                    ctx ->
                                        withTargetState(
                                            ctx,
                                            (s, state) ->
                                                handleRuleRemove(s, state, ruleIdentifier(ctx))))))
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
    source.sendFeedback(() -> Text.literal("Loot Lock commands:"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock status"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock enable"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock disable"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock profile list"), false);
    source.sendFeedback(
        () -> Text.literal("- /lootlock profile create <name|\"name with spaces\">"), false);
    source.sendFeedback(
        () -> Text.literal("- /lootlock profile delete <name|\"name with spaces\">"), false);
    source.sendFeedback(
        () -> Text.literal("- /lootlock profile activate <name|\"name with spaces\">"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock mode denylist|allowlist"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock action leave"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock action delete confirm"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock rule add <item>"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock rule remove <item>"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock rule list"), false);
    source.sendFeedback(() -> Text.literal("- /lootlock rule clear confirm"), false);
    if (source.hasPermissionLevel(2)) {
      source.sendFeedback(
          () -> Text.literal("- /lootlock player <name|uuid> <subcommand> (operator)"), false);
      source.sendFeedback(
          () -> Text.literal("- /lootlock policy allowDeleteRejectedItems true|false"), false);
    }
    return 1;
  }

  private static int policyStatus(CommandContext<ServerCommandSource> context) {
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.literal(
                    "allowDeleteRejectedItems="
                        + LootLock.SERVER_CONFIG.allowDeleteRejectedItems()),
            false);
    return 1;
  }

  private static int setAllowDeleteRejectedItems(
      CommandContext<ServerCommandSource> context, boolean allowDeleteRejectedItems) {
    boolean updated =
        ServerPolicyService.updateAllowDeleteRejectedItems(
            context.getSource().getServer(), allowDeleteRejectedItems);
    if (!updated) {
      context.getSource().sendError(Text.literal("Failed to persist server policy update."));
      return 0;
    }
    for (ServerPlayerEntity player :
        context.getSource().getServer().getPlayerManager().getPlayerList()) {
      ServerToClientPackets.sendAuthoritativeSync(player);
    }
    context
        .getSource()
        .sendFeedback(
            () -> Text.literal("allowDeleteRejectedItems set to " + allowDeleteRejectedItems),
            true);
    return 1;
  }

  private static int deleteConfirmHelp(CommandContext<ServerCommandSource> context) {
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.literal(
                    "Delete mode permanently removes rejected dropped items. Add 'confirm' to proceed."),
            false);
    return 1;
  }

  private static int handleStatus(ServerCommandSource source, StateContext state) {
    sendStatus(source, state);
    return 1;
  }

  private static int handleProfileList(ServerCommandSource source, StateContext state) {
    String header =
        state.isSelfTargeted()
            ? "Loot Lock profiles:"
            : "Loot Lock profiles for " + state.displayName() + ":";
    source.sendFeedback(() -> Text.literal(header), false);
    for (LootLockProfile profile : state.data().getProfiles()) {
      if (profile == null) {
        continue;
      }
      String marker = profile.getId().equals(state.data().getActiveProfileId()) ? "* " : "- ";
      source.sendFeedback(() -> Text.literal(marker + profile.getName()), false);
    }
    return 1;
  }

  private static int handleProfileCreate(
      ServerCommandSource source, StateContext state, String requestedName) {
    String profileName = normalizeProfileName(requestedName);
    if (profileName == null) {
      source.sendError(Text.literal("Profile name must be between 1 and 32 characters."));
      return 0;
    }

    if (!canCreateProfile(state.data())) {
      String subject =
          state.isSelfTargeted() ? "You already have " : state.displayName() + " already has ";
      source.sendError(
          Text.literal(
              subject
                  + PacketLimits.MAX_PROFILES
                  + " profiles, which is the maximum. Delete one before creating another."));
      return 0;
    }

    if (findProfileByName(state.data(), profileName).isPresent()) {
      source.sendError(Text.literal("A profile with that name already exists."));
      return 0;
    }

    LootLockProfile created = createProfileWithDefaults(profileName);
    appendProfile(state.data(), created);
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Created profile '" + profileName + "'."
            : "Created profile '" + profileName + "' for " + state.displayName() + ".";
    source.sendFeedback(() -> Text.literal(message), false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleProfileDelete(
      ServerCommandSource source, StateContext state, String requestedName) {
    if (state.data().getProfiles().size() <= 1) {
      String message =
          state.isSelfTargeted()
              ? "You cannot delete your last profile."
              : "Cannot delete " + state.displayName() + "'s last profile.";
      source.sendError(Text.literal(message));
      return 0;
    }

    Optional<LootLockProfile> found = findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(Text.literal("Profile not found: " + requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    removeProfileById(state.data(), target.getId());
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Deleted profile '" + target.getName() + "'."
            : "Deleted profile '" + target.getName() + "' for " + state.displayName() + ".";
    source.sendFeedback(() -> Text.literal(message), false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleProfileActivate(
      ServerCommandSource source, StateContext state, String requestedName) {
    Optional<LootLockProfile> found = findProfileByName(state.data(), requestedName);
    if (found.isEmpty()) {
      source.sendError(Text.literal("Profile not found: " + requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    state.data().setActiveProfileId(target.getId());
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Activated profile '" + target.getName() + "'."
            : "Activated profile '" + target.getName() + "' for " + state.displayName() + ".";
    source.sendFeedback(() -> Text.literal(message), false);
    sendStatus(source, state.withProfile(target));
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleAdd(
      ServerCommandSource source, StateContext state, Identifier itemId) {
    if (!Registries.ITEM.containsId(itemId)) {
      source.sendError(Text.literal("Unknown item id: " + itemId));
      return 0;
    }

    String token = itemId.toString();
    if (!addRuleToProfile(state.profile(), token)) {
      source.sendError(Text.literal("Rule already exists for: " + token));
      return 0;
    }

    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Added rule: " + token
            : "Added rule: " + token + " for " + state.displayName();
    source.sendFeedback(() -> Text.literal(message), false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleRemove(
      ServerCommandSource source, StateContext state, Identifier itemId) {
    String token = itemId.toString();
    if (!removeRuleFromProfile(state.profile(), token)) {
      source.sendError(Text.literal("Rule not found for: " + token));
      return 0;
    }

    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Removed rule: " + token
            : "Removed rule: " + token + " for " + state.displayName();
    source.sendFeedback(() -> Text.literal(message), false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleRuleList(ServerCommandSource source, StateContext state) {
    if (state.profile().getRules().isEmpty()) {
      String message =
          state.isSelfTargeted()
              ? "No rules in active profile."
              : "No rules in " + state.displayName() + "'s active profile.";
      source.sendFeedback(() -> Text.literal(message), false);
      return 1;
    }

    String header =
        state.isSelfTargeted()
            ? "Rules for '" + state.profile().getName() + "':"
            : "Rules for "
                + state.displayName()
                + "'s profile '"
                + state.profile().getName()
                + "':";
    source.sendFeedback(() -> Text.literal(header), false);
    int invalidRules = 0;
    for (RuleEntry rule : state.profile().getRules()) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        invalidRules++;
        continue;
      }
      source.sendFeedback(() -> Text.literal("- " + rule.itemId()), false);
    }
    if (invalidRules > 0) {
      int invalidRuleCount = invalidRules;
      source.sendFeedback(
          () -> Text.literal("- <" + invalidRuleCount + " invalid rule entries hidden>"), false);
    }
    return 1;
  }

  private static int ruleClearConfirmHelp(CommandContext<ServerCommandSource> context) {
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Add 'confirm' to clear all rules."), false);
    return 1;
  }

  private static int handleRuleClear(ServerCommandSource source, StateContext state) {
    clearRulesOnProfile(state.profile());
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Cleared all rules from active profile."
            : "Cleared all rules from " + state.displayName() + "'s active profile.";
    source.sendFeedback(() -> Text.literal(message), false);
    syncIfOnline(state);
    return 1;
  }

  private static int handleEnable(ServerCommandSource source, StateContext state, boolean enabled) {
    applyGlobalEnable(state.data(), enabled);
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Loot Lock " + (enabled ? "enabled" : "disabled") + "."
            : "Loot Lock "
                + (enabled ? "enabled" : "disabled")
                + " for "
                + state.displayName()
                + ".";
    source.sendFeedback(() -> Text.literal(message), false);
    sendStatus(source, state);
    syncIfOnline(state);
    return 1;
  }

  private static int handleMode(ServerCommandSource source, StateContext state, FilterMode mode) {
    state.profile().setMode(mode);
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Loot Lock mode set to "
                + modeToken(mode)
                + " for profile '"
                + state.profile().getName()
                + "'."
            : "Loot Lock mode set to "
                + modeToken(mode)
                + " for "
                + state.displayName()
                + "'s profile '"
                + state.profile().getName()
                + "'.";
    source.sendFeedback(() -> Text.literal(message), false);
    sendStatus(source, state);
    syncIfOnline(state);
    return 1;
  }

  private static int handleAction(
      ServerCommandSource source, StateContext state, RejectedItemAction action) {
    RejectedItemAction normalizedAction =
        normalizeRejectedItemAction(action, LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
    if (action == RejectedItemAction.DELETE && normalizedAction != RejectedItemAction.DELETE) {
      source.sendError(
          Text.literal(
              "Server policy blocks delete mode for rejected items. Use 'leave' instead."));
      return 0;
    }

    state.profile().setRejectedItemAction(normalizedAction);
    markDirty(source, state);
    String message =
        state.isSelfTargeted()
            ? "Loot Lock rejected-item action set to "
                + actionToken(normalizedAction)
                + " for profile '"
                + state.profile().getName()
                + "'."
            : "Loot Lock rejected-item action set to "
                + actionToken(normalizedAction)
                + " for "
                + state.displayName()
                + "'s profile '"
                + state.profile().getName()
                + "'.";
    source.sendFeedback(() -> Text.literal(message), false);
    if (normalizedAction == RejectedItemAction.DELETE) {
      source.sendFeedback(
          () -> Text.literal("Warning: delete mode permanently destroys rejected dropped items."),
          false);
    }
    sendStatus(source, state);
    syncIfOnline(state);
    return 1;
  }

  private static void sendStatus(ServerCommandSource source, StateContext state) {
    String header =
        state.isSelfTargeted()
            ? "Loot Lock status:"
            : "Loot Lock status for " + state.displayName() + ":";
    LootLockProfile profile = state.profile();
    source.sendFeedback(() -> Text.literal(header), false);
    source.sendFeedback(() -> Text.literal("- Active profile: " + profile.getName()), false);
    source.sendFeedback(() -> Text.literal("- Enabled: " + profile.isEnabled()), false);
    source.sendFeedback(() -> Text.literal("- Mode: " + modeToken(profile.getMode())), false);
    source.sendFeedback(
        () -> Text.literal("- Action: " + actionToken(profile.getRejectedItemAction())), false);
    source.sendFeedback(() -> Text.literal("- Rule count: " + profile.getRules().size()), false);
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
      source.sendError(Text.literal("This command can only be used by a player."));
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
      source.sendError(Text.literal("Loot Lock is not ready yet."));
      return null;
    }

    LootLockPlayerData data = dataManager.getOrLoad(uuid);
    LootLockProfile profile = data.getActiveProfile().orElse(null);
    if (profile == null) {
      source.sendError(Text.literal("No active Loot Lock profile found."));
      return null;
    }

    return new StateContext(uuid, displayName, online, isSelfTargeted, dataManager, data, profile);
  }

  static TargetContext resolveTarget(ServerCommandSource source, String input) {
    MinecraftServer server = source.getServer();
    if (server == null) {
      source.sendError(Text.literal("Server not ready."));
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

    source.sendError(Text.literal("Unknown player: " + input));
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

  private static Identifier ruleIdentifier(CommandContext<ServerCommandSource> context)
      throws CommandSyntaxException {
    return IdentifierArgumentType.getIdentifier(context, "item");
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
