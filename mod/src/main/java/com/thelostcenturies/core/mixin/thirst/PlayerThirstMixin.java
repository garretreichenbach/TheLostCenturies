package com.thelostcenturies.core.mixin.thirst;

import com.thelostcenturies.core.survival.SurvivalPresets;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Thirst Was Taken turns player exhaustion into thirst in {@code PlayerThirst#addExhaustion}.
 * Scale the incoming exhaustion by the active survival preset (easy 0, normal ~0.5, hard 1).
 */
@Mixin(PlayerThirst.class)
public abstract class PlayerThirstMixin {

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float lostcenturies$scaleByPreset(float amount) {
        return amount > 0 ? amount * SurvivalPresets.thirstRate() : amount;
    }
}
