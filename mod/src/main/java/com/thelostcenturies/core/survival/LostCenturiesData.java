package com.thelostcenturies.core.survival;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/** Per-world state, saved in data/lostcenturies.dat of the overworld. */
public final class LostCenturiesData extends SavedData {
    private static final String NAME = "lostcenturies";

    /** Null until set by command: the server config's default applies. */
    private Preset preset;
    private final Map<String, ColonyWaterState> colonies = new HashMap<>();

    public static final class ColonyWaterState {
        /** Water units in the colony's reserve (already pulled from the warehouse). */
        public double reserve;
        public boolean dehydrated;
        /** False until the colony got its starting reserve. */
        public boolean initialized;
        /** Citizens we paused (hard preset), so we only unpause those. */
        public final Set<Integer> pausedByUs = new HashSet<>();
    }

    public static LostCenturiesData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(LostCenturiesData::new, LostCenturiesData::load, null), NAME);
    }

    public Preset preset() {
        return preset;
    }

    public void setPreset(Preset preset) {
        this.preset = preset;
        setDirty();
    }

    public ColonyWaterState colony(String key) {
        return colonies.computeIfAbsent(key, k -> new ColonyWaterState());
    }

    public Map<String, ColonyWaterState> colonies() {
        return colonies;
    }

    private static LostCenturiesData load(CompoundTag tag, HolderLookup.Provider registries) {
        LostCenturiesData data = new LostCenturiesData();
        if (tag.contains("preset")) {
            data.preset = Preset.byId(tag.getString("preset"));
        }
        CompoundTag cols = tag.getCompound("colonyWater");
        for (String key : cols.getAllKeys()) {
            CompoundTag c = cols.getCompound(key);
            ColonyWaterState state = data.colony(key);
            state.reserve = c.getDouble("reserve");
            state.dehydrated = c.getBoolean("dehydrated");
            state.initialized = c.getBoolean("initialized");
            for (int id : c.getIntArray("pausedByUs")) {
                state.pausedByUs.add(id);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        if (preset != null) {
            tag.putString("preset", preset.id());
        }
        CompoundTag cols = new CompoundTag();
        colonies.forEach((key, state) -> {
            CompoundTag c = new CompoundTag();
            c.putDouble("reserve", state.reserve);
            c.putBoolean("dehydrated", state.dehydrated);
            c.putBoolean("initialized", state.initialized);
            c.put("pausedByUs", new IntArrayTag(state.pausedByUs.stream().mapToInt(Integer::intValue).toArray()));
            cols.put(key, c);
        });
        tag.put("colonyWater", cols);
        return tag;
    }
}
