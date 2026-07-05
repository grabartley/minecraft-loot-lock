package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class PaletteProfileContrastTest {

  private static final double MIN_CONTRAST = 3.0;

  static Stream<Arguments> profileColors() {
    return IntStream.range(0, Palette.PROFILE_COLORS.length)
        .mapToObj(i -> Arguments.of(i, Palette.PROFILE_COLORS[i]));
  }

  @ParameterizedTest(name = "PROFILE_COLORS[{0}] hits >= 3:1 contrast vs toast face")
  @MethodSource("profileColors")
  void profileColorMeetsToastContrast(int index, int argb) {
    double ratio = contrastRatio(argb, Palette.FACE);
    assertTrue(
        ratio >= MIN_CONTRAST,
        String.format(
            "PROFILE_COLORS[%d] 0x%08X has %.2f:1 contrast vs 0x%08X, below %.1f:1",
            index, argb, ratio, Palette.FACE, MIN_CONTRAST));
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
