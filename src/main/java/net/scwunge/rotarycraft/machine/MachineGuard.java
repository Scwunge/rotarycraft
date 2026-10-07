package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Asks claim and protection mods before a machine builds or breaks a block, as the machine's owner (a fake player standing in for them): the
 * mobGriefing rule must be on, and neither a break event nor a place event at the position may be cancelled. Unlike {@link WorldGuard#mayChange}
 * it asks about empty positions too, since a claim can forbid building in thin air.
 */
public final class MachineGuard {
    private MachineGuard() {
    }

    public static boolean mayChange(ServerLevel level, BlockPos pos, @Nullable WorldGuard.Owner owner) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || !level.isLoaded(pos)) {
            return false;
        }
        FakePlayer actor = WorldGuard.actor(level, owner);
        BlockState state = level.getBlockState(pos);
        BlockEvent.BreakEvent breaking = new BlockEvent.BreakEvent(level, pos, state, actor);
        NeoForge.EVENT_BUS.post(breaking);
        if (breaking.isCanceled()) {
            return false;
        }
        BlockEvent.EntityPlaceEvent placing = new BlockEvent.EntityPlaceEvent(net.neoforged.neoforge.common.util.BlockSnapshot.create(level.dimension(), level, pos),
                state, actor);
        NeoForge.EVENT_BUS.post(placing);
        return !placing.isCanceled();
    }
}
