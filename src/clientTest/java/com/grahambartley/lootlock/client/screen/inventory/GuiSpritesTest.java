package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;
import net.minecraft.util.Identifier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class GuiSpritesTest {

  static Stream<Arguments> spriteIds() {
    return Stream.of(
        Arguments.of("panel", GuiSprites.PANEL),
        Arguments.of("slot", GuiSprites.SLOT),
        Arguments.of("toast", GuiSprites.TOAST),
        Arguments.of("button", GuiSprites.BUTTON.enabled()),
        Arguments.of("button disabled", GuiSprites.BUTTON.disabled()),
        Arguments.of("button highlighted", GuiSprites.BUTTON.enabledFocused()),
        Arguments.of("tab selected", GuiSprites.TAB.enabled()),
        Arguments.of("tab", GuiSprites.TAB.disabled()),
        Arguments.of("tab selected highlighted", GuiSprites.TAB.enabledFocused()),
        Arguments.of("tab highlighted", GuiSprites.TAB.disabledFocused()));
  }

  @ParameterizedTest(name = "{0} sprite ships in the vanilla client")
  @MethodSource("spriteIds")
  void spriteExistsInVanillaAssets(String label, Identifier id) throws IOException {
    try (InputStream stream = GuiSpritesTest.class.getResourceAsStream(spritePath(id, ".png"))) {
      assertNotNull(stream, "missing vanilla sprite " + id);
    }
  }

  static Stream<Arguments> stretchedSpriteIds() {
    return Stream.of(
        Arguments.of("panel", GuiSprites.PANEL),
        Arguments.of("button", GuiSprites.BUTTON.enabled()),
        Arguments.of("tab", GuiSprites.TAB.disabled()));
  }

  @ParameterizedTest(name = "{0} sprite is nine-sliced so it stretches to any size")
  @MethodSource("stretchedSpriteIds")
  void stretchedSpriteIsNineSliced(String label, Identifier id) throws IOException {
    try (InputStream stream =
        GuiSpritesTest.class.getResourceAsStream(spritePath(id, ".png.mcmeta"))) {
      assertNotNull(stream, "missing sprite metadata for " + id);
      String meta = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
      assertTrue(meta.contains("nine_slice"), id + " is not nine-sliced: " + meta);
    }
  }

  @ParameterizedTest(name = "{0} sprite stays plain so region blits sample it directly")
  @MethodSource("regionBlitSpriteIds")
  void regionBlitSpriteHasNoScalingMetadata(String label, Identifier id) throws IOException {
    try (InputStream stream =
        GuiSpritesTest.class.getResourceAsStream(spritePath(id, ".png.mcmeta"))) {
      assertNull(stream, id + " gained sprite scaling metadata");
    }
  }

  static Stream<Arguments> regionBlitSpriteIds() {
    return Stream.of(Arguments.of("toast", GuiSprites.TOAST));
  }

  private static String spritePath(Identifier id, String suffix) {
    return "/assets/" + id.getNamespace() + "/textures/gui/sprites/" + id.getPath() + suffix;
  }
}
