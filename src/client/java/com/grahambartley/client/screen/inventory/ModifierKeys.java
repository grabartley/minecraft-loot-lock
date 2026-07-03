package com.grahambartley.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * System "additive selection" modifier: Ctrl on Windows / Linux, Cmd on macOS. Vanilla {@link
 * Screen#hasControlDown()} already maps to Cmd on macOS; the explicit GLFW super-key check is kept
 * so the modifier keeps working if that vanilla mapping changes.
 */
public final class ModifierKeys {
  private ModifierKeys() {}

  /** True when Ctrl is held (any OS) or Cmd is held (macOS). */
  public static boolean isAdditiveSelectionDown() {
    if (Screen.hasControlDown()) {
      return true;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.getWindow() == null) {
      return false;
    }
    long handle = client.getWindow().getHandle();
    return InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT_SUPER)
        || InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT_SUPER);
  }
}
