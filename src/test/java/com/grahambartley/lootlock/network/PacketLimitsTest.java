package com.grahambartley.lootlock.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class PacketLimitsTest {

  @Test
  void wireLimitsMatchTheProtocolContract() {
    Map<String, Integer> expected =
        Map.of(
            "MAX_PROFILE_NAME_LENGTH", 64,
            "MAX_RULE_ID_LENGTH", 256,
            "MAX_PROFILES", 9,
            "MAX_RULES_PER_PROFILE", 1024,
            "MAX_SHARE_CODE_LENGTH", 4096);

    Map<String, Integer> actual =
        Arrays.stream(PacketLimits.class.getDeclaredFields())
            .filter(field -> Modifier.isStatic(field.getModifiers()))
            .collect(Collectors.toMap(Field::getName, PacketLimitsTest::intValue));

    assertEquals(expected, actual);
  }

  @Test
  void cannotBeInstantiatedFromOutside() throws NoSuchMethodException {
    Constructor<PacketLimits> constructor = PacketLimits.class.getDeclaredConstructor();

    assertTrue(Modifier.isPrivate(constructor.getModifiers()));
  }

  private static int intValue(Field field) {
    try {
      return field.getInt(null);
    } catch (IllegalAccessException ex) {
      throw new AssertionError(ex);
    }
  }
}
