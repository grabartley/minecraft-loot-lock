package com.grahambartley.client.screen.inventory;

import java.util.function.BooleanSupplier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.sound.SoundManager;
import net.minecraft.text.Text;

/**
 * Compact vanilla-style toggle switch widget used for the Client (interactive) and Server
 * (read-only) toggles in the panel header. Paints a 42x16 switch with a sliding knob and ON / OFF
 * text matching the design.
 */
public final class VanillaSwitch extends PressableWidget {
  private static final int KNOB_WIDTH = 17;
  private static final int KNOB_HEIGHT = 12;
  private static final int KNOB_TRAVEL = 22;

  private final BooleanSupplier stateSupplier;
  private final Runnable onToggle;
  private boolean readOnly;
  private final boolean badWhenOff;

  public VanillaSwitch(
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

  @Override
  protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
    boolean on = isOn();
    if (on) {
      Chrome.switchOn(context, getX(), getY(), getWidth(), getHeight());
    } else if (badWhenOff) {
      Chrome.switchBad(context, getX(), getY(), getWidth(), getHeight());
    } else {
      Chrome.switchOff(context, getX(), getY(), getWidth(), getHeight());
    }

    int knobX = getX() + 2 + (on ? KNOB_TRAVEL - 2 : 0);
    int knobY = getY() + (getHeight() - KNOB_HEIGHT) / 2;
    Chrome.switchKnob(context, knobX, knobY, KNOB_WIDTH, KNOB_HEIGHT);

    MinecraftClient client = MinecraftClient.getInstance();
    int textY = getY() + (getHeight() - 8) / 2;
    if (on) {
      context.drawText(
          client.textRenderer, Text.literal("ON"), getX() + 4, textY, 0xFF11320C, false);
    } else {
      context.drawText(
          client.textRenderer,
          Text.literal("OFF"),
          getX() + getWidth() - 18,
          textY,
          0xFF2C2C2C,
          false);
    }
  }

  @Override
  protected void appendClickableNarrations(
      net.minecraft.client.gui.screen.narration.NarrationMessageBuilder builder) {
    appendDefaultNarrations(builder);
  }
}
