package com.grahambartley.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class LootLockLocaleParityTest {

  private static final Path LANG_DIR = Path.of("src/client/resources/assets/loot-lock/lang");
  private static final String EN_US = "en_us.json";
  private static final Pattern PLACEHOLDER = Pattern.compile("%(?:\\d+\\$)?[a-zA-Z%]");

  static Stream<String> nonEnglishLocales() throws IOException {
    try (Stream<Path> files = Files.list(LANG_DIR)) {
      return files
          .map(p -> p.getFileName().toString())
          .filter(name -> name.endsWith(".json"))
          .filter(name -> !name.equals(EN_US))
          .sorted()
          .toList()
          .stream();
    }
  }

  @ParameterizedTest
  @MethodSource("nonEnglishLocales")
  void localeHasIdenticalKeySetToEnUs(String localeFile) {
    Map<String, String> en = loadLocale(EN_US);
    Map<String, String> loc = loadLocale(localeFile);

    List<String> missing = new ArrayList<>(new TreeSet<>(en.keySet()));
    missing.removeAll(loc.keySet());
    List<String> extra = new ArrayList<>(new TreeSet<>(loc.keySet()));
    extra.removeAll(en.keySet());

    assertTrue(
        missing.isEmpty(),
        localeFile + " is missing keys present in " + EN_US + ": " + String.join(", ", missing));
    assertTrue(
        extra.isEmpty(),
        localeFile + " has keys not present in " + EN_US + ": " + String.join(", ", extra));
  }

  @ParameterizedTest
  @MethodSource("nonEnglishLocales")
  void localePreservesPlaceholdersFromEnUs(String localeFile) {
    Map<String, String> en = loadLocale(EN_US);
    Map<String, String> loc = loadLocale(localeFile);

    List<String> mismatches = new ArrayList<>();
    for (Map.Entry<String, String> entry : en.entrySet()) {
      String key = entry.getKey();
      String localized = loc.get(key);
      if (localized == null) {
        continue;
      }
      List<String> enPh = extractPlaceholders(entry.getValue());
      List<String> locPh = extractPlaceholders(localized);
      if (!enPh.equals(locPh)) {
        mismatches.add(key + " expected " + enPh + " got " + locPh);
      }
    }

    assertEquals(
        List.of(),
        mismatches,
        localeFile + " has placeholder mismatches vs " + EN_US + ": " + mismatches);
  }

  @ParameterizedTest
  @MethodSource("nonEnglishLocales")
  void localeValuesAreNonBlank(String localeFile) {
    Map<String, String> loc = loadLocale(localeFile);
    List<String> blanks = new ArrayList<>();
    for (Map.Entry<String, String> entry : loc.entrySet()) {
      if (entry.getValue() == null || entry.getValue().isEmpty()) {
        blanks.add(entry.getKey());
      }
    }
    assertTrue(
        blanks.isEmpty(), localeFile + " has empty values for keys: " + String.join(", ", blanks));
  }

  private static Map<String, String> loadLocale(String file) {
    Path path = LANG_DIR.resolve(file);
    try {
      String json = Files.readString(path);
      JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
      Map<String, String> entries = new LinkedHashMap<>();
      for (Map.Entry<String, com.google.gson.JsonElement> entry : obj.entrySet()) {
        entries.put(entry.getKey(), entry.getValue().getAsString());
      }
      return entries;
    } catch (IOException ex) {
      throw new RuntimeException("Failed to load locale " + path.toAbsolutePath(), ex);
    }
  }

  private static List<String> extractPlaceholders(String value) {
    Matcher m = PLACEHOLDER.matcher(value);
    List<String> out = new ArrayList<>();
    while (m.find()) {
      String token = m.group();
      if (!"%%".equals(token)) {
        out.add(token);
      }
    }
    java.util.Collections.sort(out);
    return out;
  }
}
