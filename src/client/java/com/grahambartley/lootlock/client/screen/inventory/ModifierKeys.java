package com.grahambartley.lootlock.client.screen.inventory;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public final class ModifierKeys {
  private ModifierKeys() {}

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
