package com.grahambartley.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LootLockConfigTest {

  @TempDir Path tempDir;

  @Test
  void loadReturnsDefaultsWhenFileMissing() {
    LootLockConfig config = LootLockConfig.load(tempDir.resolve("missing-policy.json"));
    assertTrue(config.allowDeleteRejectedItems());
  }

  @Test
  void loadReadsAllowDeleteRejectedItemsWhenPresent() throws IOException {
    Path policyPath = tempDir.resolve("server-policy.json");
    Files.writeString(policyPath, "{\"allowDeleteRejectedItems\":false}");

    LootLockConfig config = LootLockConfig.load(policyPath);
    assertFalse(config.allowDeleteRejectedItems());
  }

  @Test
  void loadFallsBackToDefaultsWhenJsonInvalid() throws IOException {
    Path policyPath = tempDir.resolve("server-policy.json");
    Files.writeString(policyPath, "not json");

    LootLockConfig config = LootLockConfig.load(policyPath);
    assertTrue(config.allowDeleteRejectedItems());
  }
}
