package com.thelostcenturies.core.survival;

import com.thelostcenturies.core.Config;
import dev.ghen.thirst.foundation.common.capability.ModAttachment;
import dev.ghen.thirst.content.thirst.PlayerThirst;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Active survival preset for the running server, plus the player-thirst side of it. */
public final class SurvivalPresets {
    private static volatile Preset active = null;

    private SurvivalPresets() {}

    /** Active preset, or the config default if no world is running yet. */
    public static Preset active() {
        Preset p = active;
        if (p != null) {
            return p;
        }
        return Config.SPEC.isLoaded() ? Config.DEFAULT_PRESET.get() : Preset.NORMAL;
    }

    /** Used by the Thirst Was Taken mixin: multiplier on player thirst exhaustion. */
    public static float thirstRate() {
        return (float) active().thirstRate();
    }

    public static void set(MinecraftServer server, Preset preset) {
        LostCenturiesData.get(server).setPreset(preset);
        active = preset;
    }

    public static void onServerStarted(ServerStartedEvent event) {
        Preset stored = LostCenturiesData.get(event.getServer()).preset();
        active = stored != null ? stored : Config.DEFAULT_PRESET.get();
    }

    public static void onServerStopped(ServerStoppedEvent event) {
        active = null;
    }

    /** Easy: keep players fully hydrated (also covers switching to easy mid-game). */
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 40 != 0 || active() != Preset.EASY) {
            return;
        }
        PlayerThirst thirst = player.getData(ModAttachment.PLAYER_THIRST.get());
        if (thirst.getThirst() < 20) {
            thirst.setThirst(20);
            thirst.setQuenched(Math.max(thirst.getQuenched(), 5));
            thirst.updateThirstData(player);
        }
    }
}
