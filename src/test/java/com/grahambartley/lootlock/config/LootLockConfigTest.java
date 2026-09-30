package com.grahambartley.lootlock.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockConfigTest {

  @TempDir Path tempDir;

  @Test
  void loadReturnsDefaultsWhenFileMissing() {
    LootLockConfig config = LootLockConfig.load(tempDir.resolve("missing-policy.json"));
    assertTrue(config.allowDeleteRejectedItems());
  }

  @ParameterizedTest(name = "invalid contents \"{0}\" -> defaults")
  @ValueSource(strings = {"not json", "", "{", "[]"})
  void loadFallsBackToDefaultsWhenJsonInvalid(String invalidJson) throws IOException {
    Path policyPath = tempDir.resolve("server-policy.json");
    Files.writeString(policyPath, invalidJson);

    assertTrue(LootLockConfig.load(policyPath).allowDeleteRejectedItems());
  }

  @ParameterizedTest(name = "contents {0} -> defaults")
  @ValueSource(
      strings = {
        "{\"allowDeleteRejectedItems\":null}",
        "{\"allowDeleteRejectedItems\":{}}",
        "{\"allowDeleteRejectedItems\":[]}",
        "{\"allowDeleteRejectedItems\":\"yes\"}",
        "{\"allowDeleteRejectedItems\":1}",
        "{\"allowDeleteRejectedItems\":false,",
        "null",
        "5",
        "\"text\"",
        "[false]"
      })
  void loadFallsBackToDefaultsWhenValueMalformed(String contents) throws IOException {
    Path policyPath = tempDir.resolve("server-policy.json");
    Files.writeString(policyPath, contents);

    assertTrue(LootLockConfig.load(policyPath).allowDeleteRejectedItems());
  }

  @ParameterizedTest(name = "allowDeleteRejectedItems={0} loads as written")
  @ValueSource(booleans = {true, false})
  void loadReadsBooleanValues(boolean allowDelete) throws IOException {
    Path policyPath = tempDir.resolve("server-policy.json");
    Files.writeString(policyPath, "{\"allowDeleteRejectedItems\":" + allowDelete + "}");

    assertEquals(allowDelete, LootLockConfig.load(policyPath).allowDeleteRejectedItems());
  }

  @ParameterizedTest(name = "save then load roundtrips allowDelete={0}")
  @ValueSource(booleans = {true, false})
  void savePersistsPolicyFile(boolean allowDelete) {
    Path policyPath = tempDir.resolve("server-policy.json");

    assertTrue(LootLockConfig.save(policyPath, new LootLockConfig(allowDelete)));

    assertEquals(allowDelete, LootLockConfig.load(policyPath).allowDeleteRejectedItems());
  }
}
