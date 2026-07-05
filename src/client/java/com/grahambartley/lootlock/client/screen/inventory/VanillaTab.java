package com.grahambartley.lootlock.client.screen.inventory;

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
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean on = activeSupplier.getAsBoolean();
    // Inactive tabs sit 2px proud of the content well per CSS .tab { top: 2px }.
    int paintY = on ? getY() : getY() - 2;
    if (on) {
      Chrome.activeTab(context, getX(), paintY, getWidth(), getHeight());
    } else {
      Chrome.inactiveTab(context, getX(), paintY, getWidth(), getHeight());
    }

    int textColor = on ? 0xFFFFFFFF : 0xFF4A4A4A;
    MinecraftClient client = MinecraftClient.getInstance();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(getMessage())) / 2;
    // Active tab paints a 4px gold bar across the top; offset its label down so the visual
    // baseline matches the inactive tab labels.
    int textY = paintY + (getHeight() - 8) / 2 + (on ? 2 : 0);
    context.drawText(client.textRenderer, getMessage(), textX, textY, textColor, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
