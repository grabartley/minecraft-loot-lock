package com.grahambartley.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.grahambartley.data.FilterMode;
import com.grahambartley.data.LootLockPlayerData;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.data.RejectedItemAction;
import com.grahambartley.data.RuleEntry;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class ConfigSerializer {
  private static final Gson GSON =
      new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

  public static String serialize(LootLockPlayerData data) {
    return GSON.toJson(serializeToTree(data));
  }

  public static LootLockPlayerData deserialize(String json, UUID expectedPlayerUuid)
      throws ConfigDeserializationException {
    try {
      JsonObject root = GSON.fromJson(json, JsonObject.class);
      if (root == null) {
        throw new ConfigDeserializationException("Empty JSON content");
      }
      return deserializeFromTree(root, expectedPlayerUuid);
    } catch (JsonParseException e) {
      throw new ConfigDeserializationException("Failed to parse JSON: " + e.getMessage(), e);
    }
  }

  static JsonObject serializeToTree(LootLockPlayerData data) {
    JsonObject root = new JsonObject();
    root.addProperty("schemaVersion", data.getSchemaVersion());
    root.addProperty("playerUuid", data.getPlayerUuid().toString());
    root.addProperty("activeProfileId", data.getActiveProfileId().toString());
    root.addProperty("revision", data.getRevision());

    JsonArray profiles = new JsonArray();
    for (LootLockProfile profile : data.getProfiles()) {
      profiles.add(serializeProfile(profile));
    }
    root.add("profiles", profiles);

    return root;
  }

  static LootLockPlayerData deserializeFromTree(JsonObject root, UUID expectedPlayerUuid)
      throws ConfigDeserializationException {
    LootLockPlayerData data = new LootLockPlayerData();

    if (root.has("schemaVersion")) {
      data.setSchemaVersion(root.get("schemaVersion").getAsInt());
    }

    if (root.has("playerUuid")) {
      try {
        UUID fileUuid = UUID.fromString(root.get("playerUuid").getAsString());
        if (expectedPlayerUuid != null && !fileUuid.equals(expectedPlayerUuid)) {
          throw new ConfigDeserializationException(
              "Player UUID mismatch: expected " + expectedPlayerUuid + ", got " + fileUuid);
        }
        data.setPlayerUuid(fileUuid);
      } catch (IllegalArgumentException e) {
        throw new ConfigDeserializationException("Invalid playerUuid format: " + e.getMessage(), e);
      }
    }

    if (root.has("activeProfileId")) {
      try {
        data.setActiveProfileId(UUID.fromString(root.get("activeProfileId").getAsString()));
      } catch (IllegalArgumentException e) {
        throw new ConfigDeserializationException(
            "Invalid activeProfileId format: " + e.getMessage(), e);
      }
    }

    if (root.has("revision")) {
      data.setRevision(root.get("revision").getAsLong());
    }

    if (root.has("profiles")) {
      List<LootLockProfile> profiles = new ArrayList<>();
      for (JsonElement element : root.getAsJsonArray("profiles")) {
        if (element.isJsonObject()) {
          profiles.add(deserializeProfile(element.getAsJsonObject()));
        }
      }
      data.setProfiles(profiles);
    }

    return data;
  }

  private static JsonObject serializeProfile(LootLockProfile profile) {
    JsonObject obj = new JsonObject();
    obj.addProperty("id", profile.getId().toString());
    obj.addProperty("name", profile.getName());
    obj.addProperty("mode", profile.getMode().name());
    obj.addProperty("rejectedItemAction", profile.getRejectedItemAction().name());
    obj.addProperty("enabled", profile.isEnabled());

    JsonArray rules = new JsonArray();
    for (RuleEntry rule : profile.getRules()) {
      JsonObject ruleObj = new JsonObject();
      ruleObj.addProperty("itemId", rule.itemId());
      rules.add(ruleObj);
    }
    obj.add("rules", rules);

    return obj;
  }

  private static LootLockProfile deserializeProfile(JsonObject obj)
      throws ConfigDeserializationException {
    UUID id = null;
    if (obj.has("id")) {
      try {
        id = UUID.fromString(obj.get("id").getAsString());
      } catch (IllegalArgumentException e) {
        throw new ConfigDeserializationException("Invalid profile id format: " + e.getMessage(), e);
      }
    }

    String name = obj.has("name") ? obj.get("name").getAsString() : null;

    FilterMode mode = FilterMode.DENYLIST;
    if (obj.has("mode")) {
      try {
        mode = FilterMode.valueOf(obj.get("mode").getAsString());
      } catch (IllegalArgumentException e) {
        throw new ConfigDeserializationException(
            "Invalid filter mode: " + obj.get("mode").getAsString(), e);
      }
    }

    RejectedItemAction action = RejectedItemAction.LEAVE_ON_GROUND;
    if (obj.has("rejectedItemAction")) {
      try {
        action = RejectedItemAction.valueOf(obj.get("rejectedItemAction").getAsString());
      } catch (IllegalArgumentException e) {
        throw new ConfigDeserializationException(
            "Invalid rejectedItemAction: " + obj.get("rejectedItemAction").getAsString(), e);
      }
    }

    boolean enabled = true;
    if (obj.has("enabled")) {
      enabled = obj.get("enabled").getAsBoolean();
    }

    List<RuleEntry> rules = new ArrayList<>();
    if (obj.has("rules")) {
      for (JsonElement element : obj.getAsJsonArray("rules")) {
        if (element.isJsonObject() && element.getAsJsonObject().has("itemId")) {
          rules.add(new RuleEntry(element.getAsJsonObject().get("itemId").getAsString()));
        }
      }
    }

    return new LootLockProfile(id, name, mode, action, enabled, rules);
  }

  public static class ConfigDeserializationException extends Exception {
    public ConfigDeserializationException(String message) {
      super(message);
    }

    public ConfigDeserializationException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
