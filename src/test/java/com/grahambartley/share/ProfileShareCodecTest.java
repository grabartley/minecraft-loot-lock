package com.grahambartley.share;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import com.grahambartley.network.PacketLimits;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

class ProfileShareCodecTest {

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<Arguments> profileShapes() {
    return Stream.of(
        Arguments.of(
            profile("Empty", FilterMode.DENYLIST, RejectedItemAction.LEAVE_ON_GROUND, List.of())),
        Arguments.of(
            profile(
                "Mining",
                FilterMode.ALLOWLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                List.of("minecraft:cobblestone", "minecraft:coal", "minecraft:iron_ore"))),
        Arguments.of(
            profile(
                "Mob farm",
                FilterMode.DENYLIST,
                RejectedItemAction.DELETE,
                List.of("minecraft:rotten_flesh", "#minecraft:beacon_payment_items"))),
        Arguments.of(
            profile(
                "Tags only",
                FilterMode.ALLOWLIST,
                RejectedItemAction.LEAVE_ON_GROUND,
                List.of("#minecraft:logs", "#minecraft:wool", "#minecraft:planks"))));
  }

  @ParameterizedTest
  @MethodSource("profileShapes")
  void encodeProducesPrefixedString(LootLockProfile profile) {
    String code = ProfileShareCodec.encode(profile);
    assertTrue(
        code.startsWith(ProfileShareCodec.PREFIX), "code did not start with prefix: " + code);
    assertTrue(code.length() > ProfileShareCodec.PREFIX.length(), "code was empty payload");
  }

  @ParameterizedTest
  @MethodSource("profileShapes")
  void roundTripPreservesEncodedFields(LootLockProfile profile) {
    String code = ProfileShareCodec.encode(profile);
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(code);
    ProfileShareCodec.DecodeResult.Ok ok =
        assertInstanceOf(ProfileShareCodec.DecodeResult.Ok.class, result);
    LootLockProfile decoded = ok.profile();
    assertEquals(profile.getName(), decoded.getName());
    assertEquals(profile.getMode(), decoded.getMode());
    assertEquals(profile.getRejectedItemAction(), decoded.getRejectedItemAction());
    assertEquals(ruleTokens(profile), ruleTokens(decoded));
  }

  @ParameterizedTest
  @MethodSource("profileShapes")
  void roundTripMintsFreshIdAndEnablesProfile(LootLockProfile profile) {
    String code = ProfileShareCodec.encode(profile);
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(code);
    LootLockProfile decoded = ((ProfileShareCodec.DecodeResult.Ok) result).profile();
    assertNotEquals(profile.getId(), decoded.getId());
    assertTrue(decoded.isEnabled());
    assertEquals(0, decoded.getColor());
  }

  @ParameterizedTest
  @MethodSource("profileShapes")
  void roundTripIsIdempotent(LootLockProfile profile) {
    String first = ProfileShareCodec.encode(profile);
    ProfileShareCodec.DecodeResult firstDecode = ProfileShareCodec.decode(first);
    LootLockProfile decodedFirst = ((ProfileShareCodec.DecodeResult.Ok) firstDecode).profile();
    String second = ProfileShareCodec.encode(decodedFirst);
    assertEquals(first, second);
  }

  static Stream<Arguments> rawInputCases() {
    String oversizedInput =
        ProfileShareCodec.PREFIX + "a".repeat(PacketLimits.MAX_SHARE_CODE_LENGTH);
    return Stream.of(
        Arguments.of((String) null, "empty"),
        Arguments.of("", "empty"),
        Arguments.of("   ", "empty"),
        Arguments.of(oversizedInput, "too_long"),
        Arguments.of("noprefix", "bad_prefix"),
        Arguments.of("ll2.something", "bad_prefix"),
        Arguments.of("ll1.@@@!", "bad_base64"));
  }

  @ParameterizedTest(name = "raw input rejected with reason {1}")
  @MethodSource("rawInputCases")
  void decodeRejectsRawInput(String input, String expectedReason) {
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(input);
    ProfileShareCodec.DecodeResult.Err err =
        assertInstanceOf(ProfileShareCodec.DecodeResult.Err.class, result);
    assertEquals(expectedReason, err.reason());
  }

  static Stream<Arguments> payloadCases() {
    String oversizedName = "x".repeat(33);
    String oversizedRuleId = "minecraft:" + "a".repeat(PacketLimits.MAX_RULE_ID_LENGTH);
    StringBuilder rulesArray = new StringBuilder("[");
    for (int i = 0; i <= PacketLimits.MAX_RULES_PER_PROFILE; i++) {
      if (i > 0) rulesArray.append(",");
      rulesArray.append("\"minecraft:item_").append(i).append("\"");
    }
    rulesArray.append("]");
    return Stream.of(
        Arguments.of(
            "{\"v\":2,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[]}",
            "bad_version"),
        Arguments.of(
            "{\"v\":1,\"name\":\"\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[]}",
            "bad_name"),
        Arguments.of(
            "{\"v\":1,\"name\":\""
                + oversizedName
                + "\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[]}",
            "bad_name"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"NOPE\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[]}",
            "bad_mode"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"NOPE\",\"rules\":[]}",
            "bad_action"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":\"oops\"}",
            "bad_rules"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[\"\"]}",
            "bad_rule_entry"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[\"!!not-an-id\"]}",
            "bad_rule_entry"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":[\""
                + oversizedRuleId
                + "\"]}",
            "bad_rule_entry"),
        Arguments.of(
            "{\"v\":1,\"name\":\"x\",\"mode\":\"DENYLIST\",\"action\":\"LEAVE_ON_GROUND\",\"rules\":"
                + rulesArray
                + "}",
            "too_many_rules"));
  }

  @ParameterizedTest(name = "payload rejected with reason {1}")
  @MethodSource("payloadCases")
  void decodeRejectsInvalidPayload(String json, String expectedReason) {
    ProfileShareCodec.DecodeResult result = ProfileShareCodec.decode(encodeRawJson(json));
    ProfileShareCodec.DecodeResult.Err err =
        assertInstanceOf(ProfileShareCodec.DecodeResult.Err.class, result);
    assertEquals(expectedReason, err.reason());
  }

  @ParameterizedTest(name = "rule token \"{0}\" valid={1}")
  @CsvSource({
    "minecraft:cobblestone, true",
    "#minecraft:logs,       true",
    "ns:path,               true",
    "'',                    false",
    "no_namespace,          true",
    "#bad upper,            false",
  })
  void isValidRuleTokenAcceptsIdsAndTagPrefixedIds(String token, boolean expected) {
    assertEquals(expected, ProfileShareCodec.isValidRuleToken(token));
  }

  private static LootLockProfile profile(
      String name, FilterMode mode, RejectedItemAction action, List<String> rules) {
    List<RuleEntry> entries = new ArrayList<>();
    for (String r : rules) {
      entries.add(new RuleEntry(r));
    }
    return new LootLockProfile(UUID.randomUUID(), name, mode, action, true, 0, entries);
  }

  private static List<String> ruleTokens(LootLockProfile profile) {
    List<String> out = new ArrayList<>();
    for (RuleEntry rule : profile.getRules()) {
      out.add(rule.itemId());
    }
    return out;
  }

  private static String encodeRawJson(String json) {
    byte[] payload = json.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    java.util.zip.Deflater deflater =
        new java.util.zip.Deflater(java.util.zip.Deflater.BEST_COMPRESSION, true);
    deflater.setInput(payload);
    deflater.finish();
    java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
    byte[] buffer = new byte[1024];
    while (!deflater.finished()) {
      out.write(buffer, 0, deflater.deflate(buffer));
    }
    deflater.end();
    return ProfileShareCodec.PREFIX
        + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(out.toByteArray());
  }
}
