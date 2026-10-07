package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;

/**
 * Hydrokinetic Engine: driven by a falling column of liquid beside it (on its left as you look down the shaft). The taller
 * the fall above it, the more power, as in the original: speed 2v and torque 1/16 x density x v^2, where
 * v = 0.92 sqrt(2 g h^1.5 / 32) (slower for thick liquids), capped at 32 rad/s and 16384 N*m (a fall of about 64 blocks).
 * Needs lubricant (1 mB every 10 ticks of running; it shares it along a row of hydro engines) and room for its blades
 * (a 3x3 disc of air or liquid across the column). Engines placed in a row, each driving the next, add their torque; more
 * than 4x the single-engine maximum breaks the blades (16x once upgraded with a bedrock rod, which also repairs them).
 * Lava sets it alight; creatures beside the blades get hurt.
 */
public class HydroEngineBlockEntity extends EngineBlockEntity {
    public static final int MAX_TORQUE = 16384;
    public static final int MAX_SPEED = 32;
    public static final int LUBE_CAPACITY = 24_000;
    private static final double GRAVITY = 9.81;

    private final FluidTank lubricant = new FluidTank(LUBE_CAPACITY, s -> s.is(RotaryFluids.LUBRICANT.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private boolean failed;
    private boolean bedrock;
    private int fallTorque;
    private int fallSpeed;
    private int ticks;

    public HydroEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.HYDRO_ENGINE.get(), pos, state);
    }

    public FluidTank lubricant() {
        return lubricant;
    }

    public boolean isFailed() {
        return failed;
    }

    public boolean isBedrock() {
        return bedrock;
    }

    /** The bedrock upgrade: stronger blades, and it mends broken ones. */
    public boolean makeBedrock() {
        if (bedrock && !failed) {
            return false;
        }
        bedrock = true;
        failed = false;
        setChanged();
        return true;
    }

    /** The side the falling liquid must be on: to the left looking along the output. */
    public Direction columnSide() {
        return facing().getCounterClockWise();
    }

    public IFluidHandler lubricantHandler(Direction side) {
        return side == facing() ? null : lubricant;
    }

    @Override
    protected int ratedTorque() {
        if (failed) {
            return 1;
        }
        int torque = (int) Math.min(Integer.MAX_VALUE, (long) fallTorque * arraySize());
        int limit = bedrock ? 16 : 4;
        if ((double) torque / MAX_TORQUE > limit) {
            failed = true;
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 1);
            level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.2F, 0.5F);
            setChanged();
            return 1;
        }
        return torque;
    }

    @Override
    protected int targetSpeed() {
        return Math.max(1, fallSpeed);
    }

    @Override
    protected boolean canRun() {
        if (lubricant.isEmpty() || level == null) {
            return false;
        }
        shareLubricant();
        if (bladesObstructed()) {
            return false;
        }
        BlockPos column = worldPosition.relative(columnSide());
        if (!isFallingColumn(column)) {
            fallTorque = 0;
            fallSpeed = 0;
            return false;
        }
        FluidType type = level.getFluidState(column).getFluidType();
        if (type.getTemperature() >= 900 && level.random.nextInt(50) == 0) {
            boolean lube = !lubricant.isEmpty();
            BlockPos p = worldPosition;
            level.removeBlock(p, false);
            level.explode(null, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, lube ? 3 : 2, lube,
                    RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
            return false;
        }
        if (type.getDensity() <= 0) {
            return false;
        }
        computeFall(column, type);
        return fallSpeed > 0;
    }

    private void computeFall(BlockPos column, FluidType type) {
        int top = column.getY();
        while (top < level.getMaxBuildHeight() && level.getFluidState(new BlockPos(column.getX(), top + 1, column.getZ())).getFluidType() == type) {
            top++;
        }
        int[] power = fallPower(top + 1 - worldPosition.getY(), type.getDensity(), type.getViscosity());
        fallSpeed = power[0];
        fallTorque = power[1];
    }

    /**
     * {speed, torque} for a fall of {@code h} blocks above the engine, as the original computes it:
     * v = 0.92 sqrt(2 g h') / max(0.25, (viscosity/1000)^0.375) with h' = h^1.5 / 32; speed 2v, torque density x v^2 / 16.
     */
    public static int[] fallPower(double h, int density, int viscosity) {
        double hEff = Math.pow(h, 1.5) / 32;
        double v = 0.92 * Math.sqrt(2 * GRAVITY * hEff) / Math.max(0.25, Math.pow(viscosity / 1000D, 0.375));
        return new int[]{(int) Math.min(2 * v, MAX_SPEED), (int) Math.min(0.0625 * Math.min(12000, density) * v * v, MAX_TORQUE)};
    }

    /** A falling (not still) liquid with the same falling liquid above and below it. */
    private boolean isFallingColumn(BlockPos p) {
        FluidState here = level.getFluidState(p);
        FluidState up = level.getFluidState(p.above());
        FluidState down = level.getFluidState(p.below());
        return !here.isEmpty() && !here.isSource() && up.getType().isSame(here.getType()) && !up.isSource()
                && down.getType().isSame(here.getType()) && !down.isSource();
    }

    private boolean soft(BlockPos p) {
        BlockState s = level.getBlockState(p);
        return failed || s.isAir() || s.canBeReplaced() || !s.getFluidState().isEmpty();
    }

    /** The blades sweep a 3x3 disc across the column: above, below and out to both sides must be clear. */
    private boolean bladesObstructed() {
        if (!soft(worldPosition.above()) || !soft(worldPosition.below())) {
            return true;
        }
        Direction side = columnSide();
        for (int d = -1; d <= 1; d++) {
            for (int i = -1; i <= 1; i++) {
                if (i == 0 && d == 0) {
                    continue;
                }
                if (!soft(worldPosition.relative(side, d).above(i))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** Engines behind this one in the row, turning at the same speed, add their torque. */
    private int arraySize() {
        int size = 1;
        BlockPos p = worldPosition.relative(inputSide());
        for (int n = 0; n < 64 && level.getBlockEntity(p) instanceof HydroEngineBlockEntity h && !h.failed; n++) {
            if (h.omega != omega || h.omega == 0) {
                break;
            }
            size++;
            p = p.relative(inputSide());
        }
        return size;
    }

    /** Evens lubricant out with the hydro engines in front of and behind it. */
    private void shareLubricant() {
        for (Direction d : new Direction[]{facing(), inputSide()}) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof HydroEngineBlockEntity other) {
                int diff = lubricant.getFluidAmount() - other.lubricant.getFluidAmount();
                if (diff > 3) {
                    int moved = other.lubricant.fill(lubricant.drain(diff / 4, IFluidHandler.FluidAction.EXECUTE), IFluidHandler.FluidAction.EXECUTE);
                    if (moved < diff / 4) {
                        lubricant.fill(new net.neoforged.neoforge.fluids.FluidStack(RotaryFluids.LUBRICANT.get(), diff / 4 - moved), IFluidHandler.FluidAction.EXECUTE);
                    }
                }
            }
        }
    }

    @Override
    protected void afterTick(boolean running) {
        ticks++;
        if (!running || omega <= 0) {
            return;
        }
        if (!failed && ticks % 10 == 0) {
            lubricant.drain(1, IFluidHandler.FluidAction.EXECUTE);
        }
        Direction side = columnSide();
        AABB box = new AABB(worldPosition).inflate(side.getAxis() == Direction.Axis.X ? 1 : 0, 1, side.getAxis() == Direction.Axis.Z ? 1 : 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box)) {
            e.hurt(level.damageSources().generic(), 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("lubricant", lubricant.writeToNBT(registries, new CompoundTag()));
        tag.putBoolean("failed", failed);
        tag.putBoolean("bedrock", bedrock);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lubricant.readFromNBT(registries, tag.getCompound("lubricant"));
        failed = tag.getBoolean("failed");
        bedrock = tag.getBoolean("bedrock");
    }

    @Override
    protected int statusKey() {
        return (failed ? 1 : 0) | (bedrock ? 2 : 0);
    }

    @Override
    protected void writeStatus(CompoundTag tag) {
        tag.putBoolean("failed", failed);
        tag.putBoolean("bedrock", bedrock);
    }

    @Override
    protected void readStatus(CompoundTag tag) {
        failed = tag.getBoolean("failed");
        bedrock = tag.getBoolean("bedrock");
    }
}
