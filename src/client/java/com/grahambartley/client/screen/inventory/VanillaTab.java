package com.grahambartley.client.screen.inventory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/** Tab strip button with raised inactive face and lowered active face with a gold top accent. */
public final class VanillaTab extends PressableWidget {
  private final BooleanSupplier activeSupplier;
  private final Runnable onPressAction;

  public VanillaTab(
      int x,
      int y,
      int width,
      int height,
      Text label,
      BooleanSupplier activeSupplier,
      Runnable onPressAction) {
    super(x, y, width, height, label);
    this.activeSupplier = activeSupplier;
    this.onPressAction = onPressAction;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean on = activeSupplier.getAsBoolean();
    if (on) {
      Chrome.activeTab(context, getX(), getY(), getWidth(), getHeight());
    } else {
      Chrome.inactiveTab(context, getX(), getY(), getWidth(), getHeight());
    }

    int textColor = on ? 0xFFFFFFFF : 0xFF4A4A4A;
    MinecraftClient client = MinecraftClient.getInstance();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(getMessage())) / 2;
    int textY = getY() + (getHeight() - 8) / 2 + 1;
    context.drawText(client.textRenderer, getMessage(), textX, textY, textColor, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
