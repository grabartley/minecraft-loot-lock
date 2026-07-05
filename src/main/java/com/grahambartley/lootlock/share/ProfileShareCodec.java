package com.grahambartley.lootlock.share;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grahambartley.lootlock.data.FilterMode;
import com.grahambartley.lootlock.data.LootLockProfile;
import com.grahambartley.lootlock.data.RejectedItemAction;
import com.grahambartley.lootlock.data.RuleEntry;
import com.grahambartley.lootlock.network.PacketLimits;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;
import java.util.zip.Inflater;
import net.minecraft.util.Identifier;

public final class ProfileShareCodec {
  public static final String PREFIX = "ll1.";
  public static final int FORMAT_VERSION = 1;
  static final int MAX_NAME_LENGTH = 32;
  static final int MAX_DECOMPRESSED_BYTES = 256 * 1024;

  private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();

  private ProfileShareCodec() {}

  public static String encode(LootLockProfile profile) {
    JsonObject root = new JsonObject();
    root.addProperty("v", FORMAT_VERSION);
    root.addProperty("name", profile.getName());
    root.addProperty("mode", profile.getMode().name());
    root.addProperty("action", profile.getRejectedItemAction().name());
    JsonArray rules = new JsonArray();
    for (RuleEntry rule : profile.getRules()) {
      if (rule == null || rule.itemId() == null || rule.itemId().isBlank()) {
        continue;
      }
      rules.add(rule.itemId());
    }
    root.add("rules", rules);
    byte[] payload = GSON.toJson(root).getBytes(StandardCharsets.UTF_8);
    byte[] compressed = deflate(payload);
    return PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(compressed);
  }

  public static DecodeResult decode(String input) {
    if (input == null) {
      return DecodeResult.err("empty");
    }
    String trimmed = input.trim();
    if (trimmed.isEmpty()) {
      return DecodeResult.err("empty");
    }
    if (trimmed.length() > PacketLimits.MAX_SHARE_CODE_LENGTH) {
      return DecodeResult.err("too_long");
    }
    if (!trimmed.startsWith(PREFIX)) {
      return DecodeResult.err("bad_prefix");
    }
    String body = trimmed.substring(PREFIX.length());
    byte[] compressed;
    try {
      compressed = Base64.getUrlDecoder().decode(body);
    } catch (IllegalArgumentException ex) {
      return DecodeResult.err("bad_base64");
    }
    byte[] payload;
    try {
      payload = inflate(compressed);
    } catch (DataFormatException ex) {
      return DecodeResult.err("bad_deflate");
    }
    String json = new String(payload, StandardCharsets.UTF_8);
    JsonObject root;
    try {
      root = GSON.fromJson(json, JsonObject.class);
    } catch (JsonParseException ex) {
      return DecodeResult.err("bad_json");
    }
    if (root == null) {
      return DecodeResult.err("bad_json");
    }
    if (!root.has("v")
        || !root.get("v").isJsonPrimitive()
        || root.get("v").getAsInt() != FORMAT_VERSION) {
      return DecodeResult.err("bad_version");
    }
    String name = readString(root, "name");
    if (name == null || name.isBlank() || name.length() > MAX_NAME_LENGTH) {
      return DecodeResult.err("bad_name");
    }
    FilterMode mode = parseMode(readString(root, "mode"));
    if (mode == null) {
      return DecodeResult.err("bad_mode");
    }
    RejectedItemAction action = parseAction(readString(root, "action"));
    if (action == null) {
      return DecodeResult.err("bad_action");
    }
    JsonElement rulesElement = root.get("rules");
    if (rulesElement == null || !rulesElement.isJsonArray()) {
      return DecodeResult.err("bad_rules");
    }
    JsonArray rulesArray = rulesElement.getAsJsonArray();
    if (rulesArray.size() > PacketLimits.MAX_RULES_PER_PROFILE) {
      return DecodeResult.err("too_many_rules");
    }
    List<RuleEntry> rules = new ArrayList<>(rulesArray.size());
    for (JsonElement element : rulesArray) {
      if (element == null
          || !element.isJsonPrimitive()
          || !element.getAsJsonPrimitive().isString()) {
        return DecodeResult.err("bad_rule_entry");
      }
      String token = element.getAsString();
      if (token == null || token.isBlank()) {
        return DecodeResult.err("bad_rule_entry");
      }
      if (token.length() > PacketLimits.MAX_RULE_ID_LENGTH) {
        return DecodeResult.err("bad_rule_entry");
      }
      if (!isValidRuleToken(token)) {
        return DecodeResult.err("bad_rule_entry");
      }
      rules.add(new RuleEntry(token));
    }
    LootLockProfile profile =
        new LootLockProfile(UUID.randomUUID(), name.trim(), mode, action, true, 0, rules);
    return DecodeResult.ok(profile);
  }

  static boolean isValidRuleToken(String token) {
    if (token == null || token.isBlank()) {
      return false;
    }
    String tokenBody = token.startsWith(RuleEntry.TAG_PREFIX) ? token.substring(1) : token;
    if (tokenBody.isBlank()) {
      return false;
    }
    return Identifier.tryParse(tokenBody) != null;
  }

  private static String readString(JsonObject root, String key) {
    JsonElement element = root.get(key);
    if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
      return null;
    }
    return element.getAsString();
  }

  private static FilterMode parseMode(String value) {
    if (value == null) {
      return null;
    }
    try {
      return FilterMode.valueOf(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static RejectedItemAction parseAction(String value) {
    if (value == null) {
      return null;
    }
    try {
      return RejectedItemAction.valueOf(value);
    } catch (IllegalArgumentException ex) {
      return null;
    }
  }

  private static byte[] deflate(byte[] input) {
    Deflater deflater = new Deflater(Deflater.BEST_COMPRESSION, true);
    try {
      deflater.setInput(input);
      deflater.finish();
      ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
      byte[] buffer = new byte[1024];
      while (!deflater.finished()) {
        int written = deflater.deflate(buffer);
        out.write(buffer, 0, written);
      }
      return out.toByteArray();
    } finally {
      deflater.end();
    }
  }

  private static byte[] inflate(byte[] input) throws DataFormatException {
    Inflater inflater = new Inflater(true);
    try {
      inflater.setInput(input);
      ByteArrayOutputStream out = new ByteArrayOutputStream(input.length);
      byte[] buffer = new byte[1024];
      while (!inflater.finished()) {
        int written = inflater.inflate(buffer);
        if (written == 0) {
          if (inflater.needsInput() || inflater.needsDictionary()) {
            throw new DataFormatException("truncated");
          }
          break;
        }
        out.write(buffer, 0, written);
        if (out.size() > MAX_DECOMPRESSED_BYTES) {
          throw new DataFormatException("oversized");
        }
      }
      return out.toByteArray();
    } finally {
      inflater.end();
    }
  }

  public sealed interface DecodeResult {
    static DecodeResult ok(LootLockProfile profile) {
      return new Ok(profile);
    }

    static DecodeResult err(String reason) {
      return new Err(reason);
    }

    record Ok(LootLockProfile profile) implements DecodeResult {}

    record Err(String reason) implements DecodeResult {}
  }
}
