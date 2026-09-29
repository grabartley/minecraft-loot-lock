package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.PlayerManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.UserCache;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;

class CommandTargetResolverTest {
  private static final UUID PLAYER_UUID = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");

  private ServerCommandSource source;
  private MinecraftServer server;
  private PlayerManager playerManager;
  private UserCache userCache;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void setUp() {
    source = mock(ServerCommandSource.class);
    server = mock(MinecraftServer.class);
    playerManager = mock(PlayerManager.class);
    userCache = mock(UserCache.class);
    when(source.getServer()).thenReturn(server);
    when(server.getPlayerManager()).thenReturn(playerManager);
    when(server.getUserCache()).thenReturn(userCache);
    when(userCache.findByName(anyString())).thenReturn(Optional.empty());
    when(userCache.getByUuid(any())).thenReturn(Optional.empty());
  }

  @AfterEach
  void resetDataManager() {
    LootLock.PLAYER_DATA_MANAGER = null;
  }

  @ParameterizedTest(name = "tryParseUuid(\"{0}\") -> present={1}")
  @CsvSource({
    "069a79f4-44e9-4726-a5be-fca90e38aaf5, true",
    "00000000-0000-0000-0000-000000000000, true",
    "069A79F4-44E9-4726-A5BE-FCA90E38AAF5, true",
    "Steve,                                false",
    "069a79f4-44e9-4726-a5be,              false",
    "'',                                   false",
    ",                                     false",
  })
  void tryParseUuidParsesOnlyValidLiterals(String input, boolean expectedPresent) {
    assertEquals(expectedPresent, CommandTargetResolver.tryParseUuid(input).isPresent());
  }

  @Test
  void resolveTargetPrefersOnlinePlayerByName() {
    ServerPlayerEntity online = player(PLAYER_UUID, "Steve");
    when(playerManager.getPlayer("steve")).thenReturn(online);

    CommandTargetResolver.TargetContext target =
        CommandTargetResolver.resolveTarget(source, "steve");

    assertEquals(new CommandTargetResolver.TargetContext(PLAYER_UUID, "Steve", online), target);
  }

  @Test
  void resolveTargetFallsBackToCachedOfflineProfile() {
    when(userCache.findByName("Alex"))
        .thenReturn(Optional.of(new GameProfile(PLAYER_UUID, "Alex")));

    CommandTargetResolver.TargetContext target =
        CommandTargetResolver.resolveTarget(source, "Alex");

    assertEquals(new CommandTargetResolver.TargetContext(PLAYER_UUID, "Alex", null), target);
  }

  @ParameterizedTest(name = "uuid input with cached name \"{0}\" displays as \"{1}\"")
  @CsvSource({
    "Notch, Notch",
    ",      069a79f4-44e9-4726-a5be-fca90e38aaf5",
  })
  void resolveTargetAcceptsRawUuid(String cachedName, String expectedDisplayName) {
    if (cachedName != null) {
      when(userCache.getByUuid(PLAYER_UUID))
          .thenReturn(Optional.of(new GameProfile(PLAYER_UUID, cachedName)));
    }

    CommandTargetResolver.TargetContext target =
        CommandTargetResolver.resolveTarget(source, PLAYER_UUID.toString());

    assertEquals(
        new CommandTargetResolver.TargetContext(PLAYER_UUID, expectedDisplayName, null), target);
  }

  @Test
  void resolveTargetReportsUnknownPlayer() {
    assertNull(CommandTargetResolver.resolveTarget(source, "Nobody"));
    assertEquals(LootLockLang.COMMAND_ERROR_UNKNOWN_PLAYER, sentErrorKey());
  }

  @Test
  void resolveTargetReportsServerNotReady() {
    when(source.getServer()).thenReturn(null);

    assertNull(CommandTargetResolver.resolveTarget(source, "Steve"));
    assertEquals(LootLockLang.COMMAND_ERROR_SERVER_NOT_READY, sentErrorKey());
  }

  @Test
  void resolveSelfStateRejectsNonPlayerSource() throws CommandSyntaxException {
    when(source.getPlayerOrThrow())
        .thenThrow(ServerCommandSource.REQUIRES_PLAYER_EXCEPTION.create());

    assertNull(CommandTargetResolver.resolveSelfState(source));
    assertEquals(LootLockLang.COMMAND_ERROR_PLAYER_ONLY, sentErrorKey());
  }

  @Test
  void resolveSelfStateReportsNotReadyWithoutDataManager() throws CommandSyntaxException {
    ServerPlayerEntity player = player(PLAYER_UUID, "Steve");
    when(source.getPlayerOrThrow()).thenReturn(player);

    assertNull(CommandTargetResolver.resolveSelfState(source));
    assertEquals(LootLockLang.COMMAND_ERROR_NOT_READY, sentErrorKey());
  }

  @Test
  void resolveSelfStateReportsMissingActiveProfile() throws CommandSyntaxException {
    ServerPlayerEntity player = player(PLAYER_UUID, "Steve");
    when(source.getPlayerOrThrow()).thenReturn(player);
    LootLockPlayerData data = LootLockPlayerData.createDefault(PLAYER_UUID);
    data.setActiveProfileId(UUID.randomUUID());
    LootLock.PLAYER_DATA_MANAGER = dataManagerReturning(data);

    assertNull(CommandTargetResolver.resolveSelfState(source));
    assertEquals(LootLockLang.COMMAND_ERROR_NO_ACTIVE_PROFILE, sentErrorKey());
  }

  @Test
  void resolveSelfStateBuildsSelfTargetedStateForExecutingPlayer() throws CommandSyntaxException {
    ServerPlayerEntity player = player(PLAYER_UUID, "Steve");
    when(source.getPlayerOrThrow()).thenReturn(player);
    LootLockPlayerData data = LootLockPlayerData.createDefault(PLAYER_UUID);
    ServerPlayerDataManager dataManager = dataManagerReturning(data);
    LootLock.PLAYER_DATA_MANAGER = dataManager;

    CommandStateContext state = CommandTargetResolver.resolveSelfState(source);

    assertEquals(PLAYER_UUID, state.targetUuid());
    assertEquals("Steve", state.displayName());
    assertSame(player, state.onlineTarget());
    assertTrue(state.isSelfTargeted());
    assertSame(dataManager, state.dataManager());
    assertSame(data, state.data());
    assertSame(data.getActiveProfile().orElseThrow(), state.profile());
    verify(source, never()).sendError(any());
  }

  private ServerPlayerEntity player(UUID uuid, String name) {
    ServerPlayerEntity player = mock(ServerPlayerEntity.class);
    when(player.getUuid()).thenReturn(uuid);
    when(player.getGameProfile()).thenReturn(new GameProfile(uuid, name));
    return player;
  }

  private static ServerPlayerDataManager dataManagerReturning(LootLockPlayerData data) {
    ServerPlayerDataManager dataManager = mock(ServerPlayerDataManager.class);
    when(dataManager.getOrLoad(data.getPlayerUuid())).thenReturn(data);
    return dataManager;
  }

  private String sentErrorKey() {
    ArgumentCaptor<Text> error = ArgumentCaptor.forClass(Text.class);
    verify(source).sendError(error.capture());
    return ((TranslatableTextContent) error.getValue().getContent()).getKey();
  }
}
