package com.thelostcenturies.core.worldgen;

import com.thelostcenturies.core.Config;
import com.thelostcenturies.core.LostCenturies;
import com.thelostcenturies.core.mixin.minecraft.RandomStateAccessor;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.levelgen.RandomState;
import net.neoforged.neoforge.common.ModConfigSpec;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * "Ground zero": makes the overworld hotter and drier around the world center (where the city is),
 * fading back to the normal climate further out.
 *
 * <p>Biomes are picked from climate noise (temperature, humidity, continentalness, ...). We bias only
 * temperature and humidity by distance from the center, so whatever biome mods are installed
 * (Terralith, Tectonic, vanilla) choose their own hot/dry biomes near the city. Terrain shape doesn't
 * use these two values, so landforms are unaffected.
 *
 * <p>Applied when the overworld loads, before spawn search and chunk generation. Changing the settings
 * on an existing world only affects newly generated chunks (expect biome seams).
 */
public final class DryZone {
    private static final Logger LOG = LoggerFactory.getLogger(LostCenturies.MOD_ID);
    // Center of the predefined spawn city (chunk 0,0).
    private static final double CENTER_X = 8.0;
    private static final double CENTER_Z = 8.0;

    private DryZone() {}

    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!value(Config.DRYZONE_ENABLED)) {
            return;
        }
        RandomState randomState = level.getChunkSource().randomState();
        Climate.Sampler sampler = randomState.sampler();
        if (sampler.temperature() instanceof RadialBias) {
            return; // already applied
        }

        double inner = value(Config.DRYZONE_INNER_RADIUS);
        double outer = Math.max(inner + 1, value(Config.DRYZONE_OUTER_RADIUS));
        double tempBias = value(Config.DRYZONE_TEMPERATURE_BIAS);
        double humidityBias = -value(Config.DRYZONE_DRYNESS);

        Climate.Sampler biased = new Climate.Sampler(
                new RadialBias(sampler.temperature(), tempBias, CENTER_X, CENTER_Z, inner, outer),
                new RadialBias(sampler.humidity(), humidityBias, CENTER_X, CENTER_Z, inner, outer),
                sampler.continentalness(),
                sampler.erosion(),
                sampler.depth(),
                sampler.weirdness(),
                sampler.spawnTarget());
        ((RandomStateAccessor) (Object) randomState).setSampler(biased);

        LOG.info("Dry zone applied to the overworld: full effect within {} blocks, fading out by {} "
                + "(temperature +{}, humidity -{})", (int) inner, (int) outer, tempBias, -humidityBias);
    }

    /** Server config should be loaded before levels are created; fall back to defaults if it isn't. */
    private static <T> T value(ModConfigSpec.ConfigValue<T> v) {
        if (Config.SPEC.isLoaded()) {
            return v.get();
        }
        LOG.warn("lostcenturies server config not loaded yet; using default for {}", v.getPath());
        return v.getDefault();
    }
}
