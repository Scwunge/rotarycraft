package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.MachineEnchantments;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.registry.RotaryItems;

/**
 * The Fertilizer, as the original: it needs 1024 W (from any side), water (6000 mB held) and bone meal or compost in its 18 slots. Each tick
 * it tries 4 x log2(speed) random spots in a sphere (radius 2 x log2(torque), at most 32, and not above its own top) and gives growing
 * plants a growth tick, however dark, a few in a row at very high speed. When it helps a plant it uses 5 mB of water now and then
 * and, one time in 20, uses up a fertilizer item (one time in four, on average). Enchanted books add efficiency (more tries), aqua affinity
 * (less water), fortune (more ticks), unbreaking (less fertilizer) and power (more range).
 * <p>
 * One difference from the original: it only ticks plants and spreading ground cover, never fire, redstone or other blocks that a forced
 * tick would turn into trouble on a server.
 */
public class FertilizerBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final int SLOTS = 18;
    public static final int CAPACITY = 6000;
    public static final int MAX_RANGE = 32;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isFertilizer(stack);
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY, fluid -> fluid.getFluid() == Fluids.WATER) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final MachineEnchantments enchantments = new MachineEnchantments(Enchantments.EFFICIENCY, Enchantments.AQUA_AFFINITY, Enchantments.FORTUNE,
            Enchantments.UNBREAKING, Enchantments.POWER);

    public FertilizerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.FERTILIZER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "fertilizer";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    public int menuRows() {
        return SLOTS / 9;
    }

    @Override
    public MachineEnchantments enchantments() {
        return enchantments;
    }

    public FluidTank tank() {
        return tank;
    }

    public static boolean isFertilizer(ItemStack stack) {
        return stack.is(Items.BONE_MEAL) || stack.is(RotaryItems.COMPOST.get());
    }

    public boolean hasFertilizer() {
        return firstFertilizer() >= 0;
    }

    private int firstFertilizer() {
        if (tank.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < SLOTS; i++) {
            if (isFertilizer(items.getStackInSlot(i))) {
                return i;
            }
        }
        return -1;
    }

    private static int log2(int value) {
        return 31 - Integer.numberOfLeadingZeros(Math.max(1, value));
    }

    public int updatesPerTick() {
        if (getPower() < REQUIREMENT.minPower()) {
            return 0;
        }
        return 4 * log2(omega) + enchantLevel(Enchantments.EFFICIENCY) * 3;
    }

    private int consecutiveUpdates() {
        return omega < 1_048_576 ? 1 : 1 + log2(omega / 1_048_576);
    }

    /** How far out it works. */
    public int range() {
        if (torque <= 0) {
            return 0;
        }
        return Math.min(2 * log2(torque) + enchantLevel(Enchantments.POWER), MAX_RANGE);
    }

    @Override
    protected void machineTick(boolean powered) {
        ServerLevel server = server();
        int n = updatesPerTick();
        for (int i = 0; i < n && hasFertilizer(); i++) {
            tickBlock(server);
        }
        if (server.getGameTime() % 20 == 0) {
            syncNow();
        }
    }

    private void tickBlock(ServerLevel server) {
        int r = range();
        if (r <= 0) {
            return;
        }
        int dx = server.random.nextInt(2 * r + 1) - r;
        int dy = server.random.nextInt(2 * r + 1) - r;
        int dz = server.random.nextInt(2 * r + 1) - r;
        if (dy > 1 || dx * dx + dy * dy + dz * dz > r * r) {
            return;
        }
        BlockPos at = worldPosition.offset(dx, dy, dz);
        if (!server.isLoaded(at)) {
            return;
        }
        BlockState state = server.getBlockState(at);
        boolean plant = Crops.isGrowing(state);
        if (!plant && !state.is(Blocks.GRASS_BLOCK) && !state.is(Blocks.MYCELIUM)) {
            return;
        }
        int ticks = consecutiveUpdates() + enchantLevel(Enchantments.FORTUNE);
        for (int i = 0; i < ticks; i++) {
            Crops.forceTick(server, at);
        }
        server.sendParticles(net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0);
        if (plant && server.random.nextInt(20) == 0) {
            consume(server);
        }
    }

    private void consume(ServerLevel server) {
        tank.drain(Math.max(1, 5 - enchantLevel(Enchantments.AQUA_AFFINITY) / 2), IFluidHandler.FluidAction.EXECUTE);
        if (server.random.nextInt(4 + enchantLevel(Enchantments.UNBREAKING)) == 0) {
            int slot = firstFertilizer();
            if (slot >= 0) {
                items.extractItem(slot, 1, false);
            }
        }
    }

    @Override
    protected int[] status() {
        return new int[] {tank.getFluidAmount(), CAPACITY, range(), updatesPerTick()};
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
