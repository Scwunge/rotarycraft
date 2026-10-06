package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.block.ReservoirBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/**
 * Reservoir: a 64-bucket open tank. As in the original: fluid goes in from the sides and top and comes out of the bottom;
 * neighbouring reservoirs share their contents; an open one collects rain; a very hot fluid (over 2500 C) melts it into
 * lava; a flammable one (fuels) explodes next to fire or lava; creatures that step in are burnt by hot fluids, chilled by
 * very cold ones, and set it off if they are burning and it holds fuel. A covered reservoir (crafted with a glass pane)
 * keeps out rain and creatures.
 */
public class ReservoirBlockEntity extends BlockEntity {
    public static final int CAPACITY = 64_000;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            dirtyForClient = true;
        }
    };
    private boolean dirtyForClient;
    private int ticks;

    public ReservoirBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.RESERVOIR.get(), pos, state);
    }

    public FluidTank tank() {
        return tank;
    }

    public boolean covered() {
        return getBlockState().getValue(ReservoirBlock.COVERED);
    }

    /** Out of the bottom only; in from every other side. */
    public IFluidHandler handler(Direction side) {
        return side == Direction.DOWN ? FluidAccess.drainOnly(tank) : FluidAccess.fillOnly(tank);
    }

    public void serverTick() {
        ticks++;
        shareWithNeighbours();
        if (!covered() && level.isRaining() && level.canSeeSky(worldPosition.above())
                && level.getBiome(worldPosition).value().getPrecipitationAt(worldPosition) == Biome.Precipitation.RAIN
                && (tank.isEmpty() || tank.getFluid().is(Fluids.WATER))) {
            tank.fill(new FluidStack(Fluids.WATER, 25), IFluidHandler.FluidAction.EXECUTE);
        }
        if (ticks % (covered() ? 30 : 20) == 0 && !tank.isEmpty() && !surrounded()) {
            fluidEffects();
        }
        if (dirtyForClient && ticks % 5 == 0) {
            dirtyForClient = false;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    private void shareWithNeighbours() {
        if (tank.getFluidAmount() >= CAPACITY) {
            return;
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof ReservoirBlockEntity other && !other.tank.isEmpty()
                    && (tank.isEmpty() || FluidStack.isSameFluidSameComponents(tank.getFluid(), other.tank.getFluid()))) {
                int diff = other.tank.getFluidAmount() - tank.getFluidAmount();
                if (diff > 1) {
                    tank.fill(other.tank.drain(diff / 2, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
    }

    private boolean surrounded() {
        for (Direction d : Direction.Plane.HORIZONTAL) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (!(s.getBlock() instanceof ReservoirBlock) && !s.isSolidRender(level, worldPosition.relative(d))) {
                return false;
            }
        }
        return true;
    }

    private boolean flammable() {
        return tank.getFluid().is(PipeType.FUELS);
    }

    private int celsius() {
        return tank.getFluid().getFluidType().getTemperature(tank.getFluid()) - 273;
    }

    private void fluidEffects() {
        int temp = celsius();
        if (temp > 2500) {
            level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.4F, 1);
            BlockState lava = Blocks.LAVA.defaultBlockState();
            level.setBlockAndUpdate(worldPosition, lava);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                level.setBlockAndUpdate(worldPosition.relative(d), lava);
            }
            return;
        }
        if (flammable() && (Ambient.temperature(level, worldPosition) >= 300 || nextToFireOrLava())) {
            explode();
        }
    }

    private boolean nextToFireOrLava() {
        for (Direction d : Direction.values()) {
            BlockState s = level.getBlockState(worldPosition.relative(d));
            if (s.getBlock() instanceof BaseFireBlock || s.getFluidState().is(FluidTags.LAVA)) {
                return true;
            }
        }
        return false;
    }

    private void explode() {
        Level lvl = level;
        BlockPos p = worldPosition;
        lvl.removeBlock(p, false);
        lvl.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 4, true,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
    }

    /** A creature standing in the open reservoir. */
    public void entityInside(Entity e) {
        if (covered() || tank.isEmpty() || !(e instanceof LivingEntity living)) {
            return;
        }
        int kelvin = tank.getFluid().getFluidType().getTemperature(tank.getFluid());
        if (tank.getFluid().is(FluidTags.LAVA) || kelvin > 500) {
            living.hurt(level.damageSources().lava(), 4);
            living.igniteForSeconds(12);
        }
        if (kelvin < 250) {
            living.hurt(level.damageSources().freeze(), 1);
        }
        if (living.isOnFire() && flammable()) {
            explode();
        }
    }

    // client sync for the fluid renderer --------------------------------------------------------------------------------

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }
}
