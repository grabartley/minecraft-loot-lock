package com.grahambartley.rules;

import com.grahambartley.api.PickupDecision;
import com.grahambartley.data.LootLockProfile;
import com.grahambartley.server.ServerPlayerDataManager;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public final class LootLockRuleEngine implements RuleEngine {
  private final ServerPlayerDataManager playerDataManager;

  public LootLockRuleEngine(ServerPlayerDataManager playerDataManager) {
    this.playerDataManager = playerDataManager;
  }

  @Override
  public PickupDecision evaluate(ServerPlayerEntity player, ItemStack stack, RuleContext context) {
    LootLockProfile profile = playerDataManager.get(player).getActiveProfile().orElse(null);
    if (profile == null || !profile.isEnabled()) {
      return PickupDecision.ALLOW;
    }
    Identifier itemId = Registries.ITEM.getId(stack.getItem());
    return profile.evaluate(itemId);
  }
}
