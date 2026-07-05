package com.grahambartley.lootlock.client.screen.inventory;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

/**
 * Recessed pill that shows the active profile and opens the dropdown manager on click. Composed of
 * a color chip, profile name, dimmed meta line, and a dropdown caret. Matches the prototype's
 * {@code .profile-current} class.
 */
public final class ProfilePill extends PressableWidget {
  private final Supplier<Integer> colorSupplier;
  private final Supplier<String> nameSupplier;
  private final Supplier<String> metaSupplier;
  private final Runnable onPressAction;

  public ProfilePill(
      int x,
      int y,
      int width,
      int height,
      Supplier<Integer> colorSupplier,
      Supplier<String> nameSupplier,
      Supplier<String> metaSupplier,
      Runnable onPressAction) {
    super(x, y, width, height, Text.empty());
    this.colorSupplier = colorSupplier;
    this.nameSupplier = nameSupplier;
    this.metaSupplier = metaSupplier;
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
    Chrome.slot(context, getX(), getY(), getWidth(), getHeight());

    int chipSize = 10;
    int chipX = getX() + 6;
    int chipY = getY() + (getHeight() - chipSize) / 2;
    Chrome.colorChip(context, chipX, chipY, chipSize, chipSize, colorSupplier.get());

    MinecraftClient client = MinecraftClient.getInstance();
    int textY = getY() + (getHeight() - 8) / 2;

    String name = nameSupplier.get();
    int nameX = chipX + chipSize + 6;
    context.drawText(client.textRenderer, Text.literal(name), nameX, textY, Palette.INK, false);

    String meta = metaSupplier.get();
    int metaWidth = client.textRenderer.getWidth(meta);
    int caretX = getX() + getWidth() - 12;
    int metaX = caretX - 4 - metaWidth;
    context.drawText(client.textRenderer, Text.literal(meta), metaX, textY, Palette.INK_DIM, false);

    context.drawText(client.textRenderer, Text.literal("v"), caretX, textY, Palette.INK, false);
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
