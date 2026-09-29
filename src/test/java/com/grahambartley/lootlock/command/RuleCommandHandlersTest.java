package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class RuleCommandHandlersTest {
  private static final UUID TARGET_UUID = UUID.randomUUID();

  private ServerCommandSource source;
  private ServerPlayerDataManager dataManager;
  private LootLockPlayerData data;
  private LootLockProfile profile;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @BeforeEach
  void setUp() {
    source = mock(ServerCommandSource.class);
    dataManager = mock(ServerPlayerDataManager.class);
    data = LootLockPlayerData.createDefault(TARGET_UUID);
    profile = data.getActiveProfile().orElseThrow();
    profile.setRules(List.of(new RuleEntry("minecraft:stone")));
  }

  @Test
  void tagExistsReturnsFalseForNullOrUnknownTag() {
    assertFalse(RuleCommandHandlers.tagExists(null));
    assertFalse(RuleCommandHandlers.tagExists(Identifier.of("lootlock", "bogus_tag")));
  }

  static Stream<Arguments> successfulMutations() {
    return Stream.of(
        Arguments.of(
            "add item for self",
            (RuleHandler) RuleCommandHandlers::handleRuleAdd,
            "minecraft:dirt",
            true,
            "minecraft:dirt",
            true,
            List.of(LootLockLang.COMMAND_RULE_ADD_SELF)),
        Arguments.of(
            "add item for target",
            (RuleHandler) RuleCommandHandlers::handleRuleAdd,
            "minecraft:dirt",
            false,
            "minecraft:dirt",
            true,
            List.of(LootLockLang.COMMAND_RULE_ADD_TARGET)),
        Arguments.of(
            "add unknown tag warns",
            (RuleHandler) RuleCommandHandlers::handleRuleAddTag,
            "lootlock:bogus_tag",
            true,
            "#lootlock:bogus_tag",
            true,
            List.of(
                LootLockLang.COMMAND_RULE_ADD_SELF, LootLockLang.COMMAND_RULE_TAG_UNKNOWN_WARNING)),
        Arguments.of(
            "remove item for target",
            (RuleHandler) RuleCommandHandlers::handleRuleRemove,
            "minecraft:stone",
            false,
            "minecraft:stone",
            false,
            List.of(LootLockLang.COMMAND_RULE_REMOVE_TARGET)));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("successfulMutations")
  void ruleMutationsApplyAndReport(
      String label,
      RuleHandler handler,
      String argument,
      boolean selfTargeted,
      String token,
      boolean expectedPresent,
      List<String> expectedFeedback) {
    int result = handler.apply(source, state(selfTargeted), Identifier.of(argument));

    assertEquals(1, result);
    assertEquals(expectedPresent, CommandProfileEdits.containsRule(profile, token));
    assertEquals(expectedFeedback, feedbackKeys());
    verify(dataManager).markDirty(TARGET_UUID, 0L);
  }

  @Test
  void ruleRemoveTagRemovesPrefixedToken() {
    profile.setRules(List.of(new RuleEntry("#minecraft:logs")));

    int result =
        RuleCommandHandlers.handleRuleRemoveTag(
            source, state(true), Identifier.of("minecraft:logs"));

    assertEquals(1, result);
    assertTrue(profile.getRules().isEmpty());
    assertEquals(List.of(LootLockLang.COMMAND_RULE_REMOVE_SELF), feedbackKeys());
  }

  static Stream<Arguments> rejectedMutations() {
    return Stream.of(
        Arguments.of(
            "add unknown item",
            (RuleHandler) RuleCommandHandlers::handleRuleAdd,
            "lootlock:not_an_item",
            LootLockLang.COMMAND_ERROR_UNKNOWN_ITEM),
        Arguments.of(
            "add existing item",
            (RuleHandler) RuleCommandHandlers::handleRuleAdd,
            "minecraft:stone",
            LootLockLang.COMMAND_ERROR_RULE_EXISTS),
        Arguments.of(
            "remove missing item",
            (RuleHandler) RuleCommandHandlers::handleRuleRemove,
            "minecraft:dirt",
            LootLockLang.COMMAND_ERROR_RULE_NOT_FOUND),
        Arguments.of(
            "remove missing tag",
            (RuleHandler) RuleCommandHandlers::handleRuleRemoveTag,
            "minecraft:logs",
            LootLockLang.COMMAND_ERROR_RULE_NOT_FOUND));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("rejectedMutations")
  void ruleMutationsRejectInvalidRequests(
      String label, RuleHandler handler, String argument, String expectedError) {
    int result = handler.apply(source, state(true), Identifier.of(argument));

    assertEquals(0, result);
    assertEquals(expectedError, sentErrorKey());
    assertEquals(List.of(new RuleEntry("minecraft:stone")), profile.getRules());
    verify(dataManager, never()).markDirty(any(UUID.class), anyLong());
  }

  static Stream<Arguments> ruleListings() {
    return Stream.of(
        Arguments.of(true, List.of(), List.of(LootLockLang.COMMAND_RULE_LIST_EMPTY_SELF)),
        Arguments.of(false, List.of(), List.of(LootLockLang.COMMAND_RULE_LIST_EMPTY_TARGET)),
        Arguments.of(
            true,
            List.of("minecraft:stone", "#minecraft:logs"),
            List.of(
                LootLockLang.COMMAND_RULE_LIST_HEADER_SELF,
                LootLockLang.COMMAND_RULE_LIST_ROW,
                LootLockLang.COMMAND_RULE_LIST_ROW)),
        Arguments.of(
            false,
            Arrays.asList("minecraft:stone", null, " "),
            List.of(
                LootLockLang.COMMAND_RULE_LIST_HEADER_TARGET,
                LootLockLang.COMMAND_RULE_LIST_ROW,
                LootLockLang.COMMAND_RULE_LIST_INVALID)));
  }

  @ParameterizedTest(name = "self={0} rules={1}")
  @MethodSource("ruleListings")
  void ruleListReportsRowsAndInvalidEntries(
      boolean selfTargeted, List<String> ruleIds, List<String> expectedFeedback) {
    List<RuleEntry> rules = new ArrayList<>();
    for (String ruleId : ruleIds) {
      rules.add(ruleId == null ? null : new RuleEntry(ruleId));
    }
    profile.setRules(rules);

    int result = RuleCommandHandlers.handleRuleList(source, state(selfTargeted));

    assertEquals(1, result);
    assertEquals(expectedFeedback, feedbackKeys());
  }

  @Test
  void ruleClearEmptiesActiveProfile() {
    int result = RuleCommandHandlers.handleRuleClear(source, state(false));

    assertEquals(1, result);
    assertTrue(profile.getRules().isEmpty());
    assertEquals(List.of(LootLockLang.COMMAND_RULE_CLEAR_TARGET), feedbackKeys());
    verify(dataManager).markDirty(TARGET_UUID, 0L);
  }

  @FunctionalInterface
  interface RuleHandler {
    int apply(ServerCommandSource source, CommandStateContext state, Identifier id);
  }

  private CommandStateContext state(boolean selfTargeted) {
    return new CommandStateContext(
        TARGET_UUID, "Alex", null, selfTargeted, dataManager, data, profile);
  }

  @SuppressWarnings("unchecked")
  private List<String> feedbackKeys() {
    ArgumentCaptor<Supplier<Text>> captor = ArgumentCaptor.forClass(Supplier.class);
    verify(source, atLeast(0)).sendFeedback(captor.capture(), anyBoolean());
    return captor.getAllValues().stream().map(supplier -> translationKey(supplier.get())).toList();
  }

  private String sentErrorKey() {
    ArgumentCaptor<Text> error = ArgumentCaptor.forClass(Text.class);
    verify(source).sendError(error.capture());
    return translationKey(error.getValue());
  }

  private static String translationKey(Text text) {
    return ((TranslatableTextContent) text.getContent()).getKey();
  }
}
