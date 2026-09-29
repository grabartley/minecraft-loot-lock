package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ChromeTest {

  static Stream<Arguments> spriteIds() {
    return Stream.of(
        Arguments.of("panel", Chrome.PANEL),
        Arguments.of("slot", Chrome.SLOT),
        Arguments.of("button", Chrome.BUTTON.enabled()),
        Arguments.of("button disabled", Chrome.BUTTON.disabled()),
        Arguments.of("button highlighted", Chrome.BUTTON.enabledFocused()),
        Arguments.of("tab selected", Chrome.TAB.enabled()),
        Arguments.of("tab", Chrome.TAB.disabled()),
        Arguments.of("tab selected highlighted", Chrome.TAB.enabledFocused()),
        Arguments.of("tab highlighted", Chrome.TAB.disabledFocused()));
  }

  @ParameterizedTest(name = "{0} sprite ships in the vanilla client")
  @MethodSource("spriteIds")
  void spriteExistsInVanillaAssets(String label, Identifier id) throws IOException {
    try (InputStream stream = ChromeTest.class.getResourceAsStream(spritePath(id, ".png"))) {
      assertNotNull(stream, "missing vanilla sprite " + id);
    }
  }

  static Stream<Arguments> stretchedSpriteIds() {
    return Stream.of(
        Arguments.of("panel", Chrome.PANEL),
        Arguments.of("button", Chrome.BUTTON.enabled()),
        Arguments.of("tab", Chrome.TAB.disabled()));
  }

  @ParameterizedTest(name = "{0} sprite is nine-sliced so it stretches to any size")
  @MethodSource("stretchedSpriteIds")
  void stretchedSpriteIsNineSliced(String label, Identifier id) throws IOException {
    try (InputStream stream = ChromeTest.class.getResourceAsStream(spritePath(id, ".png.mcmeta"))) {
      assertNotNull(stream, "missing sprite metadata for " + id);
      String meta = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(meta.contains("nine_slice"), id + " is not nine-sliced: " + meta);
    }
  }

  private static String spritePath(Identifier id, String suffix) {
    return "/assets/" + id.getNamespace() + "/textures/gui/sprites/" + id.getPath() + suffix;
  }
}
