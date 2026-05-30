package com.grahambartley.mixin;

import com.grahambartley.LootLock;
import com.grahambartley.api.PickupDecision;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {
  @Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
  private void lootlock$beforePickup(PlayerEntity player, CallbackInfo ci) {
    if (player.getWorld().isClient()) return;
    if (!(player instanceof ServerPlayerEntity serverPlayer)) return;
    if (LootLock.PICKUP_GUARD == null) return;

    ItemEntity itemEntity = (ItemEntity) (Object) this;
    ItemStack stack = itemEntity.getStack();

    PickupDecision decision = LootLock.PICKUP_GUARD.evaluate(serverPlayer, stack);

    if (decision == PickupDecision.ALLOW) return;

    long currentTick = serverPlayer.getWorld().getTime();

    if (decision == PickupDecision.REJECT_DELETE) {
      itemEntity.discard();
    }

    LootLock.PICKUP_GUARD.tryNotify(
        serverPlayer, stack, decision == PickupDecision.REJECT_DELETE, currentTick);
    ci.cancel();
  }
}
