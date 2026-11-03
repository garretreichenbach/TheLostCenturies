package com.thelostcenturies.core;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Server config (per world): serverconfig/lostcenturies-server.toml */
public final class Config {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder()
            .comment("Mechanized huts: powered Create machines inside a MineColonies hut boost its worker.")
            .push("mechanized");

    public static final ModConfigSpec.BooleanValue DRILL_BONUS_ENABLED = BUILDER
            .comment("Powered Create mechanical drills inside a Miner's or Quarrier's hut speed up the worker's mining.")
            .define("drillBonusEnabled", true);

    public static final ModConfigSpec.DoubleValue MAX_DELAY_REDUCTION = BUILDER
            .comment("Maximum reduction of the worker's per-block mining delay (0.5 = up to 50% faster).")
            .defineInRange("maxDelayReduction", 0.5, 0.0, 0.95);

    public static final ModConfigSpec.DoubleValue RPM_FOR_MAX_BONUS = BUILDER
            .comment("Combined RPM of all powered drills in the hut needed for the full bonus. Below this the bonus scales linearly.")
            .defineInRange("rpmForMaxBonus", 256.0, 1.0, 4096.0);

    public static final ModConfigSpec.IntValue SCAN_INTERVAL_TICKS = BUILDER
            .comment("How often (in ticks) a hut is re-scanned for powered machines. The result is cached in between.")
            .defineInRange("scanIntervalTicks", 100, 20, 6000);

    static {
        BUILDER.pop().comment(
                "Ground zero: the overworld is hotter and drier around the world center (the spawn city),",
                "fading back to normal further out. Biome mods pick their own dry biomes there.",
                "Changing these on an existing world only affects new chunks (biome seams).").push("dryZone");
    }

    public static final ModConfigSpec.BooleanValue DRYZONE_ENABLED = BUILDER
            .define("enabled", true);

    public static final ModConfigSpec.IntValue DRYZONE_INNER_RADIUS = BUILDER
            .comment("Blocks from the world center with the full effect.")
            .defineInRange("innerRadius", 1500, 0, 1_000_000);

    public static final ModConfigSpec.IntValue DRYZONE_OUTER_RADIUS = BUILDER
            .comment("Blocks from the world center where the climate is back to normal.")
            .defineInRange("outerRadius", 4000, 1, 1_000_000);

    public static final ModConfigSpec.DoubleValue DRYZONE_TEMPERATURE_BIAS = BUILDER
            .comment("Added to the temperature noise at full effect (noise is roughly -1..1; > 0.55 counts as hot).")
            .defineInRange("temperatureBias", 0.6, -2.0, 2.0);

    public static final ModConfigSpec.DoubleValue DRYZONE_DRYNESS = BUILDER
            .comment("Subtracted from the humidity noise at full effect (noise is roughly -1..1; < -0.35 counts as arid).")
            .defineInRange("dryness", 0.8, 0.0, 2.0);

    static {
        BUILDER.pop().comment(
                "Survival presets. The active preset is stored per world and changed with",
                "/lostcenturies preset <easy|normal|hard>; this is only the default for new worlds.",
                "  easy:   no player thirst, colony water not needed",
                "  normal: light player thirst; colonists are slowed when the colony is out of water",
                "  hard:   regular player thirst; workers stop working when the colony is out of water").push("survival");
    }

    public static final ModConfigSpec.EnumValue<com.thelostcenturies.core.survival.Preset> DEFAULT_PRESET = BUILDER
            .defineEnum("defaultPreset", com.thelostcenturies.core.survival.Preset.NORMAL);

    public static final ModConfigSpec.DoubleValue NORMAL_THIRST_RATE = BUILDER
            .comment("Player thirst rate on normal, relative to Thirst Was Taken's own rate.")
            .defineInRange("normalThirstRate", 0.5, 0.0, 4.0);

    public static final ModConfigSpec.DoubleValue HARD_THIRST_RATE = BUILDER
            .comment("Player thirst rate on hard, relative to Thirst Was Taken's own rate.")
            .defineInRange("hardThirstRate", 1.0, 0.0, 4.0);

    static {
        BUILDER.pop().comment(
                "Colony drinking water (normal/hard). Each colony keeps a water reserve that refills from water",
                "containers (bottles, buckets, canteens...) stored in its warehouse(s) or town hall.").push("colonyWater");
    }

    public static final ModConfigSpec.IntValue COLONY_WATER_CHECK_INTERVAL = BUILDER
            .comment("Ticks between colony water checks (1200 = 1 minute).")
            .defineInRange("checkIntervalTicks", 1200, 100, 24000);

    public static final ModConfigSpec.DoubleValue WATER_PER_CITIZEN_PER_DAY = BUILDER
            .comment("Water units each citizen drinks per Minecraft day. A bottle/bowl/canteen is 1 unit.")
            .defineInRange("waterPerCitizenPerDay", 1.0, 0.0, 64.0);

    public static final ModConfigSpec.DoubleValue STARTING_RESERVE_DAYS = BUILDER
            .comment("Days of water a new colony starts with (before it has a warehouse).")
            .defineInRange("startingReserveDays", 3.0, 0.0, 100.0);

    public static final ModConfigSpec.IntValue BUCKET_UNITS = BUILDER
            .comment("Water units in a water bucket.")
            .defineInRange("bucketUnits", 3, 1, 64);

    public static final ModConfigSpec.IntValue MIN_PURITY = BUILDER
            .comment("Minimum Thirst Was Taken purity colonists accept: 0 dirty, 1 slightly dirty, 2 acceptable, 3 purified.")
            .defineInRange("minPurity", 2, 0, 3);

    public static final ModConfigSpec SPEC = BUILDER.pop().build();

    private Config() {}
}
