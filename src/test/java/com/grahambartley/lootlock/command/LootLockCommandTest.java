package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class LootLockCommandTest {
  private static final int HELP_LINES_FOR_EVERYONE = 19;
  private static final int HELP_LINES_FOR_OPERATORS = 21;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @AfterEach
  void resetDataManager() {
    LootLock.PLAYER_DATA_MANAGER = null;
  }

  static Stream<Arguments> nodeRequirements() {
    return Stream.of(
        Arguments.of("lootlock", true, true, true, true),
        Arguments.of("lootlock status", false, false, true, true),
        Arguments.of("lootlock enable", false, false, true, true),
        Arguments.of("lootlock disable", false, false, true, true),
        Arguments.of("lootlock mode allowlist", false, false, true, true),
        Arguments.of("lootlock action delete confirm", false, false, true, true),
        Arguments.of("lootlock profile import code", false, false, true, true),
        Arguments.of("lootlock rule add tag tag", false, false, true, true),
        Arguments.of("lootlock rule clear confirm", false, false, true, true),
        Arguments.of("lootlock player", false, true, false, true),
        Arguments.of("lootlock player target rule remove tag tag", false, true, false, true),
        Arguments.of("lootlock player target profile export name", false, true, false, true),
        Arguments.of("lootlock policy", false, true, false, true),
        Arguments.of("lootlock policy allowDeleteRejectedItems true", false, true, false, true));
  }

  @ParameterizedTest(name = "{0}: console={1} op console={2} player={3} op player={4}")
  @MethodSource("nodeRequirements")
  void registeredNodesKeepTheirPermissionRequirements(
      String path,
      boolean usableByConsole,
      boolean usableByOperatorConsole,
      boolean usableByPlayer,
      boolean usableByOperatorPlayer) {
    List<Boolean> expected =
        List.of(usableByConsole, usableByOperatorConsole, usableByPlayer, usableByOperatorPlayer);

    List<Boolean> actual =
        Stream.of(source(false, 0), source(false, 2), source(true, 0), source(true, 2))
            .map(source -> requiredPathUsable(path, source))
            .toList();

    assertEquals(expected, actual);
  }

  @ParameterizedTest(name = "permission level {0} sees {1} help lines")
  @CsvSource({
    "0, " + HELP_LINES_FOR_EVERYONE,
    "2, " + HELP_LINES_FOR_OPERATORS,
  })
  void rootCommandPrintsHelpGatedByPermission(int permissionLevel, int expectedLines)
      throws CommandSyntaxException {
    ServerCommandSource source = source(true, permissionLevel);

    int result = dispatcher().execute("lootlock", source);

    List<String> keys = feedbackKeys(source);
    assertEquals(1, result);
    assertEquals(expectedLines, keys.size());
    assertEquals(LootLockLang.COMMAND_HELP_HEADER, keys.get(0));
  }

  @ParameterizedTest(name = "\"{0}\" prints {1}")
  @CsvSource({
    "lootlock action delete, loot-lock.command.delete_confirm.hint",
    "lootlock rule clear,    loot-lock.command.rule_clear.hint",
  })
  void confirmationGatedCommandsPrintTheirHint(String command, String expectedKey)
      throws CommandSyntaxException {
    ServerCommandSource source = source(true, 0);

    int result = dispatcher().execute(command, source);

    assertEquals(1, result);
    assertEquals(List.of(expectedKey), feedbackKeys(source));
  }

  @Test
  void selfCommandReportsNotReadyWithoutDataManager() throws CommandSyntaxException {
    ServerCommandSource source = source(true, 0);
    ServerPlayerEntity player = mock(ServerPlayerEntity.class);
    when(player.getUuid()).thenReturn(UUID.randomUUID());
    when(player.getGameProfile()).thenReturn(new GameProfile(UUID.randomUUID(), "Steve"));
    when(source.getPlayerOrThrow()).thenReturn(player);

    int result = dispatcher().execute("lootlock status", source);

    assertEquals(0, result);
    ArgumentCaptor<Text> error = ArgumentCaptor.forClass(Text.class);
    verify(source).sendError(error.capture());
    assertEquals(LootLockLang.COMMAND_ERROR_NOT_READY, translationKey(error.getValue()));
  }

  private static boolean requiredPathUsable(String path, ServerCommandSource source) {
    CommandNode<ServerCommandSource> node = dispatcher().getRoot();
    boolean usable = true;
    for (String segment : path.split(" ")) {
      node = node.getChild(segment);
      assertNotNull(node, "missing node " + segment + " in " + path);
      usable &= node.canUse(source);
    }
    return usable;
  }

  private static CommandDispatcher<ServerCommandSource> dispatcher() {
    CommandDispatcher<ServerCommandSource> dispatcher = new CommandDispatcher<>();
    LootLockCommand.register(dispatcher);
    return dispatcher;
  }

  private static ServerCommandSource source(boolean player, int permissionLevel) {
    ServerCommandSource source = mock(ServerCommandSource.class);
    when(source.isExecutedByPlayer()).thenReturn(player);
    when(source.hasPermissionLevel(anyInt()))
        .thenAnswer(invocation -> permissionLevel >= (int) invocation.getArgument(0));
    return source;
  }

  @SuppressWarnings("unchecked")
  private static List<String> feedbackKeys(ServerCommandSource source) {
    ArgumentCaptor<Supplier<Text>> captor = ArgumentCaptor.forClass(Supplier.class);
    verify(source, atLeast(0)).sendFeedback(captor.capture(), anyBoolean());
    return captor.getAllValues().stream().map(supplier -> translationKey(supplier.get())).toList();
  }

  private static String translationKey(Text text) {
    return text.getContent() instanceof TranslatableTextContent translatable
        ? translatable.getKey()
        : text.getString();
  }
}
