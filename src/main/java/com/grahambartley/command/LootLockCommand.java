package com.grahambartley.command;

import com.grahambartley.LootLock;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import com.grahambartley.network.ServerToClientPackets;
import com.grahambartley.server.ServerPlayerDataManager;
import com.grahambartley.server.ServerPolicyService;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registries;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
                                        context -> setAction(context, RejectedItemAction.DELETE)))))
            .then(
                CommandManager.literal("profile")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(CommandManager.literal("list").executes(LootLockCommand::profileList))
                    .then(
                        CommandManager.literal("create")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(LootLockCommand::profileCreate)))
                    .then(
                        CommandManager.literal("delete")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(LootLockCommand::profileDelete)))
                    .then(
                        CommandManager.literal("activate")
                            .then(
                                CommandManager.argument("name", StringArgumentType.string())
                                    .executes(LootLockCommand::profileActivate))))
            .then(
                CommandManager.literal("rule")
                    .requires(ServerCommandSource::isExecutedByPlayer)
                    .then(
                        CommandManager.literal("add")
                            .then(
                                CommandManager.argument("item", IdentifierArgumentType.identifier())
                                    .executes(LootLockCommand::ruleAdd)))
                    .then(
                        CommandManager.literal("remove")
                            .then(
                                CommandManager.argument("item", IdentifierArgumentType.identifier())
                                    .executes(LootLockCommand::ruleRemove)))
                    .then(CommandManager.literal("list").executes(LootLockCommand::ruleList))
                    .then(
                        CommandManager.literal("clear")
                            .executes(LootLockCommand::ruleClearConfirmHelp)
                            .then(
                                CommandManager.literal("confirm")
                                    .executes(LootLockCommand::ruleClearConfirm))))
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
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock profile list"), false);
    context
        .getSource()
        .sendFeedback(
            () -> Text.literal("- /lootlock profile create <name|\"name with spaces\">"), false);
    context
        .getSource()
        .sendFeedback(
            () -> Text.literal("- /lootlock profile delete <name|\"name with spaces\">"), false);
    context
        .getSource()
        .sendFeedback(
            () -> Text.literal("- /lootlock profile activate <name|\"name with spaces\">"), false);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("- /lootlock mode denylist|allowlist"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock action leave"), false);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("- /lootlock action delete confirm"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock rule add <item>"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock rule remove <item>"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock rule list"), false);
    context.getSource().sendFeedback(() -> Text.literal("- /lootlock rule clear confirm"), false);
    context
        .getSource()
        .sendFeedback(
            () -> Text.literal("- /lootlock policy allowDeleteRejectedItems true|false"), false);
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

  private static int profileList(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    context.getSource().sendFeedback(() -> Text.literal("LootLock profiles:"), false);
    for (LootLockProfile profile : state.data.getProfiles()) {
      if (profile == null) {
        continue;
      }

      String marker = profile.getId().equals(state.data.getActiveProfileId()) ? "* " : "- ";
      context.getSource().sendFeedback(() -> Text.literal(marker + profile.getName()), false);
    }
    return 1;
  }

  private static int profileCreate(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    String requestedName = StringArgumentType.getString(context, "name");
    String profileName = normalizeProfileName(requestedName);
    if (profileName == null) {
      context
          .getSource()
          .sendError(Text.literal("Profile name must be between 1 and 32 characters."));
      return 0;
    }

    if (findProfileByName(state.data, profileName).isPresent()) {
      context.getSource().sendError(Text.literal("A profile with that name already exists."));
      return 0;
    }

    LootLockProfile created =
        new LootLockProfile(
            UUID.randomUUID(),
            profileName,
            FilterMode.DENYLIST,
            RejectedItemAction.LEAVE_ON_GROUND,
            true,
            List.of());
    List<LootLockProfile> profiles = new ArrayList<>(state.data.getProfiles());
    profiles.add(created);
    state.data.setProfiles(profiles);
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Created profile '" + profileName + "'."), false);
    return 1;
  }

  private static int profileDelete(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    if (state.data.getProfiles().size() <= 1) {
      context.getSource().sendError(Text.literal("You cannot delete your last profile."));
      return 0;
    }

    String requestedName = StringArgumentType.getString(context, "name");
    Optional<LootLockProfile> found = findProfileByName(state.data, requestedName);
    if (found.isEmpty()) {
      context.getSource().sendError(Text.literal("Profile not found: " + requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    List<LootLockProfile> profiles = new ArrayList<>(state.data.getProfiles());
    profiles.removeIf(profile -> profile != null && profile.getId().equals(target.getId()));
    state.data.setProfiles(profiles);

    if (target.getId().equals(state.data.getActiveProfileId()) && !profiles.isEmpty()) {
      state.data.setActiveProfileId(profiles.get(0).getId());
    }

    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Deleted profile '" + target.getName() + "'."), false);
    return 1;
  }

  private static int profileActivate(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    String requestedName = StringArgumentType.getString(context, "name");
    Optional<LootLockProfile> found = findProfileByName(state.data, requestedName);
    if (found.isEmpty()) {
      context.getSource().sendError(Text.literal("Profile not found: " + requestedName));
      return 0;
    }

    LootLockProfile target = found.get();
    state.data.setActiveProfileId(target.getId());
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Activated profile '" + target.getName() + "'."), false);
    sendStatus(context.getSource(), target);
    return 1;
  }

  private static int ruleAdd(CommandContext<ServerCommandSource> context)
      throws CommandSyntaxException {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    Identifier itemId = IdentifierArgumentType.getIdentifier(context, "item");
    if (!Registries.ITEM.containsId(itemId)) {
      context.getSource().sendError(Text.literal("Unknown item id: " + itemId));
      return 0;
    }

    String token = itemId.toString();
    if (containsRule(state.profile, token)) {
      context.getSource().sendError(Text.literal("Rule already exists for: " + token));
      return 0;
    }

    List<RuleEntry> rules = new ArrayList<>(state.profile.getRules());
    rules.add(new RuleEntry(token));
    state.profile.setRules(rules);
    state.dataManager.markDirty(state.player);
    context.getSource().sendFeedback(() -> Text.literal("Added rule: " + token), false);
    return 1;
  }

  private static int ruleRemove(CommandContext<ServerCommandSource> context)
      throws CommandSyntaxException {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    Identifier itemId = IdentifierArgumentType.getIdentifier(context, "item");
    String token = itemId.toString();
    List<RuleEntry> rules = new ArrayList<>(state.profile.getRules());
    boolean removed = rules.removeIf(rule -> rule != null && token.equals(rule.itemId()));
    if (!removed) {
      context.getSource().sendError(Text.literal("Rule not found for: " + token));
      return 0;
    }

    state.profile.setRules(rules);
    state.dataManager.markDirty(state.player);
    context.getSource().sendFeedback(() -> Text.literal("Removed rule: " + token), false);
    return 1;
  }

  private static int ruleList(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    if (state.profile.getRules().isEmpty()) {
      context.getSource().sendFeedback(() -> Text.literal("No rules in active profile."), false);
      return 1;
    }

    context
        .getSource()
        .sendFeedback(() -> Text.literal("Rules for '" + state.profile.getName() + "':"), false);
    int invalidRules = 0;
    for (RuleEntry rule : state.profile.getRules()) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        invalidRules++;
        continue;
      }
      context.getSource().sendFeedback(() -> Text.literal("- " + rule.itemId()), false);
    }
    if (invalidRules > 0) {
      int invalidRuleCount = invalidRules;
      context
          .getSource()
          .sendFeedback(
              () -> Text.literal("- <" + invalidRuleCount + " invalid rule entries hidden>"),
              false);
    }
    return 1;
  }

  private static int ruleClearConfirmHelp(CommandContext<ServerCommandSource> context) {
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Add 'confirm' to clear all rules."), false);
    return 1;
  }

  private static int ruleClearConfirm(CommandContext<ServerCommandSource> context) {
    StateContext state = resolveStateContext(context.getSource());
    if (state == null) {
      return 0;
    }

    state.profile.setRules(List.of());
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(() -> Text.literal("Cleared all rules from active profile."), false);
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

    RejectedItemAction normalizedAction =
        normalizeRejectedItemAction(action, LootLock.SERVER_CONFIG.allowDeleteRejectedItems());
    if (action == RejectedItemAction.DELETE && normalizedAction != RejectedItemAction.DELETE) {
      context
          .getSource()
          .sendError(
              Text.literal(
                  "Server policy blocks delete mode for rejected items. Use 'leave' instead."));
      return 0;
    }

    state.profile.setRejectedItemAction(normalizedAction);
    state.dataManager.markDirty(state.player);
    context
        .getSource()
        .sendFeedback(
            () ->
                Text.literal(
                    "LootLock rejected-item action set to "
                        + actionToken(normalizedAction)
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

    return new StateContext(player, dataManager, playerData, activeProfile);
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

  private record StateContext(
      ServerPlayerEntity player,
      ServerPlayerDataManager dataManager,
      LootLockPlayerData data,
      LootLockProfile profile) {}
}
