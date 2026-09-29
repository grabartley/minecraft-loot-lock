package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class LootLockToastTest {

  private static final double MIN_CONTRAST = 4.5;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  @Test
  void backgroundMatchesTheVanillaSystemToastSprite() throws IOException {
    assertEquals(LootLockToast.BACKGROUND, spritePixel(80, 16));
  }

  static Stream<Arguments> textColors() {
    Stream<Arguments> profile =
        IntStream.range(0, Palette.PROFILE_COLORS.length)
            .mapToObj(i -> Arguments.of("PROFILE_COLORS[" + i + "]", Palette.PROFILE_COLORS[i]));
    Stream<Arguments> formatting =
        Stream.of(Formatting.RED, Formatting.GREEN, Formatting.YELLOW, Formatting.DARK_RED)
            .map(f -> Arguments.of(f.getName(), 0xFF000000 | f.getColorValue()));
    return Stream.concat(profile, formatting);
  }

  @ParameterizedTest(name = "{0} is readable on the toast")
  @MethodSource("textColors")
  void readableColorMeetsContrastOnTheSprite(String label, int argb) throws IOException {
    int adjusted = LootLockToast.readableColor(argb & 0xFFFFFF);

    double ratio = contrast(adjusted, spritePixel(80, 16));
    assertTrue(
        ratio >= MIN_CONTRAST, String.format("%s -> %06X at %.2f:1", label, adjusted, ratio));
  }

  @ParameterizedTest(name = "0x{0} is already readable and stays unchanged")
  @ValueSource(ints = {0xFFFFFF, 0xFFFF55, 0xFFFF00})
  void readableColorLeavesBrightColoursAlone(int rgb) {
    assertEquals(rgb, LootLockToast.readableColor(rgb));
  }

  static Stream<Arguments> fixedTextColors() {
    return Stream.of(
        Arguments.of("title", LootLockToast.TITLE_COLOR),
        Arguments.of("body", LootLockToast.BODY_COLOR));
  }

  @ParameterizedTest(name = "{0} text is readable on the toast")
  @MethodSource("fixedTextColors")
  void fixedTextColoursMeetContrast(String label, int argb) throws IOException {
    assertTrue(contrast(argb & 0xFFFFFF, spritePixel(80, 16)) >= MIN_CONTRAST);
  }

  @Test
  void readableStyleKeepsUncolouredStyles() {
    Style style = Style.EMPTY.withBold(true);

    assertSame(style, LootLockToast.readable(style));
  }

  @Test
  void readableTextLightensOnlyColouredSpans() {
    OrderedText text =
        OrderedText.concat(
            OrderedText.styledForwardsVisitedString("a", Style.EMPTY),
            OrderedText.styledForwardsVisitedString(
                "b", Style.EMPTY.withColor(TextColor.fromRgb(0x2C5FA5))));
    List<TextColor> colors = new ArrayList<>();

    LootLockToast.readable(text)
        .accept(
            (index, style, codePoint) -> {
              colors.add(style.getColor());
              return true;
            });

    assertNull(colors.get(0));
    assertNotNull(colors.get(1));
    assertEquals(LootLockToast.readableColor(0x2C5FA5), colors.get(1).getRgb());
  }

  private static int spritePixel(int x, int y) throws IOException {
    try (InputStream stream =
        LootLockToastTest.class.getResourceAsStream(
            "/assets/minecraft/textures/gui/sprites/toast/system.png")) {
      assertNotNull(stream);
      BufferedImage image = ImageIO.read(stream);
      Raster raster = image.getRaster();
      int[] samples = raster.getPixel(x, y, (int[]) null);
      if (image.getColorModel() instanceof IndexColorModel indexed) {
        return indexed.getRGB(samples[0]);
      }
      return 0xFF000000 | samples[0] << 16 | samples[1] << 8 | samples[2];
    }
  }

  private static double contrast(int rgbA, int rgbB) {
    double la = luminance(rgbA);
    double lb = luminance(rgbB);
    return (Math.max(la, lb) + 0.05) / (Math.min(la, lb) + 0.05);
  }

  private static double luminance(int rgb) {
    return 0.2126 * channel((rgb >> 16) & 0xFF)
        + 0.7152 * channel((rgb >> 8) & 0xFF)
        + 0.0722 * channel(rgb & 0xFF);
  }

  private static double channel(int value) {
    double c = value / 255.0;
    return c <= 0.03928 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
  }
}
