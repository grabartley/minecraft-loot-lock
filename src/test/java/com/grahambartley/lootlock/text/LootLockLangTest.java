package com.grahambartley.lootlock.text;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LootLockLangTest {

  @Test
  void everyConstantKeyHasAValueInEnUs() {
    Map<String, String> entries = LootLockTestLanguage.loadEnUs();
    Set<String> jsonKeys = entries.keySet();

    List<String> missing = new ArrayList<>();
    for (Field field : LootLockLang.class.getDeclaredFields()) {
      if (!isPublicStaticFinalString(field)) {
        continue;
      }
      String key = readKey(field);
      if (!jsonKeys.contains(key)) {
        missing.add(field.getName() + " -> " + key);
      }
    }

    assertTrue(
        missing.isEmpty(),
        "LootLockLang keys missing from en_us.json (add a value or remove the constant): "
            + String.join(", ", missing));
  }

  @Test
  void enUsHasNoUnusedKeys() {
    Map<String, String> entries = LootLockTestLanguage.loadEnUs();
    Set<String> constantValues = constantKeys();

    List<String> orphans = new ArrayList<>();
    for (String key : entries.keySet()) {
      if (!constantValues.contains(key)) {
        orphans.add(key);
      }
    }

    assertTrue(
        orphans.isEmpty(),
        "en_us.json keys with no matching LootLockLang constant: " + String.join(", ", orphans));
  }

  @Test
  void constantsHaveNoDuplicateKeys() {
    Set<String> seen = new HashSet<>();
    Set<String> duplicates = new LinkedHashSet<>();
    for (Field field : LootLockLang.class.getDeclaredFields()) {
      if (!isPublicStaticFinalString(field)) {
        continue;
      }
      String key = readKey(field);
      if (!seen.add(key)) {
        duplicates.add(key);
      }
    }

    assertEquals(Collections.emptySet(), duplicates, "LootLockLang has duplicate key values");
  }

  @Test
  void everyConstantValueResolvesToNonBlankString() {
    Map<String, String> entries = LootLockTestLanguage.loadEnUs();
    for (Field field : LootLockLang.class.getDeclaredFields()) {
      if (!isPublicStaticFinalString(field)) {
        continue;
      }
      String key = readKey(field);
      String value = entries.get(key);
      assertTrue(value != null, "Missing value for " + key);
    }
  }

  private static Set<String> constantKeys() {
    Set<String> values = new HashSet<>();
    for (Field field : LootLockLang.class.getDeclaredFields()) {
      if (isPublicStaticFinalString(field)) {
        values.add(readKey(field));
      }
    }
    return values;
  }

  private static boolean isPublicStaticFinalString(Field field) {
    int mod = field.getModifiers();
    return Modifier.isPublic(mod)
        && Modifier.isStatic(mod)
        && Modifier.isFinal(mod)
        && field.getType() == String.class;
  }

  private static String readKey(Field field) {
    try {
      return (String) field.get(null);
    } catch (IllegalAccessException ex) {
      throw new RuntimeException(ex);
    }
  }
}
