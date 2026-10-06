package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.scwunge.rotarycraft.block.PipeBlock;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;

/**
 * A pipe holds one fluid at some amount (no fixed capacity). Every tick, as in the original, it evens out with connected
 * pipes a quarter of the difference at a time (keeping 5 mB). It trades with this mod's machines on any side, as each
 * machine allows (engines only take, producers only give); other mods' tanks feed it from above and below and it fills
 * them beside it. Pressure is 101.3 kPa + 24 Pa per mB: past the pipe's limit it bursts and the fluid spills.
 * A fluid hotter than the pipe can take melts the whole run into lava.
 */
public class PipeBlockEntity extends BlockEntity {
    private static final int CAPACITY_LIMIT = 1_000_000_000;
    private static final int KEEP = 5;

    private FluidStack fluid = FluidStack.EMPTY;
    private int amount;

    /** Lets other mods' machines push fluid in (pipes pull from machines themselves). */
    private final IFluidHandler input = new IFluidHandler() {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return contents();
        }

        @Override
        public int getTankCapacity(int tank) {
            return CAPACITY_LIMIT;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return canTake(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (!canTake(resource)) {
                return 0;
            }
            int add = Math.min(resource.getAmount(), CAPACITY_LIMIT - amount);
            if (action.execute() && add > 0) {
                add(resource, add);
            }
            return add;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return FluidStack.EMPTY;
        }
    };

    public PipeBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.PIPE.get(), pos, state);
    }

    public PipeType type() {
        return getBlockState().getBlock() instanceof PipeBlock p ? p.type() : PipeType.PIPE;
    }

    public FluidStack contents() {
        return amount > 0 ? fluid.copyWithAmount(amount) : FluidStack.EMPTY;
    }

    public int amount() {
        return amount;
    }

    public IFluidHandler input() {
        return input;
    }

    public boolean canTake(FluidStack f) {
        return type().carries(f) && (amount == 0 || fluid.isEmpty() || FluidStack.isSameFluidSameComponents(fluid, f));
    }

    private void add(FluidStack f, int n) {
        if (amount == 0 || fluid.isEmpty()) {
            fluid = f.copyWithAmount(1);
        }
        amount += n;
        setChanged();
    }

    private int remove(int n) {
        int r = Math.min(n, amount);
        amount -= r;
        if (amount <= 0) {
            amount = 0;
            fluid = FluidStack.EMPTY;
        }
        setChanged();
        return r;
    }

    private boolean connected(Direction d) {
        return getBlockState().getValue(PipeBlock.PROPERTIES.get(d));
    }

    public void serverTick() {
        if (level == null) {
            return;
        }
        intake();
        dump();
        if (amount > 0) {
            if (PipeType.pressure(amount) > type().maxPressure) {
                burst();
                return;
            }
            int k = fluid.getFluidType().getTemperature(fluid);
            int celsius = k > 750 ? k - 425 : k - 273;
            if (celsius > type().maxTemperature) {
                melt();
            }
        }
    }

    private boolean powered() {
        return level.hasNeighborSignal(worldPosition);
    }

    private void intake() {
        boolean powered = powered();
        for (Direction d : Direction.values()) {
            if (!connected(d)) {
                continue;
            }
            BlockPos p = worldPosition.relative(d);
            if (level.getBlockEntity(p) instanceof PipeBlockEntity other) {
                if (type().connectsTo(other.type()) && type().receivesFromPipe(d, powered) && other.type().emitsToPipe(d.getOpposite(), other.powered())
                        && other.amount > amount && canTake(other.fluid)) {
                    int take = Math.min(CAPACITY_LIMIT - amount, (other.amount - amount) / 4);
                    if (take > 0) {
                        FluidStack f = other.fluid;
                        other.remove(take);
                        add(f, take);
                    }
                }
            } else if (type().drawsFromTank(d, powered, isOwnMachine(p))) {
                // this mod's machines give fluid on whichever sides they choose; other mods' tanks feed pipes from above and below
                IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, p, d.getOpposite());
                if (h == null) {
                    continue;
                }
                int cap = level.getBlockEntity(p) instanceof PumpLimit pl ? pl.maxBackPressure() : Integer.MAX_VALUE;
                if (amount >= cap) {
                    continue;
                }
                FluidStack avail = h.drain(Integer.MAX_VALUE, IFluidHandler.FluidAction.SIMULATE);
                if (avail.isEmpty() || !canTake(avail)) {
                    continue;
                }
                int take = Math.min(CAPACITY_LIMIT - amount, (avail.getAmount() - amount) / 4);
                if (take > 0) {
                    FluidStack got = h.drain(avail.copyWithAmount(take), IFluidHandler.FluidAction.EXECUTE);
                    if (!got.isEmpty()) {
                        add(got, got.getAmount());
                    }
                }
            }
        }
    }

    private void dump() {
        if (amount <= 1 || fluid.isEmpty()) {
            return;
        }
        boolean powered = powered();
        for (Direction d : Direction.values()) {
            if (amount <= 0) {
                return;
            }
            if (!connected(d)) {
                continue;
            }
            BlockPos p = worldPosition.relative(d);
            if (level.getBlockEntity(p) instanceof PipeBlockEntity other) {
                if (type().connectsTo(other.type()) && type().emitsToPipe(d, powered) && other.type().receivesFromPipe(d.getOpposite(), other.powered())
                        && other.canTake(fluid) && amount > other.amount) {
                    int give = Math.min((amount - other.amount) / 4, amount - KEEP);
                    if (give > 0) {
                        FluidStack f = fluid;
                        remove(give);
                        other.add(f, give);
                    }
                }
            } else if (type().fillsTank(d, isOwnMachine(p))) {
                // pipes feed this mod's machines on any side they take fluid, other mods' tanks beside them
                IFluidHandler h = level.getCapability(Capabilities.FluidHandler.BLOCK, p, d.getOpposite());
                if (h == null) {
                    continue;
                }
                int give = Math.min(amount / 4, amount - KEEP);
                if (give > 0) {
                    int took = h.fill(fluid.copyWithAmount(give), IFluidHandler.FluidAction.EXECUTE);
                    if (took > 0) {
                        remove(took);
                    }
                }
            }
        }
    }

    /** This mod's machines decide per side whether they take or give fluid (the original's pipe "flow"). */
    private boolean isOwnMachine(BlockPos p) {
        BlockEntity be = level.getBlockEntity(p);
        return be != null && net.scwunge.rotarycraft.RotaryCraft.MOD_ID.equals(
                net.minecraft.core.registries.BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType()).getNamespace());
    }

    private void burst() {
        BlockState spill = fluid.getFluid().defaultFluidState().createLegacyBlock();
        level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 1, 0.6F);
        level.setBlockAndUpdate(worldPosition, spill.isAir() ? Blocks.AIR.defaultBlockState() : spill);
    }

    /** The whole connected run of this pipe kind turns to lava. */
    private void melt() {
        Set<BlockPos> run = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(worldPosition);
        while (!queue.isEmpty() && run.size() < 4096) {
            BlockPos p = queue.poll();
            if (!run.add(p)) {
                continue;
            }
            for (Direction d : Direction.values()) {
                BlockPos n = p.relative(d);
                if (!run.contains(n) && level.getBlockEntity(n) instanceof PipeBlockEntity other && other.type() == type()) {
                    queue.add(n);
                }
            }
        }
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1, 1);
        for (BlockPos p : run) {
            level.setBlockAndUpdate(p, Blocks.LAVA.defaultBlockState());
        }
    }

    /** Machines that limit how full a pipe above or below them may get (the Pump's back-pressure). */
    public interface PumpLimit {
        int maxBackPressure();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (amount > 0 && !fluid.isEmpty()) {
            tag.put("fluid", fluid.copyWithAmount(1).save(registries));
            tag.putInt("amount", amount);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        fluid = tag.contains("fluid") ? FluidStack.parseOptional(registries, tag.getCompound("fluid")) : FluidStack.EMPTY;
        amount = tag.getInt("amount");
    }
}
