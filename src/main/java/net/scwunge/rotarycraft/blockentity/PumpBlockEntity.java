package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Pump: needs 8 N*m and 1 kW. It maps the pool of liquid under it (up to 16 blocks out, the two layers below) and every
 * 300 - 30 x log2(speed) ticks takes one source block from it, furthest first, into its 24-bucket tank. Water comes out
 * multiplied with power (x2 at 16x the minimum, then at 64x, 256x, 1024x and 4096x), as in the original. It gives fluid out
 * of its four sides; how full it lets the pipes beside it get rises with torque. Creatures under the intake get hurt and
 * slowly wear it out (400 hits break it).
 */
public class PumpBlockEntity extends ConsumerBlockEntity implements PipeBlockEntity.PumpLimit {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(8, 1, 1024);
    public static final int CAPACITY = 24_000;
    private static final int RANGE = 16;
    private static final int BREAK_DAMAGE = 400;

    private final FluidTank tank = new FluidTank(CAPACITY) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final ArrayDeque<BlockPos> pool = new ArrayDeque<>();
    private int timer;
    private int damage;

    public PumpBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.PUMP.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public FluidTank tank() {
        return tank;
    }

    public int damage() {
        return damage;
    }

    public boolean isBroken() {
        return damage >= BREAK_DAMAGE;
    }

    /** The four sides give fluid; top and bottom nothing. */
    public IFluidHandler output(Direction side) {
        return side == null || side.getAxis().isHorizontal() ? FluidAccess.drainOnly(tank) : null;
    }

    @Override
    public int maxBackPressure() {
        return 1000 * (1 + (int) (Math.log(Math.max(1, torque / (double) REQUIREMENT.minTorque())) / Math.log(2)));
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered || isBroken()) {
            timer = 0;
            return;
        }
        hurtCreatures();
        FluidState below = level.getFluidState(worldPosition.below());
        if (below.isEmpty()) {
            pool.clear();
            return;
        }
        if (pool.isEmpty()) {
            mapPool(below.getType());
        }
        if (++timer < PowerRequirement.operationTime(300, 30, omega) || tank.getFluidAmount() >= CAPACITY) {
            return;
        }
        timer = 0;
        while (!pool.isEmpty()) {
            BlockPos p = pool.poll();
            if (harvest(p)) {
                break;
            }
        }
    }

    /** The connected liquid under the pump, nearest last so the far edge is taken first. */
    private void mapPool(Fluid type) {
        List<BlockPos> found = new ArrayList<>();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition.below());
        int x0 = worldPosition.getX();
        int z0 = worldPosition.getZ();
        int yTop = worldPosition.getY() - 1;
        while (!queue.isEmpty() && found.size() < 4096) {
            BlockPos p = queue.poll();
            if (!seen.add(p) || Math.abs(p.getX() - x0) > RANGE || Math.abs(p.getZ() - z0) > RANGE || p.getY() > yTop || p.getY() < yTop - 1) {
                continue;
            }
            if (!level.getFluidState(p).getType().isSame(type)) {
                continue;
            }
            found.add(p);
            for (Direction d : Direction.values()) {
                queue.add(p.relative(d));
            }
        }
        for (int i = found.size() - 1; i >= 0; i--) {
            pool.add(found.get(i));
        }
    }

    private boolean harvest(BlockPos p) {
        FluidState fs = level.getFluidState(p);
        if (fs.isEmpty() || !fs.isSource()) {
            return false;
        }
        FluidStack taken = new FluidStack(fs.getType(), 1000);
        if (!tank.isEmpty() && !FluidStack.isSameFluidSameComponents(tank.getFluid(), taken)) {
            return false;
        }
        int mult = 1;
        if (fs.getType().isSame(Fluids.WATER)) {
            long ratio = getPower() / REQUIREMENT.minPower();
            for (long step : new long[]{16, 64, 256, 1024, 4096}) {
                if (ratio >= step) {
                    mult *= 2;
                }
            }
        }
        tank.fill(taken.copyWithAmount(Math.min(CAPACITY - tank.getFluidAmount(), 1000 * mult)), IFluidHandler.FluidAction.EXECUTE);
        level.setBlockAndUpdate(p, Blocks.AIR.defaultBlockState());
        return true;
    }

    private void hurtCreatures() {
        List<LivingEntity> caught = level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition.below()));
        boolean hitLiving = false;
        for (LivingEntity e : caught) {
            e.hurt(level.damageSources().generic(), 5);
            hitLiving |= e.isAlive();
        }
        if (hitLiving && ++damage >= BREAK_DAMAGE) {
            level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 1);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("damage", damage);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tank.readFromNBT(registries, tag.getCompound("tank"));
        damage = tag.getInt("damage");
    }
}
