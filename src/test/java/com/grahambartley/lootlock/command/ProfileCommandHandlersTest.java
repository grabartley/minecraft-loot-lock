package com.grahambartley.lootlock.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.server.ServerPlayerDataManager;
import com.grahambartley.lootlock.share.ProfileShareCodec;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;

class ProfileCommandHandlersTest {
  private static final UUID TARGET_UUID = UUID.randomUUID();

  private ServerCommandSource source;
  private ServerPlayerDataManager dataManager;
  private LootLockPlayerData data;

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
  }

  @ParameterizedTest(name = "reason \"{0}\" -> {1}")
  @CsvSource({
    "empty,        loot-lock.command.error.share_code.empty",
    "too_long,     loot-lock.command.error.share_code.too_long",
    "bad_prefix,   loot-lock.command.error.share_code.bad_prefix",
    "bad_base64,   loot-lock.command.error.share_code.bad_payload",
    "bad_deflate,  loot-lock.command.error.share_code.bad_payload",
    "bad_json,     loot-lock.command.error.share_code.bad_payload",
    "bad_version,  loot-lock.command.error.share_code.bad_payload",
    "bad_name,     loot-lock.command.error.share_code.bad_field",
    "bad_rules,    loot-lock.command.error.share_code.bad_field",
    "totally_new,  loot-lock.command.error.share_code.bad_field",
  })
  void shareCodeErrorKeyMapsReasonToTranslationKey(String reason, String expectedKey) {
    assertEquals(expectedKey, ProfileCommandHandlers.shareCodeErrorKey(reason));
  }

  @ParameterizedTest(name = "self={0} lists header {1}")
  @MethodSource("listHeaders")
  void profileListMarksActiveProfile(boolean selfTargeted, String expectedHeader) {
    LootLockProfile inactive = CommandProfileEdits.createProfileWithDefaults("Mining");
    CommandProfileEdits.appendProfile(data, inactive);

    int result = ProfileCommandHandlers.handleProfileList(source, state(selfTargeted));

    assertEquals(1, result);
    assertEquals(
        List.of(
            expectedHeader,
            LootLockLang.COMMAND_PROFILE_LIST_ROW_ACTIVE,
            LootLockLang.COMMAND_PROFILE_LIST_ROW_INACTIVE),
        feedbackKeys());
  }

  static Stream<Arguments> listHeaders() {
    return Stream.of(
        Arguments.of(true, LootLockLang.COMMAND_PROFILE_LIST_HEADER_SELF),
        Arguments.of(false, LootLockLang.COMMAND_PROFILE_LIST_HEADER_TARGET));
  }

  @ParameterizedTest(name = "self={0} creates with feedback {1}")
  @MethodSource("createFeedback")
  void profileCreateAppendsProfileAndMarksDirty(boolean selfTargeted, String expectedFeedback) {
    int result =
        ProfileCommandHandlers.handleProfileCreate(source, state(selfTargeted), " Mining ");

    assertEquals(1, result);
    assertTrue(CommandProfileEdits.findProfileByName(data, "Mining").isPresent());
    assertEquals(List.of(expectedFeedback), feedbackKeys());
    verify(dataManager).markDirty(TARGET_UUID, 0L);
  }

  static Stream<Arguments> createFeedback() {
    return Stream.of(
        Arguments.of(true, LootLockLang.COMMAND_PROFILE_CREATE_SELF),
        Arguments.of(false, LootLockLang.COMMAND_PROFILE_CREATE_TARGET));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("createRejections")
  void profileCreateRejectsInvalidRequests(
      String label, String requestedName, int profileCount, boolean selfTargeted, String error) {
    fillProfiles(profileCount);
    int profilesBefore = data.getProfiles().size();

    int result =
        ProfileCommandHandlers.handleProfileCreate(source, state(selfTargeted), requestedName);

    assertEquals(0, result);
    assertEquals(error, sentErrorKey());
    assertEquals(profilesBefore, data.getProfiles().size());
    verify(dataManager, never()).markDirty(any(UUID.class), anyLong());
  }

  static Stream<Arguments> createRejections() {
    return Stream.of(
        Arguments.of("blank name", "   ", 1, true, LootLockLang.COMMAND_ERROR_PROFILE_NAME_LENGTH),
        Arguments.of(
            "duplicate name", "default", 1, true, LootLockLang.COMMAND_ERROR_PROFILE_DUPLICATE),
        Arguments.of(
            "cap reached for self",
            "Mining",
            PacketLimits.MAX_PROFILES,
            true,
            LootLockLang.COMMAND_ERROR_PROFILE_MAX_SELF),
        Arguments.of(
            "cap reached for target",
            "Mining",
            PacketLimits.MAX_PROFILES,
            false,
            LootLockLang.COMMAND_ERROR_PROFILE_MAX_TARGET));
  }

  @Test
  void profileExportSendsHeaderCodeAndSummary() {
    LootLockProfile active = data.getActiveProfile().orElseThrow();
    active.setRules(List.of(new RuleEntry("minecraft:stone")));
    String expectedCode = ProfileShareCodec.encode(active);

    int result = ProfileCommandHandlers.handleProfileExport(source, state(true), "default");

    assertEquals(1, result);
    assertEquals(
        List.of(
            LootLockLang.COMMAND_PROFILE_EXPORT_HEADER_SELF,
            expectedCode,
            LootLockLang.COMMAND_PROFILE_EXPORT_SUMMARY_ONE),
        feedbackKeys());
  }

  @ParameterizedTest(name = "{0} with an unknown name reports not found")
  @MethodSource("namedHandlers")
  void namedHandlersReportUnknownProfile(String label, NamedProfileHandler handler) {
    CommandProfileEdits.appendProfile(
        data, CommandProfileEdits.createProfileWithDefaults("Mining"));

    int result = handler.apply(source, state(true), "Nether");

    assertEquals(0, result);
    assertEquals(LootLockLang.COMMAND_ERROR_PROFILE_NOT_FOUND, sentErrorKey());
  }

  static Stream<Arguments> namedHandlers() {
    return Stream.of(
        Arguments.of("export", (NamedProfileHandler) ProfileCommandHandlers::handleProfileExport),
        Arguments.of("delete", (NamedProfileHandler) ProfileCommandHandlers::handleProfileDelete),
        Arguments.of(
            "activate", (NamedProfileHandler) ProfileCommandHandlers::handleProfileActivate));
  }

  @Test
  void profileImportAddsDecodedProfileUnderItsOwnName() {
    String code = ProfileShareCodec.encode(sharedProfile("Farming"));

    int result = ProfileCommandHandlers.handleProfileImport(source, state(true), code);

    assertEquals(1, result);
    LootLockProfile imported = CommandProfileEdits.findProfileByName(data, "Farming").orElseThrow();
    assertEquals(FilterMode.ALLOWLIST, imported.getMode());
    assertEquals(List.of(new RuleEntry("minecraft:wheat")), imported.getRules());
    assertEquals(List.of(LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_SELF), feedbackKeys());
  }

  @Test
  void profileImportRenamesOnNameCollision() {
    String code = ProfileShareCodec.encode(sharedProfile("Default"));

    int result = ProfileCommandHandlers.handleProfileImport(source, state(false), code);

    assertEquals(1, result);
    assertTrue(CommandProfileEdits.findProfileByName(data, "Default (2)").isPresent());
    assertEquals(List.of(LootLockLang.COMMAND_PROFILE_IMPORT_FEEDBACK_RENAMED), feedbackKeys());
  }

  @Test
  void profileImportRejectsMalformedCode() {
    int result = ProfileCommandHandlers.handleProfileImport(source, state(true), "not-a-code");

    assertEquals(0, result);
    assertEquals(LootLockLang.COMMAND_ERROR_SHARE_CODE_BAD_PREFIX, sentErrorKey());
  }

  @ParameterizedTest(name = "self={0} refuses to delete last profile with {1}")
  @MethodSource("lastProfileErrors")
  void profileDeleteRefusesToRemoveLastProfile(boolean selfTargeted, String expectedError) {
    int result = ProfileCommandHandlers.handleProfileDelete(source, state(selfTargeted), "default");

    assertEquals(0, result);
    assertEquals(expectedError, sentErrorKey());
    assertEquals(1, data.getProfiles().size());
  }

  static Stream<Arguments> lastProfileErrors() {
    return Stream.of(
        Arguments.of(true, LootLockLang.COMMAND_ERROR_PROFILE_LAST_SELF),
        Arguments.of(false, LootLockLang.COMMAND_ERROR_PROFILE_LAST_TARGET));
  }

  @Test
  void profileDeleteRemovesNamedProfile() {
    CommandProfileEdits.appendProfile(
        data, CommandProfileEdits.createProfileWithDefaults("Mining"));

    int result = ProfileCommandHandlers.handleProfileDelete(source, state(true), "mining");

    assertEquals(1, result);
    assertTrue(CommandProfileEdits.findProfileByName(data, "Mining").isEmpty());
    assertEquals(List.of(LootLockLang.COMMAND_PROFILE_DELETE_SELF), feedbackKeys());
  }

  @Test
  void profileActivateSwitchesActiveProfileAndReportsStatus() {
    LootLockProfile mining = CommandProfileEdits.createProfileWithDefaults("Mining");
    CommandProfileEdits.appendProfile(data, mining);

    int result = ProfileCommandHandlers.handleProfileActivate(source, state(false), "Mining");

    assertEquals(1, result);
    assertEquals(mining.getId(), data.getActiveProfileId());
    List<String> keys = feedbackKeys();
    assertEquals(LootLockLang.COMMAND_PROFILE_ACTIVATE_TARGET, keys.get(0));
    assertEquals(LootLockLang.COMMAND_STATUS_HEADER_TARGET, keys.get(1));
  }

  @FunctionalInterface
  interface NamedProfileHandler {
    int apply(ServerCommandSource source, CommandStateContext state, String name);
  }

  private CommandStateContext state(boolean selfTargeted) {
    return new CommandStateContext(
        TARGET_UUID,
        "Alex",
        null,
        selfTargeted,
        dataManager,
        data,
        data.getActiveProfile().orElseThrow());
  }

  private void fillProfiles(int count) {
    while (data.getProfiles().size() < count) {
      CommandProfileEdits.appendProfile(
          data,
          CommandProfileEdits.createProfileWithDefaults("Filler " + data.getProfiles().size()));
    }
  }

  private static LootLockProfile sharedProfile(String name) {
    return new LootLockProfile(
        UUID.randomUUID(),
        name,
        FilterMode.ALLOWLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        List.of(new RuleEntry("minecraft:wheat")));
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
    return text.getContent() instanceof TranslatableTextContent translatable
        ? translatable.getKey()
        : text.getString();
  }
}
