package com.grahambartley.rules;

import com.grahambartley.api.PickupDecision;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public interface RuleEngine {
	PickupDecision evaluate(ServerPlayerEntity player, ItemStack stack, RuleContext context);
}
