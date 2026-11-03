package com.thelostcenturies.core.mechanized;

import com.minecolonies.core.colony.buildings.AbstractBuilding;
import com.simibubi.create.content.kinetics.drill.DrillBlockEntity;
import com.thelostcenturies.core.Config;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Tuple;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.structure.BoundingBox;

/**
 * Looks for powered Create machines inside a MineColonies building's footprint and turns
 * them into worker bonuses. Results are cached per building for {@link Config#SCAN_INTERVAL_TICKS}.
 */
public final class MechanizedBuildings {
    private record Key(ResourceKey<Level> dimension, BlockPos buildingId) {}
    private record Entry(long expiresAt, double miningDelayFactor) {}

    private static final int MAX_CACHE_SIZE = 4096;
    private static final Map<Key, Entry> CACHE = new ConcurrentHashMap<>();

    private MechanizedBuildings() {}

    /**
     * Multiplier for the worker's per-block mining delay: 1.0 = no bonus,
     * lower = faster. Never below {@code 1 - maxDelayReduction}.
     */
    public static double miningDelayFactor(AbstractBuilding building) {
        if (!Config.DRILL_BONUS_ENABLED.get()) {
            return 1.0;
        }
        Level level = building.getColony().getWorld();
        if (level == null || level.isClientSide()) {
            return 1.0;
        }

        long now = level.getGameTime();
        Key key = new Key(level.dimension(), building.getID());
        Entry entry = CACHE.get(key);
        if (entry == null || now >= entry.expiresAt()) {
            if (CACHE.size() > MAX_CACHE_SIZE) {
                CACHE.clear(); // buildings come and go; cheap way to drop stale entries
            }
            entry = new Entry(now + Config.SCAN_INTERVAL_TICKS.get(), computeMiningDelayFactor(level, building));
            CACHE.put(key, entry);
        }
        return entry.miningDelayFactor();
    }

    private static double computeMiningDelayFactor(Level level, AbstractBuilding building) {
        double totalRpm = poweredDrillRpm(level, building);
        if (totalRpm <= 0) {
            return 1.0;
        }
        double strength = Math.min(1.0, totalRpm / Config.RPM_FOR_MAX_BONUS.get());
        return 1.0 - Config.MAX_DELAY_REDUCTION.get() * strength;
    }

    /** Sum of |RPM| of all non-overstressed Create drills inside the building's corners. */
    private static double poweredDrillRpm(Level level, AbstractBuilding building) {
        Tuple<BlockPos, BlockPos> corners = building.getCorners();
        if (corners == null) {
            return 0;
        }
        BoundingBox box = BoundingBox.fromCorners(corners.getA(), corners.getB());

        double totalRpm = 0;
        for (int cx = box.minX() >> 4; cx <= box.maxX() >> 4; cx++) {
            for (int cz = box.minZ() >> 4; cz <= box.maxZ() >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue; // never force-load chunks from worker AI
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : chunk.getBlockEntities().values()) {
                    if (be instanceof DrillBlockEntity drill
                            && box.isInside(be.getBlockPos())
                            && !drill.isOverStressed()) {
                        totalRpm += Math.abs(drill.getSpeed());
                    }
                }
            }
        }
        return totalRpm;
    }
}
