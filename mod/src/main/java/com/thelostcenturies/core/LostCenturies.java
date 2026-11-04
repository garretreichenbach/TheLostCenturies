package com.thelostcenturies.core;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import com.thelostcenturies.core.command.LostCenturiesCommand;
import com.thelostcenturies.core.survival.ColonyWater;
import com.thelostcenturies.core.survival.SurvivalPresets;
import com.thelostcenturies.core.teams.TeamColonySync;
import com.thelostcenturies.core.worldgen.DryZone;

/**
 * Companion mod for The Lost Centuries modpack: gameplay glue that can't be done
 * with KubeJS/datapacks alone: mechanized huts (MineColonies <-> Create), the dry zone,
 * survival presets (thirst) and colony drinking water.
 */
@Mod(LostCenturies.MOD_ID)
public final class LostCenturies {
    public static final String MOD_ID = "lostcenturies";

    public LostCenturies(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(DryZone::onLevelLoad);
        NeoForge.EVENT_BUS.addListener(SurvivalPresets::onServerStarted);
        NeoForge.EVENT_BUS.addListener(SurvivalPresets::onServerStopped);
        NeoForge.EVENT_BUS.addListener(SurvivalPresets::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ColonyWater::onServerTick);
        NeoForge.EVENT_BUS.addListener(LostCenturiesCommand::register);
        TeamColonySync.init();
        modBus.addListener(TeamColonySync::onCommonSetup);
    }
}
