package net.scwunge.rotarycraft.transmission;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EndPortalBlock;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.ShaftBlockEntity;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import org.jetbrains.annotations.Nullable;

/**
 * Portal shafts, as the original's: a shaft whose output points into a nether or end portal hands its power through to the shaft standing just
 * beyond the same portal on the other side (the nether is eight times smaller across, so the place is divided or multiplied by eight; the
 * height stays), when that shaft points back at the portal. The far side is kept loaded while the link stands. Nothing else is needed: any
 * shaft will do for either end.
 */
public final class PortalShafts {
    /** Chunks kept for a portal link. A ticket is only ever held by a live shaft, so on loading a world every old one is let go (the shafts ask again). */
    public static final TicketController TICKETS = new TicketController(RotaryCraft.id("portal_shaft"),
            (level, helper) -> new java.util.ArrayList<>(helper.getBlockTickets().keySet()).forEach(helper::removeAllTickets));
    /** How long (ticks) power handed through a portal lasts without being renewed. */
    public static final int FEED_TICKS = 3;

    private PortalShafts() {}

    public static boolean isPortal(BlockState state) {
        return state.getBlock() instanceof NetherPortalBlock || state.getBlock() instanceof EndPortalBlock;
    }

    /** The level on the other side of this kind of portal, or null. */
    @Nullable
    public static ServerLevel otherSide(ServerLevel level, BlockState portal) {
        var key = level.dimension();
        if (portal.getBlock() instanceof NetherPortalBlock) {
            return level.getServer().getLevel(key == Level.NETHER ? Level.OVERWORLD : key == Level.OVERWORLD ? Level.NETHER : null);
        }
        if (portal.getBlock() instanceof EndPortalBlock) {
            return level.getServer().getLevel(key == Level.END ? Level.OVERWORLD : key == Level.OVERWORLD ? Level.END : null);
        }
        return null;
    }

    /** Where the portal block at {@code pos} stands on the other side: x and z divided by eight going to the nether, multiplied coming back; the height is kept (within the level). */
    public static BlockPos across(BlockPos pos, ServerLevel from, ServerLevel to) {
        int x = pos.getX();
        int z = pos.getZ();
        if (from.dimension() != Level.NETHER && to.dimension() == Level.NETHER) {
            x = Math.floorDiv(x, 8);
            z = Math.floorDiv(z, 8);
        } else if (from.dimension() == Level.NETHER && to.dimension() != Level.NETHER) {
            x *= 8;
            z *= 8;
        }
        return new BlockPos(x, Mth.clamp(pos.getY(), to.getMinBuildHeight(), to.getMaxBuildHeight() - 1), z);
    }

    /** What a shaft takes in: if nothing comes in at its back but it stands against a portal that is handing power through, that. */
    public static IShaftPowerOutput.Reading input(ShaftBlockEntity shaft, IShaftPowerOutput.Reading in) {
        Level level = shaft.getLevel();
        if (in.power() > 0 || level == null || level.getGameTime() - shaft.feedTime() > FEED_TICKS) {
            return in;
        }
        if (!isPortal(level.getBlockState(shaft.getBlockPos().relative(shaft.inputSide())))) {
            return in;
        }
        return new IShaftPowerOutput.Reading(shaft.feedTorque(), shaft.feedOmega());
    }

    /** Passes a shaft's power through the portal in front of it, to the shaft beyond, and keeps that place loaded. */
    public static void carry(ShaftBlockEntity shaft) {
        if (!(shaft.getLevel() instanceof ServerLevel level)) {
            return;
        }
        BlockPos front = shaft.getBlockPos().relative(shaft.facing());
        BlockState portal = level.getBlockState(front);
        ServerLevel other = isPortal(portal) ? otherSide(level, portal) : null;
        if (other == null) {
            release(shaft);
            return;
        }
        BlockPos beyond = across(front, level, other);
        BlockPos receiver = beyond.relative(shaft.facing());
        hold(shaft, other, beyond, receiver);
        if (!other.isLoaded(beyond) || !other.isLoaded(receiver) || other.getBlockState(beyond).getBlock() != portal.getBlock()) {
            return;
        }
        if (other.getBlockEntity(receiver) instanceof ShaftBlockEntity far && far.facing() == shaft.facing()) {
            far.feed(shaft.getTorque(), shaft.getOmega(), other.getGameTime());
        }
    }

    private static void hold(ShaftBlockEntity shaft, ServerLevel other, BlockPos beyond, BlockPos receiver) {
        LongArrayList wanted = new LongArrayList();
        wanted.add(ChunkPos.asLong(beyond));
        long second = ChunkPos.asLong(receiver);
        if (!wanted.contains(second)) {
            wanted.add(second);
        }
        if (shaft.ticketLevel() == other && wanted.equals(shaft.ticketChunks())) {
            return;
        }
        release(shaft);
        for (long chunk : wanted) {
            TICKETS.forceChunk(other, shaft.getBlockPos(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), true, true);
        }
        shaft.setTickets(other, wanted);
    }

    /** Lets go of the chunks held for this shaft's link. */
    public static void release(ShaftBlockEntity shaft) {
        ServerLevel held = shaft.ticketLevel();
        if (held != null) {
            for (long chunk : shaft.ticketChunks()) {
                TICKETS.forceChunk(held, shaft.getBlockPos(), ChunkPos.getX(chunk), ChunkPos.getZ(chunk), false, true);
            }
            shaft.setTickets(null, new LongArrayList());
        }
    }
}
