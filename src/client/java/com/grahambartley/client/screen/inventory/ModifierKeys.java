package com.grahambartley.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

/**
 * Cross-platform modifier-key helper. Vanilla {@link Screen#hasControlDown()} does not bridge to
 * Cmd on macOS in 1.20.1 Yarn, so any code that wants the system "additive selection" modifier
 * (Ctrl on Windows / Linux, Cmd on macOS) must OR in an explicit GLFW super-key check.
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
