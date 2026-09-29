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
import org.junit.jupiter.params.provider.CsvSource;
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

    double ratio = LootLockToast.contrast(adjusted, spritePixel(80, 16) & 0xFFFFFF);
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
    assertTrue(
        LootLockToast.contrast(argb & 0xFFFFFF, spritePixel(80, 16) & 0xFFFFFF) >= MIN_CONTRAST);
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

  @ParameterizedTest(name = "#{0} on #{1} is {2}:1")
  @CsvSource({
    "FFFFFF, 000000, 21.0",
    "000000, FFFFFF, 21.0",
    "767676, FFFFFF, 4.54",
    "777777, FFFFFF, 4.48",
    "FFFFFF, FFFFFF, 1.0",
  })
  void contrastMatchesWcagReferenceValues(String a, String b, double expected) {
    assertEquals(
        expected, LootLockToast.contrast(Integer.parseInt(a, 16), Integer.parseInt(b, 16)), 0.01);
  }

  @ParameterizedTest(name = "{0}x{1} toast is tiled exactly from the clean sprite")
  @CsvSource({"160, 32", "200, 42", "240, 52", "161, 33", "239, 62"})
  void backgroundTilesCoverTheToastExactlyFromCleanSpritePixels(int width, int height)
      throws IOException {
    int[][] coverage = new int[width][height];
    for (LootLockToast.Tile tile : LootLockToast.backgroundTiles(width, height)) {
      for (int dx = 0; dx < tile.width(); dx++) {
        for (int dy = 0; dy < tile.height(); dy++) {
          int x = tile.x() + dx;
          int y = tile.y() + dy;
          coverage[x][y]++;
          int source = spritePixel(tile.u() + dx, tile.v() + dy);
          if (x >= 4 && x < width - 4 && y >= 4 && y < height - 4) {
            assertEquals(LootLockToast.BACKGROUND, source, "interior at " + x + "," + y);
          }
          if (x < 4 || x >= width - 4 || y < 4 || y >= height - 4) {
            assertEquals(
                spritePixel(frameU(x, width), frameV(y, height)),
                source,
                "frame at " + x + "," + y);
          }
        }
      }
    }
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) {
        assertEquals(1, coverage[x][y], "coverage at " + x + "," + y);
      }
    }
  }

  private static int frameU(int x, int width) {
    if (x < 4) {
      return x;
    }
    return x >= width - 4 ? 160 - (width - x) : 80;
  }

  private static int frameV(int y, int height) {
    if (y < 4) {
      return y;
    }
    return y >= height - 4 ? 32 - (height - y) : 16;
  }

  private static BufferedImage sprite;

  private static int spritePixel(int x, int y) throws IOException {
    if (sprite == null) {
      try (InputStream stream =
          LootLockToastTest.class.getResourceAsStream(
              "/assets/minecraft/textures/gui/sprites/toast/system.png")) {
        assertNotNull(stream);
        sprite = ImageIO.read(stream);
      }
    }
    Raster raster = sprite.getRaster();
    int[] samples = raster.getPixel(x, y, (int[]) null);
    if (sprite.getColorModel() instanceof IndexColorModel indexed) {
      return indexed.getRGB(samples[0]);
    }
    return 0xFF000000 | samples[0] << 16 | samples[1] << 8 | samples[2];
  }
}
