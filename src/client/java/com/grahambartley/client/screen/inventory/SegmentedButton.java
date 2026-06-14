package com.grahambartley.client.screen.inventory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/**
 * One half of a segmented control. Renders with a vanilla beveled face when off, and with a colored
 * on-state (allow=green, deny=red, leave=grey, delete=red) when on. Matches the prototype's {@code
 * .seg} CSS class.
 */
public final class SegmentedButton extends PressableWidget {
  private final BooleanSupplier onSupplier;
  private final Runnable onPressAction;
  private final int onColor;

  public SegmentedButton(
      int x,
      int y,
      int width,
      int height,
      Text label,
      int onColor,
      BooleanSupplier onSupplier,
      Runnable onPressAction) {
    super(x, y, width, height, label);
    this.onSupplier = onSupplier;
    this.onPressAction = onPressAction;
    this.onColor = onColor;
  }

  @Override
  public void onPress() {
    if (onPressAction != null) {
      onPressAction.run();
    }
  }

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean on = onSupplier.getAsBoolean();
    if (on) {
      Chrome.coloredSegment(context, getX(), getY(), getWidth(), getHeight(), onColor);
    } else {
      Chrome.guiButton(context, getX(), getY(), getWidth(), getHeight());
    }

    int textColor;
    if (on) {
      textColor = 0xFFFFFFFF;
    } else if (!active) {
      textColor = 0xFF9A9A9A;
    } else {
      textColor = 0xFF4A4A4A;
    }
    MinecraftClient client = MinecraftClient.getInstance();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(getMessage())) / 2;
    int textY = getY() + (getHeight() - 8) / 2;
    context.drawText(client.textRenderer, getMessage(), textX, textY, textColor, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
