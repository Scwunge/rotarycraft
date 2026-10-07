package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Spillway (TileEntitySpillway): an 8 bucket tank that takes the water from the side it faces, with no power. Water falling there gives 250 mB a tick
 * without being used up, and so does a source with water above it (50, scaled by the config); a pool of still water is drained a bucket a tick (the block nearest
 * and highest first, within 64 blocks across and 24 up, and nothing the owner's claims forbid). Water that gets above the spillway is removed. Pipes
 * take the water from underneath. Off unless the server enables it, as it removes blocks.
 */
public class SpillwayBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "spillway";
    public static final int CAPACITY = 8000;
    public static final int STREAM = 250;
    public static final int COLUMN = 50;
    public static final int POOL_RADIUS = 64;
    public static final int POOL_HEIGHT = 24;
    public static final int POOL_LIMIT = 4096;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(0, 0, 0);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).tanks(1).build();

    private final FluidTank tank = addTank(CAPACITY, s -> s.getFluid() == Fluids.WATER);
    private final IFluidHandler drainOnly = drainOnly(tank);
    private final List<BlockPos> pool = new ArrayList<>();
    private int active;

    public SpillwayBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.SPILLWAY.type().get(), pos, state, 0, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    public FluidTank tank() {
        return tank;
    }

    /** Pipes take water from underneath. */
    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side == Direction.DOWN ? drainOnly : null;
    }

    /** The side it takes water from: the way it faces, taken as a horizontal one. */
    public Direction drainSide() {
        Direction facing = facing();
        return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
    }

    public boolean isActive() {
        return active > 0;
    }

    private void markActive() {
        boolean was = active > 0;
        active = 4;
        if (!was) {
            markClientDirty();
        }
    }

    /** Water coming down in a stream: flowing, and falling. (Water that has spread sideways into the gap a drained pool left is not one.) */
    private static boolean falling(FluidState state) {
        return isWater(state) && !state.isSource() && state.getValue(net.minecraft.world.level.material.FlowingFluid.FALLING);
    }

    private int add(int amount) {
        return tank.fill(new FluidStack(Fluids.WATER, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    private static boolean isWater(FluidState state) {
        return state.is(FluidTags.WATER);
    }

    /** Finds the still water that joins the position, in the order it is to be drained. */
    private void findPool(ServerLevel server, BlockPos from) {
        pool.clear();
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(from);
        seen.add(from);
        while (!queue.isEmpty() && pool.size() < POOL_LIMIT) {
            BlockPos at = queue.poll();
            FluidState state = server.getFluidState(at);
            if (!isWater(state) || !state.isSource() || !server.getBlockState(at).getBlock().equals(Blocks.WATER)) {
                continue;
            }
            pool.add(at);
            for (Direction dir : Direction.values()) {
                BlockPos next = at.relative(dir);
                if (seen.add(next) && Math.abs(next.getX() - worldPosition.getX()) <= POOL_RADIUS && Math.abs(next.getZ() - worldPosition.getZ()) <= POOL_RADIUS
                        && next.getY() >= worldPosition.getY() && next.getY() <= worldPosition.getY() + POOL_HEIGHT) {
                    queue.add(next);
                }
            }
        }
        // nearest first, and of those the highest
        pool.sort(Comparator.<BlockPos>comparingInt(p -> (int) p.distSqr(worldPosition)).thenComparing(p -> -p.getY()));
    }

    /** How many source blocks it has found to drain. */
    public int poolSize() {
        return pool.size();
    }

    private void drainPool(ServerLevel server, BlockPos drain) {
        if (pool.isEmpty() || !server.getFluidState(pool.get(0)).isSource()) {
            findPool(server, drain);
        }
        if (pool.isEmpty() || tank.getSpace() < 1000) {
            return;
        }
        BlockPos next = pool.remove(0);
        if (!isWater(server.getFluidState(next))) {
            return;
        }
        if (!MachineGuard.mayChange(server, next, owner)) {
            return;
        }
        server.setBlock(next, Blocks.AIR.defaultBlockState(), 3);
        add(1000);
        markActive();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !MachineConfig.enabled("spillway")) {
            return;
        }
        BlockPos drain = worldPosition.relative(drainSide());
        FluidState at = server.getFluidState(drain);
        FluidState above = server.getFluidState(drain.above());
        boolean stream = falling(at) || falling(above);
        if (stream) {
            pool.clear();
            if (tank.getSpace() >= STREAM && add(STREAM) > 0) {
                markActive();
            }
        } else if (isWater(at) && isWater(above)) {
            pool.clear();
            if (tank.getSpace() >= COLUMN && add(COLUMN) > 0) {
                markActive();
            }
        } else if (isWater(at)) {
            drainPool(server, drain);
        } else if (!pool.isEmpty()) {
            // the gap it left is not filled yet: go on with the pool it found
            drainPool(server, drain);
        }
        if (active > 0) {
            active--;
        }
        BlockPos top = worldPosition.above();
        if (isWater(server.getFluidState(top)) && MachineGuard.mayChange(server, top, owner)) {
            server.setBlock(top, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("active", active);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        active = tag.getInt("active");
    }
}
