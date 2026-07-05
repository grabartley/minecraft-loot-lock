package com.grahambartley.lootlock.config;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LootLockConfig {
  // Server policy fields in lootlock/server-policy.json:
  // - allowDeleteRejectedItems (boolean, default: true)
  private static final Gson GSON = new Gson();

  private final boolean allowDeleteRejectedItems;

  public LootLockConfig(boolean allowDeleteRejectedItems) {
    this.allowDeleteRejectedItems = allowDeleteRejectedItems;
  }

  public static LootLockConfig defaults() {
    return new LootLockConfig(true);
  }

  public static LootLockConfig load(Path path) {
    if (path == null || !Files.exists(path)) {
      return defaults();
    }

    try {
      JsonObject root =
          GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
      if (root == null) {
        return defaults();
      }
      boolean allowDelete =
          root.has("allowDeleteRejectedItems")
              ? root.get("allowDeleteRejectedItems").getAsBoolean()
              : true;
      return new LootLockConfig(allowDelete);
    } catch (IOException | JsonParseException | IllegalStateException ex) {
      return defaults();
    }
  }

  public static boolean save(Path path, LootLockConfig config) {
    if (path == null || config == null) {
      return false;
    }

    try {
      Files.createDirectories(path.getParent());
      JsonObject root = new JsonObject();
      root.addProperty("allowDeleteRejectedItems", config.allowDeleteRejectedItems());
      Files.writeString(path, GSON.toJson(root), StandardCharsets.UTF_8);
      return true;
    } catch (IOException ex) {
      return false;
    }
  }

  public boolean allowDeleteRejectedItems() {
    return allowDeleteRejectedItems;
  }
}
