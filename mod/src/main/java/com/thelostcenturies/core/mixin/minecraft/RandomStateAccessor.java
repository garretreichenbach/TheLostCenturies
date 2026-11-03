package com.thelostcenturies.core.mixin.minecraft;

import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Lets {@link com.thelostcenturies.core.worldgen.DryZone} replace the overworld's climate sampler. */
@Mixin(RandomState.class)
public interface RandomStateAccessor {
    @Mutable
    @Accessor("sampler")
    void setSampler(Climate.Sampler sampler);
}
