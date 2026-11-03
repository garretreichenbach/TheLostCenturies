package com.thelostcenturies.core.survival;

import com.thelostcenturies.core.Lang;
import com.minecolonies.api.colony.ICitizenData;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.buildings.IBuilding;
import java.util.ArrayList;
import com.thelostcenturies.core.Config;
import com.thelostcenturies.core.LostCenturies;
import dev.ghen.thirst.content.purity.WaterPurity;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Colony drinking water. Every {@code checkIntervalTicks}, each active colony drinks
 * {@code citizens * waterPerCitizenPerDay} (scaled to the interval) from its reserve. The reserve refills from
 * water containers in the colony's warehouse(s), then the town hall's inventory, with at least {@code minPurity};
 * empty containers are put back. New colonies start with {@code startingReserveDays} of water.
 * If the warehouse is out of water the colony is dehydrated:
 * <ul>
 *   <li>normal: citizens get Slowness</li>
 *   <li>hard: workers are paused (only the ones we paused get unpaused again)</li>
 * </ul>
 */
public final class ColonyWater {
    private static final Logger LOG = LoggerFactory.getLogger(LostCenturies.MOD_ID);
    private static final ResourceLocation TERRACOTTA_BOWL = ResourceLocation.fromNamespaceAndPath("thirst", "terracotta_bowl");
    private static final ResourceLocation TERRACOTTA_WATER_BOWL = ResourceLocation.fromNamespaceAndPath("thirst", "terracotta_water_bowl");

    private ColonyWater() {}

    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        int interval = Config.COLONY_WATER_CHECK_INTERVAL.get();
        if (server.getTickCount() % interval != 0) {
            return;
        }
        Preset preset = SurvivalPresets.active();
        LostCenturiesData data = LostCenturiesData.get(server);

        for (IColony colony : IColonyManager.getInstance().getAllColonies()) {
            if (colony.getWorld() == null || !colony.isActive()) {
                continue; // unloaded colonies are frozen; they don't drink either
            }
            LostCenturiesData.ColonyWaterState state = data.colony(key(colony));
            if (!preset.colonyNeedsWater()) {
                if (state.dehydrated) {
                    recover(colony, state);
                }
                continue;
            }
            tickColony(colony, state, preset, interval);
        }
        data.setDirty();
    }

    private static void tickColony(IColony colony, LostCenturiesData.ColonyWaterState state, Preset preset, int interval) {
        List<ICitizenData> citizens = colony.getCitizenManager().getCitizens();
        double perDay = citizens.size() * Config.WATER_PER_CITIZEN_PER_DAY.get();
        if (!state.initialized) {
            // Grace period: a fresh colony has no warehouse yet.
            state.initialized = true;
            state.reserve = Math.max(state.reserve, Math.max(1, citizens.size()) * Config.WATER_PER_CITIZEN_PER_DAY.get()
                    * Config.STARTING_RESERVE_DAYS.get());
        }
        double need = perDay * interval / 24000.0;
        state.reserve -= need;
        if (state.reserve < 0) {
            state.reserve += pullFromStorage(colony, -state.reserve);
        }

        if (state.reserve >= 0) {
            if (state.dehydrated) {
                recover(colony, state);
            }
            return;
        }

        // Out of water: don't build up a debt, just stay dehydrated until water arrives.
        state.reserve = 0;
        if (!state.dehydrated) {
            state.dehydrated = true;
            notify(colony, Lang.tr(preset == Preset.HARD
                            ? "lostcenturies.colony_water.out.hard" : "lostcenturies.colony_water.out.normal",
                    colony.getName(), Config.MIN_PURITY.get()).withStyle(ChatFormatting.GOLD));
        }
        applyEffects(citizens, state, preset, interval);
    }

    private static void applyEffects(List<ICitizenData> citizens, LostCenturiesData.ColonyWaterState state, Preset preset, int interval) {
        for (ICitizenData citizen : citizens) {
            if (preset == Preset.HARD) {
                if (citizen.getJob() != null && !citizen.isPaused()) {
                    citizen.setPaused(true);
                    state.pausedByUs.add(citizen.getId());
                }
            } else {
                citizen.getEntity().ifPresent(entity -> entity.addEffect(
                        new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, interval + 100, 1, true, true)));
            }
        }
    }

    private static void recover(IColony colony, LostCenturiesData.ColonyWaterState state) {
        state.dehydrated = false;
        for (ICitizenData citizen : colony.getCitizenManager().getCitizens()) {
            if (state.pausedByUs.contains(citizen.getId()) && citizen.isPaused()) {
                citizen.setPaused(false);
            }
            citizen.getEntity().ifPresent(entity -> entity.removeEffect(MobEffects.MOVEMENT_SLOWDOWN));
        }
        state.pausedByUs.clear();
        notify(colony, Lang.tr("lostcenturies.colony_water.restored", colony.getName())
                .withStyle(ChatFormatting.GREEN));
    }

    /** Takes water containers from the warehouses, then the town hall, until {@code wanted} units are covered. */
    private static double pullFromStorage(IColony colony, double wanted) {
        double taken = 0;
        int minPurity = Config.MIN_PURITY.get();
        List<IBuilding> storage = new ArrayList<>(colony.getServerBuildingManager().getWareHouses());
        if (colony.getServerBuildingManager().hasTownHall()) {
            storage.add(colony.getServerBuildingManager().getTownHall());
        }
        for (IBuilding building : storage) {
            IItemHandler handler = building.getItemHandlerCap();
            if (handler == null) {
                continue;
            }
            for (int slot = 0; slot < handler.getSlots() && taken < wanted; slot++) {
                while (taken < wanted) {
                    ItemStack stack = handler.getStackInSlot(slot);
                    if (stack.isEmpty() || !isDrinkableWater(stack, minPurity)) {
                        break;
                    }
                    ItemStack extracted = handler.extractItem(slot, 1, false);
                    if (extracted.isEmpty()) {
                        break;
                    }
                    taken += units(extracted);
                    ItemStack empty = emptyContainer(extracted);
                    if (!empty.isEmpty()) {
                        ItemHandlerHelper.insertItemStacked(handler, empty, false); // dropped if no room
                    }
                }
            }
            if (taken >= wanted) {
                break;
            }
        }
        if (taken > 0) {
            LOG.debug("Colony {} drank {} water units from storage", colony.getName(), taken);
        }
        return taken;
    }

    private static boolean isDrinkableWater(ItemStack stack, int minPurity) {
        if (!WaterPurity.isWaterFilledContainer(stack)) {
            return false;
        }
        Integer purity = WaterPurity.getPurity(stack.copy()); // getPurity writes a default onto the stack
        return purity != null && purity >= minPurity;
    }

    private static int units(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) ? Config.BUCKET_UNITS.get() : 1;
    }

    private static ItemStack emptyContainer(ItemStack filled) {
        if (filled.is(Items.WATER_BUCKET)) {
            return new ItemStack(Items.BUCKET);
        }
        if (filled.is(Items.POTION)) {
            return new ItemStack(Items.GLASS_BOTTLE);
        }
        if (BuiltInRegistries.ITEM.getKey(filled.getItem()).equals(TERRACOTTA_WATER_BOWL)) {
            Item bowl = BuiltInRegistries.ITEM.get(TERRACOTTA_BOWL);
            return bowl == Items.AIR ? ItemStack.EMPTY : new ItemStack(bowl);
        }
        ItemStack remainder = filled.getCraftingRemainingItem();
        return remainder == null ? ItemStack.EMPTY : remainder;
    }

    private static void notify(IColony colony, Component message) {
        for (Player player : colony.getImportantMessageEntityPlayers()) {
            player.sendSystemMessage(message);
        }
    }

    static String key(IColony colony) {
        return colony.getDimension().location() + "#" + colony.getID();
    }

    /** Status line for /lostcenturies water. */
    public static Component status(IColony colony, LostCenturiesData data) {
        LostCenturiesData.ColonyWaterState state = data.colony(key(colony));
        int citizens = colony.getCitizenManager().getCitizens().size();
        double perDay = citizens * Config.WATER_PER_CITIZEN_PER_DAY.get();
        return Lang.tr("lostcenturies.colony_water.status", colony.getName(), citizens,
                String.format("%.1f", state.reserve), String.format("%.1f", perDay),
                Lang.tr(state.dehydrated ? "lostcenturies.colony_water.state.dry" : "lostcenturies.colony_water.state.ok"));
    }
}
