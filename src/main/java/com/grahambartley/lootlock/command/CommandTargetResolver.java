package com.grahambartley.lootlock.command;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

final class CommandTargetResolver {
  private CommandTargetResolver() {}

  static CommandStateContext resolveSelfState(ServerCommandSource source) {
    ServerPlayerEntity player;
    try {
      player = source.getPlayerOrThrow();
    } catch (CommandSyntaxException ex) {
      source.sendError(Text.translatable(LootLockLang.COMMAND_ERROR_PLAYER_ONLY));
      return null;
    }
    return buildState(source, player.getUuid(), player.getGameProfile().getName(), player, true);
  }

  static CommandStateContext resolveTargetState(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String input = StringArgumentType.getString(context, "target");
    TargetContext target = resolveTarget(source, input);
    if (target == null) {
      return null;
    }
    return buildState(source, target.uuid(), target.displayName(), target.online(), false);
  }

  private static CommandStateContext buildState(
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

    return new CommandStateContext(
        uuid, displayName, online, isSelfTargeted, dataManager, data, profile);
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

  record TargetContext(UUID uuid, String displayName, ServerPlayerEntity online) {}
}
