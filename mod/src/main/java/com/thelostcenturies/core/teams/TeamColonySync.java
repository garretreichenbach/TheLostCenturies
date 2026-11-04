package com.thelostcenturies.core.teams;

import com.minecolonies.api.IMinecoloniesAPI;
import com.minecolonies.api.colony.IColony;
import com.minecolonies.api.colony.IColonyManager;
import com.minecolonies.api.colony.permissions.IPermissions;
import com.minecolonies.api.colony.permissions.Rank;
import com.minecolonies.api.eventbus.events.colony.ColonyCreatedModEvent;
import com.mojang.authlib.GameProfile;
import com.thelostcenturies.core.Config;
import com.thelostcenturies.core.Lang;
import com.thelostcenturies.core.LostCenturies;
import dev.ftb.mods.ftbteams.api.FTBTeamsAPI;
import dev.ftb.mods.ftbteams.api.Team;
import dev.ftb.mods.ftbteams.api.event.PlayerLeftPartyTeamEvent;
import dev.ftb.mods.ftbteams.api.event.TeamEvent;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One colony per team: members of an FTB Teams party are added to the colony owned by any member of that party,
 * and removed when they leave the party. Runs when someone joins/leaves a party, on login, and when a colony is
 * founded (so a party formed before founding is picked up too).
 *
 * <p>Never demotes: members who already are officers (or the owner) keep their rank. Removal on leaving only
 * applies to players whose rank is still the auto-assigned one.
 */
public final class TeamColonySync {
    private static final Logger LOG = LoggerFactory.getLogger(LostCenturies.MOD_ID);

    public enum AutoRank { OFFICER, FRIEND }

    private TeamColonySync() {}

    /** Called from the mod constructor. */
    public static void init() {
        TeamEvent.PLAYER_JOINED_PARTY.register(e -> sync(e.getTeam()));
        TeamEvent.PLAYER_LOGGED_IN.register(e -> sync(e.getTeam()));
        TeamEvent.PLAYER_LEFT_PARTY.register(TeamColonySync::onLeftParty);
    }

    /** MineColonies' own event bus; subscribe once its API is up. */
    public static void onCommonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> IMinecoloniesAPI.getInstance().getEventBus()
                .subscribe(ColonyCreatedModEvent.class, e -> onColonyCreated(e.getColony())));
    }

    private static void onColonyCreated(IColony colony) {
        if (!enabled() || !FTBTeamsAPI.api().isManagerLoaded()) {
            return;
        }
        UUID owner = colony.getPermissions().getOwner();
        if (owner != null) {
            FTBTeamsAPI.api().getManager().getTeamForPlayerID(owner).ifPresent(TeamColonySync::sync);
        }
    }

    /** Adds every member of a party team to the colonies owned by its members. */
    public static void sync(Team team) {
        if (!enabled() || team == null || !team.isPartyTeam()) {
            return;
        }
        Set<UUID> members = team.getMembers();
        for (IColony colony : IColonyManager.getInstance().getAllColonies()) {
            IPermissions perms = colony.getPermissions();
            UUID owner = perms.getOwner();
            if (owner == null || !members.contains(owner)) {
                continue;
            }
            Rank target = autoRank(perms);
            for (UUID member : members) {
                if (member.equals(owner) || hasAtLeast(perms, member, target)) {
                    continue;
                }
                boolean changed = perms.getPlayers().containsKey(member)
                        ? perms.setPlayerRank(member, target, colony.getWorld())
                        : perms.addPlayer(member, name(member), target);
                if (changed) {
                    LOG.info("Added {} to colony {} as {} (FTB team {})", name(member), colony.getName(), target.getName(), team.getShortName());
                    online(member).ifPresent(p -> p.sendSystemMessage(Lang.tr("lostcenturies.teams.joined",
                            colony.getName(), target.getName()).withStyle(ChatFormatting.GREEN)));
                }
            }
        }
    }

    private static void onLeftParty(PlayerLeftPartyTeamEvent event) {
        if (!enabled() || !FTBTeamsAPI.api().isManagerLoaded()) {
            return;
        }
        UUID player = event.getPlayerId();
        for (IColony colony : IColonyManager.getInstance().getAllColonies()) {
            IPermissions perms = colony.getPermissions();
            UUID owner = perms.getOwner();
            if (owner == null || owner.equals(player) || !perms.getPlayers().containsKey(player)) {
                continue;
            }
            // Only remove players we'd have added: same rank as the auto rank, and no longer in the owner's party.
            if (perms.getRank(player).getId() != autoRank(perms).getId()) {
                continue;
            }
            boolean stillTeammates = FTBTeamsAPI.api().getManager().getTeamForPlayerID(owner)
                    .map(t -> t.isPartyTeam() && t.getMembers().contains(player))
                    .orElse(false);
            if (!stillTeammates && perms.removePlayer(player)) {
                LOG.info("Removed {} from colony {} (left FTB team)", name(player), colony.getName());
                online(player).ifPresent(p -> p.sendSystemMessage(Lang.tr("lostcenturies.teams.left", colony.getName())
                        .withStyle(ChatFormatting.YELLOW)));
            }
        }
    }

    private static boolean hasAtLeast(IPermissions perms, UUID player, Rank target) {
        if (!perms.getPlayers().containsKey(player)) {
            return false;
        }
        int id = perms.getRank(player).getId();
        return id == perms.getRankOwner().getId() || id == perms.getRankOfficer().getId() || id == target.getId();
    }

    private static Rank autoRank(IPermissions perms) {
        return Config.TEAMS_RANK.get() == AutoRank.FRIEND ? perms.getRankFriend() : perms.getRankOfficer();
    }

    private static boolean enabled() {
        return Config.SPEC.isLoaded() && Config.TEAMS_AUTO_JOIN.get();
    }

    private static Optional<ServerPlayer> online(UUID id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        return server == null ? Optional.empty() : Optional.ofNullable(server.getPlayerList().getPlayer(id));
    }

    private static String name(UUID id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) {
            ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) {
                return p.getGameProfile().getName();
            }
            if (server.getProfileCache() != null) {
                return server.getProfileCache().get(id).map(GameProfile::getName).orElse(id.toString());
            }
        }
        return id.toString();
    }
}
