package com.grahambartley.lootlock.mixin;

import com.grahambartley.lootlock.server.WorldSession;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LevelStorage.class)
public abstract class LevelStorageMixin {
  @Inject(
      method = {"createSession", "createSessionWithoutSymlinkCheck"},
      at = @At("RETURN"))
  private void lootlock$openWorldSession(
      String directoryName, CallbackInfoReturnable<LevelStorage.Session> cir) {
    WorldSession.open(cir.getReturnValue().getDirectory(WorldSavePath.ROOT));
  }
}
