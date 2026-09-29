package com.grahambartley.lootlock.client.screen.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.Bootstrap;
import net.minecraft.SharedConstants;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.Window;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.lwjgl.glfw.GLFW;
import org.mockito.MockedStatic;

class ModifierKeysTest {
  private static final long WINDOW_HANDLE = 42L;

  @BeforeAll
  static void bootstrap() {
    SharedConstants.createGameVersion();
    Bootstrap.initialize();
  }

  static Stream<Arguments> heldKeys() {
    boolean controlCounts = !MinecraftClient.IS_SYSTEM_MAC;
    return Stream.of(
        Arguments.of("nothing", Set.of(), false),
        Arguments.of("shift", Set.of(GLFW.GLFW_KEY_LEFT_SHIFT), false),
        Arguments.of("left control", Set.of(GLFW.GLFW_KEY_LEFT_CONTROL), controlCounts),
        Arguments.of("right control", Set.of(GLFW.GLFW_KEY_RIGHT_CONTROL), controlCounts),
        Arguments.of("left super", Set.of(GLFW.GLFW_KEY_LEFT_SUPER), true),
        Arguments.of("right super", Set.of(GLFW.GLFW_KEY_RIGHT_SUPER), true));
  }

  @ParameterizedTest(name = "{0} held -> {2}")
  @MethodSource("heldKeys")
  void additiveSelectionFollowsHeldKeys(String label, Set<Integer> held, boolean expected) {
    MinecraftClient client = clientWithWindow(mock(Window.class));
    when(client.getWindow().getHandle()).thenReturn(WINDOW_HANDLE);
    try (MockedStatic<MinecraftClient> clientStatic = mockStatic(MinecraftClient.class);
        MockedStatic<InputUtil> input = mockStatic(InputUtil.class)) {
      clientStatic.when(MinecraftClient::getInstance).thenReturn(client);
      input
          .when(() -> InputUtil.isKeyPressed(anyLong(), anyInt()))
          .thenAnswer(
              call ->
                  (long) call.getArgument(0) == WINDOW_HANDLE
                      && held.contains(call.getArgument(1)));

      assertEquals(expected, ModifierKeys.isAdditiveSelectionDown());
    }
  }

  @Test
  void falseWhenNoClientIsRunning() {
    try (MockedStatic<Screen> screen = mockStatic(Screen.class);
        MockedStatic<MinecraftClient> clientStatic = mockStatic(MinecraftClient.class)) {
      screen.when(Screen::hasControlDown).thenReturn(false);
      clientStatic.when(MinecraftClient::getInstance).thenReturn(null);

      assertFalse(ModifierKeys.isAdditiveSelectionDown());
    }
  }

  @Test
  void falseWhenClientHasNoWindow() {
    MinecraftClient client = clientWithWindow(null);
    try (MockedStatic<Screen> screen = mockStatic(Screen.class);
        MockedStatic<MinecraftClient> clientStatic = mockStatic(MinecraftClient.class)) {
      screen.when(Screen::hasControlDown).thenReturn(false);
      clientStatic.when(MinecraftClient::getInstance).thenReturn(client);

      assertFalse(ModifierKeys.isAdditiveSelectionDown());
    }
  }

  private static MinecraftClient clientWithWindow(Window window) {
    MinecraftClient client = mock(MinecraftClient.class);
    when(client.getWindow()).thenReturn(window);
    return client;
  }
}
