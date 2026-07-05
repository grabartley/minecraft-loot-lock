package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockPlayerData;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.PacketLimits;
import com.grahambartley.lootlock.share.ProfileShareCodec;
import com.grahambartley.lootlock.text.LootLockLang;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableTextContent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileShareControllerTest {

  @Test
  void exportCopiesEncodedCodeAndShowsToast() {
    LootLockProfile profile = sampleProfile();
    AtomicReference<String> clipboard = new AtomicReference<>();
    AtomicReference<Text> toastTitle = new AtomicReference<>();
    AtomicReference<Text> toastSubtitle = new AtomicReference<>();

    boolean exported =
        ProfileShareController.export(
            profile,
            clipboard::set,
            (title, subtitle) -> {
              toastTitle.set(title);
              toastSubtitle.set(subtitle);
            });

    assertTrue(exported);
    assertNotNull(clipboard.get());
    assertTrue(clipboard.get().startsWith(ProfileShareCodec.PREFIX));
    assertEquals(LootLockLang.BRAND, translationKey(toastTitle.get()));
    assertEquals(LootLockLang.TOAST_EXPORT_COPIED, translationKey(toastSubtitle.get()));
  }

  @Test
  void exportReturnsFalseAndCallsNoSinksWhenProfileIsNull() {
    AtomicInteger clipboardCalls = new AtomicInteger();
    AtomicInteger toastCalls = new AtomicInteger();

    boolean exported =
        ProfileShareController.export(
            null, code -> clipboardCalls.incrementAndGet(), (t, s) -> toastCalls.incrementAndGet());

    assertFalse(exported);
    assertEquals(0, clipboardCalls.get());
    assertEquals(0, toastCalls.get());
  }

  @Test
  void importSuccessSendsCreateRequestAndExposesProfile() {
    LootLockProfile source = sampleProfile();
    String code = ProfileShareCodec.encode(source);
    LootLockPlayerData snapshot = snapshotWith(42L, "Other");
    AtomicReference<Long> sentRevision = new AtomicReference<>();
    AtomicReference<String> sentName = new AtomicReference<>();
    AtomicReference<LootLockProfile> sentProfile = new AtomicReference<>();
    AtomicReference<Text> toastTitle = new AtomicReference<>();
    AtomicReference<Text> toastSubtitle = new AtomicReference<>();

    ProfileShareController.ImportOutcome outcome =
        ProfileShareController.importCode(
            code,
            snapshot,
            (revision, name, copyFrom) -> {
              sentRevision.set(revision);
              sentName.set(name);
              sentProfile.set(copyFrom);
              return true;
            },
            (title, subtitle) -> {
              toastTitle.set(title);
              toastSubtitle.set(subtitle);
            });

    assertTrue(outcome.success());
    assertNull(outcome.errorReason());
    assertEquals(42L, sentRevision.get());
    assertEquals(source.getName(), sentName.get());
    assertNotNull(sentProfile.get());
    assertEquals(source.getRules().size(), sentProfile.get().getRules().size());
    assertSame(sentProfile.get(), outcome.profile());
    assertEquals(LootLockLang.BRAND, translationKey(toastTitle.get()));
    assertEquals(LootLockLang.TOAST_IMPORT_SUCCESS, translationKey(toastSubtitle.get()));
  }

  @Test
  void importResolvesDuplicateNameBeforeSendingCreateRequest() {
    LootLockProfile source = sampleProfile();
    String code = ProfileShareCodec.encode(source);
    LootLockPlayerData snapshot = snapshotWith(0L, source.getName());
    AtomicReference<String> sentName = new AtomicReference<>();

    ProfileShareController.ImportOutcome outcome =
        ProfileShareController.importCode(
            code,
            snapshot,
            (revision, name, copyFrom) -> {
              sentName.set(name);
              return true;
            },
            (title, subtitle) -> {});

    assertTrue(outcome.success());
    assertEquals(source.getName() + " (2)", sentName.get());
  }

  static Stream<Arguments> importFailureCases() {
    String validCode = ProfileShareCodec.encode(sampleProfile());
    return Stream.of(
        Arguments.of(
            "decode-failure", "not a share code", snapshotWith(7L, "Other"), true, "bad_prefix", 0),
        Arguments.of("snapshot-null", "ll1.abc", null, true, "not_ready", 0),
        Arguments.of("at-capacity", validCode, fullSnapshot(50L), true, "at_capacity", 0),
        Arguments.of(
            "create-rejected", validCode, snapshotWith(99L, "Other"), false, "not_ready", 1));
  }

  @ParameterizedTest(name = "{0} -> {4}")
  @MethodSource("importFailureCases")
  void importFailureProducesOutcomeAndToast(
      String label,
      String code,
      LootLockPlayerData snapshot,
      boolean sinkSucceeds,
      String expectedReason,
      int expectedSinkCalls) {
    AtomicInteger sinkCalls = new AtomicInteger();
    AtomicReference<Text> toastTitle = new AtomicReference<>();
    AtomicReference<Text> toastSubtitle = new AtomicReference<>();

    ProfileShareController.ImportOutcome outcome =
        ProfileShareController.importCode(
            code,
            snapshot,
            (revision, name, profile) -> {
              sinkCalls.incrementAndGet();
              return sinkSucceeds;
            },
            (title, subtitle) -> {
              toastTitle.set(title);
              toastSubtitle.set(subtitle);
            });

    assertFalse(outcome.success());
    assertEquals(expectedReason, outcome.errorReason());
    assertNotNull(outcome.errorText());
    assertNull(outcome.profile());
    assertEquals(expectedSinkCalls, sinkCalls.get());
    assertEquals(LootLockLang.BRAND, translationKey(toastTitle.get()));
    assertNotNull(toastSubtitle.get());
  }

  @ParameterizedTest(name = "reason \"{0}\" -> {1}")
  @CsvSource(
      nullValues = {"NULL"},
      value = {
        "empty,             loot-lock.command.error.share_code.empty",
        "too_long,          loot-lock.command.error.share_code.too_long",
        "bad_prefix,        loot-lock.command.error.share_code.bad_prefix",
        "bad_base64,        loot-lock.command.error.share_code.bad_payload",
        "bad_deflate,       loot-lock.command.error.share_code.bad_payload",
        "bad_json,          loot-lock.command.error.share_code.bad_payload",
        "bad_version,       loot-lock.command.error.share_code.bad_payload",
        "bad_name,          loot-lock.command.error.share_code.bad_field",
        "bad_mode,          loot-lock.command.error.share_code.bad_field",
        "bad_action,        loot-lock.command.error.share_code.bad_field",
        "bad_rules,         loot-lock.command.error.share_code.bad_field",
        "too_many_rules,    loot-lock.command.error.share_code.bad_field",
        "bad_rule_entry,    loot-lock.command.error.share_code.bad_field",
        "NULL,              loot-lock.command.error.share_code.bad_field",
        "unknown,           loot-lock.command.error.share_code.bad_field",
      })
  void shareCodeErrorKeyMapsEveryReason(String reason, String expectedKey) {
    assertEquals(expectedKey, ProfileShareController.shareCodeErrorKey(reason));
  }

  private static LootLockProfile sampleProfile() {
    List<RuleEntry> rules = new ArrayList<>();
    rules.add(new RuleEntry("minecraft:cobblestone"));
    rules.add(new RuleEntry("minecraft:coal"));
    return new LootLockProfile(
        UUID.randomUUID(),
        "Mining starter",
        FilterMode.ALLOWLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        0,
        rules);
  }

  private static LootLockProfile namedProfile(String name) {
    return new LootLockProfile(
        UUID.randomUUID(),
        name,
        FilterMode.DENYLIST,
        RejectedItemAction.LEAVE_ON_GROUND,
        true,
        0,
        new ArrayList<>());
  }

  private static LootLockPlayerData snapshotWith(long revision, String existingProfileName) {
    LootLockPlayerData data = new LootLockPlayerData();
    data.setProfiles(new ArrayList<>(List.of(namedProfile(existingProfileName))));
    data.setRevision(revision);
    return data;
  }

  private static LootLockPlayerData fullSnapshot(long revision) {
    LootLockPlayerData data = new LootLockPlayerData();
    List<LootLockProfile> profiles = new ArrayList<>();
    for (int i = 0; i < PacketLimits.MAX_PROFILES; i++) {
      profiles.add(namedProfile("Profile " + i));
    }
    data.setProfiles(profiles);
    data.setRevision(revision);
    return data;
  }

  private static String translationKey(Text text) {
    if (text == null) {
      return null;
    }
    if (text.getContent() instanceof TranslatableTextContent translatable) {
      return translatable.getKey();
    }
    return null;
  }
}
