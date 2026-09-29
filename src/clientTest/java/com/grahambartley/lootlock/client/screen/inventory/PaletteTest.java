package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaletteTest {

  private static final double MIN_CONTRAST = 3.0;

  static Stream<Arguments> profileColors() {
    return IntStream.range(0, Palette.PROFILE_COLORS.length)
        .mapToObj(i -> Arguments.of(i, Palette.PROFILE_COLORS[i]));
  }

  @ParameterizedTest(name = "PROFILE_COLORS[{0}] hits >= 3:1 contrast vs its chip ring")
  @MethodSource("profileColors")
  void profileColorMeetsToastContrast(int index, int argb) {
    double ratio = contrastRatio(argb, Palette.FACE);
    assertTrue(
        ratio >= MIN_CONTRAST,
        String.format(
            "PROFILE_COLORS[%d] 0x%08X has %.2f:1 contrast vs 0x%08X, below %.1f:1",
            index, argb, ratio, Palette.FACE, MIN_CONTRAST));
  }

  static Stream<Arguments> textOnSurfaces() {
    return Stream.of(
        Arguments.of("INK on slot inset", Palette.INK, "container/slot"),
        Arguments.of("INK_DIM on slot inset", Palette.INK_DIM, "container/slot"),
        Arguments.of("ALLOW_ON_SLOT on slot inset", Palette.ALLOW_ON_SLOT, "container/slot"),
        Arguments.of("DENY_ON_SLOT on slot inset", Palette.DENY_ON_SLOT, "container/slot"),
        Arguments.of("OFF_ON_SLOT on slot inset", Palette.OFF_ON_SLOT, "container/slot"),
        Arguments.of("TITLE on panel", Palette.TITLE, "recipe_book/overlay_recipe"),
        Arguments.of(
            "ALLOW_ON_PRESSED on pressed button",
            Palette.ALLOW_ON_PRESSED,
            "widget/button_disabled"),
        Arguments.of(
            "DENY_ON_PRESSED on pressed button", Palette.DENY_ON_PRESSED, "widget/button_disabled"),
        Arguments.of(
            "LEAVE_ON_PRESSED on pressed button",
            Palette.LEAVE_ON_PRESSED,
            "widget/button_disabled"),
        Arguments.of("BUTTON_TEXT on button", Palette.BUTTON_TEXT, "widget/button"));
  }

  @ParameterizedTest(name = "{0} hits >= 3:1 contrast")
  @MethodSource("textOnSurfaces")
  void textMeetsContrastOnTheVanillaSurfaceItSitsOn(String label, int text, String spritePath)
      throws IOException {
    int surface = centerPixel(spritePath);
    double ratio = contrastRatio(text, surface);
    assertTrue(
        ratio >= MIN_CONTRAST,
        String.format("%s has %.2f:1 vs 0x%08X from %s", label, ratio, surface, spritePath));
  }

  static Stream<Arguments> surfaceConstants() {
    return Stream.of(
        Arguments.of("FACE", Palette.FACE, "recipe_book/overlay_recipe"),
        Arguments.of("SLOT", Palette.SLOT, "container/slot"));
  }

  @ParameterizedTest(name = "{0} matches the vanilla {2} sprite")
  @MethodSource("surfaceConstants")
  void surfaceConstantMatchesVanillaSprite(String label, int constant, String spritePath)
      throws IOException {
    assertEquals(constant, centerPixel(spritePath));
  }

  private static int centerPixel(String spritePath) throws IOException {
    try (InputStream stream =
        PaletteTest.class.getResourceAsStream(
            "/assets/minecraft/textures/gui/sprites/" + spritePath + ".png")) {
      assertNotNull(stream, "missing vanilla sprite " + spritePath);
      BufferedImage image = ImageIO.read(stream);
      Raster raster = image.getRaster();
      int[] samples = raster.getPixel(raster.getWidth() / 2, raster.getHeight() / 2, (int[]) null);
      if (image.getColorModel() instanceof IndexColorModel indexed) {
        return indexed.getRGB(samples[0]);
      }
      if (samples.length < 3) {
        return 0xFF000000 | samples[0] << 16 | samples[0] << 8 | samples[0];
      }
      return 0xFF000000 | samples[0] << 16 | samples[1] << 8 | samples[2];
    }
  }

  private static double contrastRatio(int argbA, int argbB) {
    double lA = relativeLuminance(argbA);
    double lB = relativeLuminance(argbB);
    double lighter = Math.max(lA, lB);
    double darker = Math.min(lA, lB);
    return (lighter + 0.05) / (darker + 0.05);
  }

  private static double relativeLuminance(int argb) {
    double r = channelLuminance((argb >> 16) & 0xFF);
    double g = channelLuminance((argb >> 8) & 0xFF);
    double b = channelLuminance(argb & 0xFF);
    return 0.2126 * r + 0.7152 * g + 0.0722 * b;
  }

  private static double channelLuminance(int sRgb8Bit) {
    double c = sRgb8Bit / 255.0;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  }
}
