package net.scwunge.rotarycraft.weapon;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Keeps weapons and world-changing machines server-safe: before they break or change a block they ask, as the machine's owner,
 * whether that is allowed. The mobGriefing rule must be on, and the break event (which claim and protection mods listen to) must
 * not be cancelled for a fake player standing in for the owner.
 */
public final class WorldGuard {
    private static final GameProfile NO_OWNER = new GameProfile(UUID.fromString("41c82c87-7afb-4024-ba57-13d2c99cae77"), "[RotaryCraft]");

    private WorldGuard() {}

    /** The fake player that acts for a machine owned by {@code owner} (or for ownerless machines). */
    public static FakePlayer actor(ServerLevel level, @Nullable Owner owner) {
        return FakePlayerFactory.get(level, owner == null ? NO_OWNER : owner.profile());
    }

    /** Whether a machine owned by {@code owner} may break or change the block at {@code pos}. */
    public static boolean mayChange(ServerLevel level, BlockPos pos, @Nullable Owner owner) {
        if (!level.getGameRules().getBoolean(GameRules.RULE_MOBGRIEFING) || !level.isLoaded(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        if (state.isAir()) {
            return true;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, pos, state, actor(level, owner));
        NeoForge.EVENT_BUS.post(event);
        return !event.isCanceled();
    }

    /** Breaks the block at {@code pos} (dropping it if {@code drop}) if the owner may; returns whether it did. */
    public static boolean breakBlock(ServerLevel level, BlockPos pos, @Nullable Owner owner, boolean drop) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || state.getDestroySpeed(level, pos) < 0 || !mayChange(level, pos, owner)) {
            return false;
        }
        return level.destroyBlock(pos, drop);
    }

    /** Replaces the block at {@code pos} if the owner may; returns whether it did. */
    public static boolean setBlock(ServerLevel level, BlockPos pos, BlockState to, @Nullable Owner owner) {
        BlockState state = level.getBlockState(pos);
        if (state.getDestroySpeed(level, pos) < 0 || !mayChange(level, pos, owner)) {
            return false;
        }
        return level.setBlockAndUpdate(pos, to);
    }

    /** Who placed a machine. */
    public record Owner(UUID id, String name) {
        public GameProfile profile() {
            return new GameProfile(id, name);
        }
    }
}
