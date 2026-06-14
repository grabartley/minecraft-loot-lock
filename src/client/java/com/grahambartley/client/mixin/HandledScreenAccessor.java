package com.grahambartley.client.mixin;

import net.minecraft.client.gui.screen.ingame.HandledScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Exposes the screen-origin fields {@code x} and {@code y} on {@link HandledScreen}. */
@Mixin(HandledScreen.class)
public interface HandledScreenAccessor {
  @Accessor("x")
  int lootlock$getInvX();

  @Accessor("y")
  int lootlock$getInvY();
}
