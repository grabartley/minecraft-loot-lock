package com.grahambartley.lootlock.config;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class LootLockConfig {
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
      JsonElement root = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8));
      if (!root.isJsonObject()) {
        return defaults();
      }
      JsonElement allowDelete = root.getAsJsonObject().get("allowDeleteRejectedItems");
      if (allowDelete == null
          || !allowDelete.isJsonPrimitive()
          || !allowDelete.getAsJsonPrimitive().isBoolean()) {
        return defaults();
      }
      return new LootLockConfig(allowDelete.getAsBoolean());
    } catch (IOException | JsonParseException ex) {
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
