package com.grahambartley.lootlock.client.screen.inventory;

import java.util.Collection;
import java.util.List;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.texture.StatusEffectSpriteManager;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

final class StatusEffectStrip {
  private static final int ICON_SIZE = 18;
  private static final int GAP = 2;
  private static final int PAD_X = 4;
  private static final int PAD_Y = 2;

  private StatusEffectStrip() {}

  static void paint(
      DrawContext context, int panelX, int panelY, int panelWidth, int mouseX, int mouseY) {
    MinecraftClient client = MinecraftClient.getInstance();
    if (client == null || client.player == null) {
      return;
    }
    Collection<StatusEffectInstance> effects = client.player.getStatusEffects();
    if (effects.isEmpty()) {
      return;
    }
    int stripHeight = ICON_SIZE + 4;
    int stripY = panelY - stripHeight - 2;
    if (stripY < 0) {
      stripY = panelY + 1;
    }
    int maxWidth = panelWidth - 4;
    int totalIconsWidth =
        Math.min(maxWidth, effects.size() * ICON_SIZE + (effects.size() - 1) * GAP);
    int stripX = panelX + (panelWidth - totalIconsWidth) / 2;
    context.fill(
        stripX - PAD_X,
        stripY - PAD_Y,
        stripX + totalIconsWidth + PAD_X,
        stripY + ICON_SIZE + PAD_Y,
        0xC0000000);
    StatusEffectSpriteManager spriteManager = client.getStatusEffectSpriteManager();
    int cursorX = stripX;
    StatusEffectInstance hoveredEffect = null;
    for (StatusEffectInstance effect : effects) {
      if (cursorX + ICON_SIZE > stripX + maxWidth) {
        break;
      }
      Sprite sprite = spriteManager.getSprite(effect.getEffectType());
      context.drawSprite(cursorX, stripY, 0, ICON_SIZE, ICON_SIZE, sprite);
      if (mouseX >= cursorX
          && mouseX < cursorX + ICON_SIZE
          && mouseY >= stripY
          && mouseY < stripY + ICON_SIZE) {
        hoveredEffect = effect;
      }
      cursorX += ICON_SIZE + GAP;
    }
    if (hoveredEffect != null) {
      Text name = Text.translatable(hoveredEffect.getTranslationKey());
      Text duration =
          StatusEffectUtil.getDurationText(
              hoveredEffect,
              1.0f,
              client.world == null ? 20.0f : client.world.getTickManager().getTickRate());
      context.drawTooltip(
          client.textRenderer,
          List.of(name, duration.copy().formatted(Formatting.GRAY)),
          mouseX,
          mouseY);
    }
  }
}
