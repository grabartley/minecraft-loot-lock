package com.grahambartley.lootlock.client.screen.inventory;

import com.grahambartley.lootlock.text.LootLockLang;
import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public final class OnOffButton extends PressableWidget {
  private final BooleanSupplier stateSupplier;
  private final Runnable onToggle;
  private boolean readOnly;
  private final boolean badWhenOff;

  public OnOffButton(
      int x,
      int y,
      int width,
      int height,
      BooleanSupplier stateSupplier,
      Runnable onToggle,
      boolean readOnly,
      boolean badWhenOff) {
    super(x, y, width, height, Text.empty());
    this.stateSupplier = stateSupplier;
    this.onToggle = onToggle;
    this.readOnly = readOnly;
    this.badWhenOff = badWhenOff;
  }

  public boolean isOn() {
    return stateSupplier.getAsBoolean();
  }

  public boolean isReadOnly() {
    return readOnly;
  }

  public void setReadOnly(boolean readOnly) {
    this.readOnly = readOnly;
  }

  @Override
  public Text getMessage() {
    return Text.translatable(isOn() ? LootLockLang.SWITCH_ON : LootLockLang.SWITCH_OFF);
  }

  @Override
  public void onPress() {
    if (!readOnly && onToggle != null) {
      onToggle.run();
    }
  }

  @Override
  public void playDownSound(SoundManager soundManager) {
    if (!readOnly) {
      super.playDownSound(soundManager);
    }
  }

  static Identifier sprite(boolean active, boolean readOnly, boolean hovered) {
    boolean interactive = active && !readOnly;
    return GuiSprites.BUTTON.get(interactive, interactive && hovered);
  }

  static int labelColor(boolean on, boolean active, boolean readOnly, boolean badWhenOff) {
    if (!on && badWhenOff) {
      return Palette.DENY_ON_PRESSED;
    }
    if (!active) {
      return Palette.BUTTON_TEXT_DISABLED;
    }
    if (readOnly) {
      return on ? Palette.ALLOW_ON_PRESSED : Palette.BUTTON_TEXT_DISABLED;
    }
    return Palette.BUTTON_TEXT;
  }

  @Override
  protected void renderWidget(DrawContext context, int mouseX, int mouseY, float delta) {
    context.drawGuiTexture(
        sprite(active, readOnly, isSelected()), getX(), getY(), getWidth(), getHeight());
    MinecraftClient client = MinecraftClient.getInstance();
    Text label = getMessage();
    int textX = getX() + (getWidth() - client.textRenderer.getWidth(label)) / 2;
    int textY = getY() + (getHeight() - 8) / 2;
    context.drawText(
        client.textRenderer,
        label,
        textX,
        textY,
        labelColor(isOn(), active, readOnly, badWhenOff),
        true);
  }

  @Override
  protected void appendClickableNarrations(NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
