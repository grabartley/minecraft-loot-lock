package com.grahambartley.text;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Style;
import net.minecraft.util.Language;

public final class LootLockTestLanguage {
  private LootLockTestLanguage() {}

  private static volatile boolean installed = false;

  public static synchronized void install() {
    if (installed) {
      return;
    }
    Map<String, String> entries = loadEnUs();
    Language.setInstance(new MapLanguage(entries));
    installed = true;
  }

  static Map<String, String> loadEnUs() {
    Path path = Path.of("src/client/resources/assets/loot-lock/lang/en_us.json");
    try {
      String json = Files.readString(path);
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      Map<String, String> entries = new LinkedHashMap<>();
      for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
        entries.put(entry.getKey(), entry.getValue().getAsString());
      }
      return entries;
    } catch (Exception ex) {
      throw new RuntimeException("Failed to load en_us.json from " + path.toAbsolutePath(), ex);
    }
  }

  private static final class MapLanguage extends Language {
    private final Map<String, String> entries;

    MapLanguage(Map<String, String> entries) {
      this.entries = entries;
    }

    @Override
    public String get(String key, String fallback) {
      return entries.getOrDefault(key, fallback);
    }

    @Override
    public boolean hasTranslation(String key) {
      return entries.containsKey(key);
    }

    @Override
    public boolean isRightToLeft() {
      return false;
    }

    @Override
    public OrderedText reorder(StringVisitable text) {
      return OrderedText.styledForwardsVisitedString(text.getString(), Style.EMPTY);
    }
  }
}
