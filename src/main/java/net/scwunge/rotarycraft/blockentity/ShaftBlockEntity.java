package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.ShaftBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.transmission.PortalShafts;
import org.jetbrains.annotations.Nullable;

/** Carries power straight through, unchanged. Breaks when the load is beyond its material's limits. */
public class ShaftBlockEntity extends PowerBlockEntity {
    public ShaftBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.SHAFT.get(), pos, state);
    }

    public ShaftMaterial material() {
        return getBlockState().getBlock() instanceof ShaftBlock shaft ? shaft.material() : ShaftMaterial.STEEL;
    }

    // power handed through a portal from the shaft on its other side, and the chunks kept loaded for the link in front of this one
    private int feedTorque;
    private int feedOmega;
    private long feedTime = Long.MIN_VALUE / 2;
    @Nullable
    private ServerLevel ticketLevel;
    private it.unimi.dsi.fastutil.longs.LongArrayList ticketChunks = new it.unimi.dsi.fastutil.longs.LongArrayList();

    /** Power arrives from the shaft on the other side of a portal behind this one. */
    public void feed(int torque, int omega, long time) {
        feedTorque = torque;
        feedOmega = omega;
        feedTime = time;
    }

    public int feedTorque() {
        return feedTorque;
    }

    public int feedOmega() {
        return feedOmega;
    }

    public long feedTime() {
        return feedTime;
    }

    @Nullable
    public ServerLevel ticketLevel() {
        return ticketLevel;
    }

    public it.unimi.dsi.fastutil.longs.LongArrayList ticketChunks() {
        return ticketChunks;
    }

    public void setTickets(@Nullable ServerLevel level, it.unimi.dsi.fastutil.longs.LongArrayList chunks) {
        ticketLevel = level;
        ticketChunks = chunks;
    }

    @Override
    public void setRemoved() {
        PortalShafts.release(this);
        super.setRemoved();
    }

    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = PortalShafts.input(this, readInput());
        if (RotaryConfig.get(RotaryConfig.SHAFT_FAILURE) && material().fails(in.torque(), in.omega())) {
            fail(level, worldPosition, in);
            return;
        }
        setPower(in.torque(), in.omega());
        PortalShafts.carry(this);
    }

    /** The shaft snaps: sound, smoke, and the block is gone (it drops nothing). */
    static void fail(net.minecraft.world.level.Level level, BlockPos pos, IShaftPowerOutput.Reading load) {
        if (level instanceof ServerLevel server) {
            server.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1.0F, 0.6F);
            server.sendParticles(ParticleTypes.LARGE_SMOKE, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 12, 0.25, 0.25, 0.25, 0.02);
        }
        level.destroyBlock(pos, false);
    }
}
