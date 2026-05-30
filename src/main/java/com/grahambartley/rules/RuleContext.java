package com.grahambartley.rules;

import net.minecraft.entity.ItemEntity;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public record RuleContext(World world, @Nullable ItemEntity sourceEntity, PickupSource source) {}
