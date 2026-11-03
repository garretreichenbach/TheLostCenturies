package com.thelostcenturies.core.mixin.minecolonies;

import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIBasic;
import com.minecolonies.core.entity.ai.workers.AbstractEntityAIInteract;
import com.minecolonies.core.entity.ai.workers.production.EntityAIQuarrier;
import com.minecolonies.core.entity.ai.workers.production.EntityAIStructureMiner;
import com.thelostcenturies.core.mechanized.MechanizedBuildings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mechanized mining: MineColonies computes a worker's per-block mining delay in
 * {@code calculateWorkerMiningDelay} from block hardness, tool, worker skill and colony research.
 * For Miners and Quarriers we scale the result down when their hut has powered Create drills.
 */
@Mixin(AbstractEntityAIInteract.class)
public abstract class AbstractEntityAIInteractMixin {

    @Inject(method = "calculateWorkerMiningDelay", at = @At("RETURN"), cancellable = true)
    private void lostcenturies$mechanizedMiningBonus(BlockState state, BlockPos pos, CallbackInfoReturnable<Integer> cir) {
        Object self = this;
        if (!(self instanceof EntityAIStructureMiner) && !(self instanceof EntityAIQuarrier)) {
            return;
        }
        AbstractBuilding building = ((AbstractEntityAIBasic<?, ?>) self).building;
        if (building == null) {
            return;
        }
        double factor = MechanizedBuildings.miningDelayFactor(building);
        if (factor < 1.0) {
            cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValue() * factor)));
        }
    }
}
