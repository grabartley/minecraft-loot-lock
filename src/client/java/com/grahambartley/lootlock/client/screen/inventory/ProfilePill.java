package com.grahambartley.lootlock.client.screen.inventory;

import java.util.function.Supplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

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
    GuiSprites.button(context, getX(), getY(), getWidth(), getHeight(), active, isSelected());

    int chipSize = 10;
    int chipX = getX() + 6;
    int chipY = getY() + (getHeight() - chipSize) / 2;
    GuiSprites.colorChip(context, chipX, chipY, chipSize, chipSize, colorSupplier.get());

    MinecraftClient client = MinecraftClient.getInstance();
    int textY = getY() + (getHeight() - 8) / 2;
    int textColor = active ? Palette.BUTTON_TEXT : Palette.BUTTON_TEXT_DISABLED;

    String name = nameSupplier.get();
    int nameX = chipX + chipSize + 6;
    context.drawText(client.textRenderer, Text.literal(name), nameX, textY, textColor, true);

    String meta = metaSupplier.get();
    int metaWidth = client.textRenderer.getWidth(meta);
    int caretX = getX() + getWidth() - 12;
    int metaX = caretX - 4 - metaWidth;
    int metaColor = active ? Palette.BUTTON_TEXT_DIM : Palette.BUTTON_TEXT_DISABLED;
    context.drawText(client.textRenderer, Text.literal(meta), metaX, textY, metaColor, true);

    context.drawText(client.textRenderer, Text.literal("v"), caretX, textY, textColor, true);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
