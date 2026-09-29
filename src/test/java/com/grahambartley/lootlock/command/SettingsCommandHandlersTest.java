package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.grahambartley.lootlock.LootLock;
import com.grahambartley.lootlock.config.LootLockConfig;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.text.LootLockLang;
import com.mojang.brigadier.context.CommandContext;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class SettingsCommandHandlersTest {
  private static final UUID TARGET_UUID = UUID.randomUUID();
  private static final List<String> STATUS_LINES =
      List.of(
          LootLockLang.COMMAND_STATUS_LINE_ACTIVE,
          LootLockLang.COMMAND_STATUS_LINE_ENABLED,
          LootLockLang.COMMAND_STATUS_LINE_MODE,
          LootLockLang.COMMAND_STATUS_LINE_ACTION,
          LootLockLang.COMMAND_STATUS_LINE_RULE_COUNT);

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
  }

  @AfterEach
  void resetServerConfig() {
    LootLock.SERVER_CONFIG = LootLockConfig.defaults();
  }

  static Stream<Arguments> modeLabels() {
    return Stream.of(
        Arguments.of(FilterMode.ALLOWLIST, LootLockLang.MODE_ALLOWLIST),
        Arguments.of(FilterMode.DENYLIST, LootLockLang.MODE_DENYLIST));
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("modeLabels")
  void modeLabelTranslatesMode(FilterMode mode, String expectedKey) {
    assertEquals(expectedKey, translationKey(SettingsCommandHandlers.modeLabel(mode)));
  }

  static Stream<Arguments> actionLabels() {
    return Stream.of(
        Arguments.of(RejectedItemAction.DELETE, LootLockLang.ACTION_DELETE),
        Arguments.of(RejectedItemAction.LEAVE_ON_GROUND, LootLockLang.ACTION_LEAVE));
  }

  @ParameterizedTest(name = "{0} -> {1}")
  @MethodSource("actionLabels")
  void actionLabelTranslatesAction(RejectedItemAction action, String expectedKey) {
    assertEquals(expectedKey, translationKey(SettingsCommandHandlers.actionLabel(action)));
  }

  static Stream<Arguments> statusHeaders() {
    return Stream.of(
        Arguments.of(true, LootLockLang.COMMAND_STATUS_HEADER_SELF),
        Arguments.of(false, LootLockLang.COMMAND_STATUS_HEADER_TARGET));
  }

  @ParameterizedTest(name = "self={0} status header {1}")
  @MethodSource("statusHeaders")
  void statusPrintsHeaderAndEveryLine(boolean selfTargeted, String expectedHeader) {
    int result = SettingsCommandHandlers.handleStatus(source, state(selfTargeted));

    assertEquals(1, result);
    assertEquals(withHeader(expectedHeader), feedbackKeys());
  }

  static Stream<Arguments> enableFeedback() {
    return Stream.of(
        Arguments.of(true, true, LootLockLang.COMMAND_ENABLE_SELF),
        Arguments.of(true, false, LootLockLang.COMMAND_DISABLE_SELF),
        Arguments.of(false, true, LootLockLang.COMMAND_ENABLE_TARGET),
        Arguments.of(false, false, LootLockLang.COMMAND_DISABLE_TARGET));
  }

  @ParameterizedTest(name = "self={0} enabled={1} -> {2}")
  @MethodSource("enableFeedback")
  void enableTogglesEveryProfileAndReports(
      boolean selfTargeted, boolean enabled, String expectedFeedback) {
    data.setEnabledForAll(!enabled);

    int result = SettingsCommandHandlers.handleEnable(source, state(selfTargeted), enabled);

    assertEquals(1, result);
    assertEquals(enabled, data.isGloballyEnabled());
    assertEquals(expectedFeedback, feedbackKeys().get(0));
    verify(dataManager).markDirty(TARGET_UUID, 0L);
  }

  static Stream<Arguments> modeFeedback() {
    return Stream.of(
        Arguments.of(true, FilterMode.ALLOWLIST, LootLockLang.COMMAND_MODE_SELF),
        Arguments.of(false, FilterMode.DENYLIST, LootLockLang.COMMAND_MODE_TARGET));
  }

  @ParameterizedTest(name = "self={0} mode={1} -> {2}")
  @MethodSource("modeFeedback")
  void modeUpdatesActiveProfile(boolean selfTargeted, FilterMode mode, String expectedFeedback) {
    int result = SettingsCommandHandlers.handleMode(source, state(selfTargeted), mode);

    assertEquals(1, result);
    assertEquals(mode, profile.getMode());
    assertEquals(expectedFeedback, feedbackKeys().get(0));
    verify(dataManager).markDirty(TARGET_UUID, 0L);
  }

  static Stream<Arguments> allowedActions() {
    List<String> leaveFeedback = new ArrayList<>();
    leaveFeedback.add(LootLockLang.COMMAND_ACTION_SELF);
    leaveFeedback.addAll(withHeader(LootLockLang.COMMAND_STATUS_HEADER_SELF));
    List<String> deleteFeedback = new ArrayList<>();
    deleteFeedback.add(LootLockLang.COMMAND_ACTION_SELF);
    deleteFeedback.add(LootLockLang.COMMAND_ACTION_DELETE_WARNING);
    deleteFeedback.addAll(withHeader(LootLockLang.COMMAND_STATUS_HEADER_SELF));
    return Stream.of(
        Arguments.of(RejectedItemAction.LEAVE_ON_GROUND, false, leaveFeedback),
        Arguments.of(RejectedItemAction.DELETE, true, deleteFeedback));
  }

  @ParameterizedTest(name = "{0} with allowDelete={1}")
  @MethodSource("allowedActions")
  void actionAppliesWhenPolicyAllows(
      RejectedItemAction action, boolean allowDelete, List<String> expectedFeedback) {
    LootLock.SERVER_CONFIG = new LootLockConfig(allowDelete);

    int result = SettingsCommandHandlers.handleAction(source, state(true), action);

    assertEquals(1, result);
    assertEquals(action, profile.getRejectedItemAction());
    assertEquals(expectedFeedback, feedbackKeys());
  }

  @Test
  void actionDeleteIsBlockedWhenPolicyForbidsIt() {
    LootLock.SERVER_CONFIG = new LootLockConfig(false);

    int result =
        SettingsCommandHandlers.handleAction(source, state(true), RejectedItemAction.DELETE);

    assertEquals(0, result);
    assertEquals(RejectedItemAction.LEAVE_ON_GROUND, profile.getRejectedItemAction());
    ArgumentCaptor<Text> error = ArgumentCaptor.forClass(Text.class);
    verify(source).sendError(error.capture());
    assertEquals(
        LootLockLang.COMMAND_ERROR_DELETE_POLICY_BLOCKED, translationKey(error.getValue()));
    verify(dataManager, never()).markDirty(any(UUID.class), anyLong());
  }

  @ParameterizedTest(name = "policy status reports allowDelete={0}")
  @ValueSource(booleans = {true, false})
  void policyStatusReportsCurrentServerPolicy(boolean allowDelete) {
    LootLock.SERVER_CONFIG = new LootLockConfig(allowDelete);
    @SuppressWarnings("unchecked")
    CommandContext<ServerCommandSource> context = mock(CommandContext.class);
    when(context.getSource()).thenReturn(source);

    int result = SettingsCommandHandlers.policyStatus(context);

    assertEquals(1, result);
    Text feedback = feedbackTexts().get(0);
    TranslatableTextContent content = (TranslatableTextContent) feedback.getContent();
    assertEquals(LootLockLang.COMMAND_POLICY_STATUS, content.getKey());
    assertEquals(allowDelete, content.getArgs()[0]);
  }

  private static List<String> withHeader(String header) {
    List<String> lines = new ArrayList<>();
    lines.add(header);
    lines.addAll(STATUS_LINES);
    return lines;
  }

  private CommandStateContext state(boolean selfTargeted) {
    return new CommandStateContext(
        TARGET_UUID, "Alex", null, selfTargeted, dataManager, data, profile);
  }

  @SuppressWarnings("unchecked")
  private List<Text> feedbackTexts() {
    ArgumentCaptor<Supplier<Text>> captor = ArgumentCaptor.forClass(Supplier.class);
    verify(source, atLeast(0)).sendFeedback(captor.capture(), anyBoolean());
    return captor.getAllValues().stream().map(Supplier::get).toList();
  }

  private List<String> feedbackKeys() {
    return feedbackTexts().stream().map(SettingsCommandHandlersTest::translationKey).toList();
  }

  private static String translationKey(Text text) {
    return ((TranslatableTextContent) text.getContent()).getKey();
  }
}
