package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;

/**
 * The Ground Hydrator, as the original: no shaft, only water (1000 mB held, from pipes). Every other tick, while it holds 25 mB, it picks
 * a spot in the 13 by 13 patch round it (nearer spots likelier, by the original's table) and if that is dry farmland it wets it, paying 25 mB.
 * It looks at its own level and the layer below.
 */
public class GroundHydratorBlockEntity extends FarmBlockEntity {
    public static final int FLUID_PER_BLOCK = 25;
    public static final int CAPACITY = 1000;
    private static final int[][] AREA = {
            {1, 1, 1, 1, 2, 2, 3, 2, 2, 1, 1, 1, 1},
            {1, 1, 1, 2, 2, 3, 4, 3, 2, 2, 1, 1, 1},
            {1, 1, 2, 3, 5, 6, 6, 6, 5, 3, 2, 1, 1},
            {1, 2, 3, 4, 6, 7, 7, 7, 6, 4, 3, 2, 1},
            {2, 2, 4, 6, 7, 8, 8, 8, 7, 6, 4, 2, 2},
            {2, 3, 6, 7, 8, 9, 9, 9, 8, 7, 6, 3, 2},
            {3, 4, 6, 7, 8, 9, 0, 9, 8, 7, 6, 4, 3},
            {2, 3, 6, 7, 8, 9, 9, 9, 8, 7, 6, 3, 2},
            {2, 2, 4, 6, 7, 8, 8, 8, 7, 6, 4, 2, 2},
            {1, 2, 3, 5, 6, 7, 7, 7, 6, 5, 3, 2, 1},
            {1, 1, 2, 3, 4, 6, 6, 6, 4, 3, 2, 1, 1},
            {1, 1, 1, 2, 2, 3, 4, 3, 2, 2, 1, 1, 1},
            {1, 1, 1, 1, 2, 2, 3, 2, 2, 1, 1, 1, 1},
    };
    private static final int TOTAL;

    static {
        int total = 0;
        for (int[] row : AREA) {
            for (int w : row) {
                total += w;
            }
        }
        TOTAL = total;
    }

    private final FluidTank tank = new FluidTank(CAPACITY, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    public GroundHydratorBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.GROUND_HYDRATOR_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "groundHydrator";
    }

    @Override
    public PowerRequirement requirement() {
        return new PowerRequirement(0, 0, 0);
    }

    @Override
    protected boolean usesShaftPower() {
        return false;
    }

    public static int range() {
        return (AREA.length - 1) / 2;
    }

    public FluidTank tank() {
        return tank;
    }

    /** A spot of the patch, picked by the original's weights: offsets from the hydrator. */
    public static int[] pick(RandomSource random) {
        int roll = random.nextInt(TOTAL);
        for (int z = 0; z < AREA.length; z++) {
            for (int x = 0; x < AREA[z].length; x++) {
                roll -= AREA[z][x];
                if (roll < 0) {
                    return new int[] {x - range(), z - range()};
                }
            }
        }
        return new int[] {0, 0};
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        if (tank.getFluidAmount() >= FLUID_PER_BLOCK && server.random.nextInt(2) == 0) {
            int[] offset = pick(server.random);
            BlockPos at = worldPosition.offset(offset[0], 0, offset[1]);
            if (server.isLoaded(at)) {
                boolean did = false;
                if (server.getBlockState(at).is(Blocks.FARMLAND)) {
                    did = Crops.hydrateFarmland(server, at);
                } else if (server.getBlockState(at.below()).is(Blocks.FARMLAND)) {
                    did = Crops.hydrateFarmland(server, at.below());
                }
                if (did) {
                    tank.drain(FLUID_PER_BLOCK, IFluidHandler.FluidAction.EXECUTE);
                }
            }
        }
        if (server.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        tank.readFromNBT(registries, tag.getCompound("tank"));
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("water", tank.getFluidAmount());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tank.setFluid(tag.getInt("water") > 0 ? new FluidStack(Fluids.WATER, tag.getInt("water")) : FluidStack.EMPTY);
    }
}
