package com.thelostcenturies.core.command;

import com.thelostcenturies.core.Lang;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.thelostcenturies.core.survival.ColonyWater;
import com.thelostcenturies.core.survival.LostCenturiesData;
import com.thelostcenturies.core.survival.Preset;
import com.thelostcenturies.core.survival.SurvivalPresets;
import java.util.Arrays;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/**
 * /lostcenturies preset                       show the world's survival preset
 * /lostcenturies preset easy|normal|hard      set it (op)
 * /lostcenturies water                        colony water status
 */
public final class LostCenturiesCommand {
    private LostCenturiesCommand() {}

    public static void register(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> dispatcher = event.getDispatcher();
        dispatcher.register(Commands.literal("lostcenturies")
                .then(Commands.literal("preset")
                        .executes(ctx -> {
                            Preset p = SurvivalPresets.active();
                            ctx.getSource().sendSuccess(() -> Lang.tr("lostcenturies.preset.current",
                                    Lang.tr("lostcenturies.preset." + p.id())), false);
                            return 1;
                        })
                        .then(Commands.argument("preset", StringArgumentType.word())
                                .requires(src -> src.hasPermission(2))
                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(
                                        Arrays.stream(Preset.values()).map(Preset::id), builder))
                                .executes(ctx -> {
                                    Preset p = Preset.byId(StringArgumentType.getString(ctx, "preset"));
                                    if (p == null) {
                                        ctx.getSource().sendFailure(Lang.tr("lostcenturies.preset.unknown"));
                                        return 0;
                                    }
                                    SurvivalPresets.set(ctx.getSource().getServer(), p);
                                    ctx.getSource().sendSuccess(() -> Lang.tr("lostcenturies.preset.set",
                                            Lang.tr("lostcenturies.preset." + p.id())), true);
                                    return 1;
                                })))
                .then(Commands.literal("water")
                        .executes(ctx -> {
                            LostCenturiesData data = LostCenturiesData.get(ctx.getSource().getServer());
                            var colonies = IColonyManager.getInstance().getAllColonies();
                            if (colonies.isEmpty()) {
                                ctx.getSource().sendSuccess(() -> Lang.tr("lostcenturies.colony_water.none"), false);
                            }
                            for (IColony colony : colonies) {
                                ctx.getSource().sendSuccess(() -> ColonyWater.status(colony, data), false);
                            }
                            return colonies.size();
                        })));
    }
}
