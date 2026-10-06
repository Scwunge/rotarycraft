package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.menu.RefrigeratorMenu;
import net.scwunge.rotarycraft.pipe.FluidAccess;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.power.RefrigeratorAttachment;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/**
 * Refrigerator, as in the original: needs 2048 N*m (any speed, 32 kW) and turns one ice block a cycle into liquid nitrogen
 * (100 mB x (torque / 2048) squared, at most 2000), sometimes leaving dry ice. A cycle takes 1000 - 80 x log2(speed) ticks.
 * Adjacent {@link RefrigeratorAttachment}s are told of each cycle. Breaking it with a lot of liquid nitrogen in it freezes
 * everything within five blocks.
 */
public class RefrigeratorBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(2048, 1, 32768);
    public static final int CAPACITY = 12000;
    public static final int SLOT_ICE = 0;
    public static final int SLOT_DRY_ICE = 1;
    public static final int SLOTS = 2;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == SLOT_ICE && stack.is(Items.ICE);
        }
    };
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return SLOTS;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == SLOT_ICE ? items.insertItem(slot, stack, simulate) : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == SLOT_DRY_ICE ? items.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 64;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };
    private final FluidTank tank = new FluidTank(CAPACITY, s -> s.getFluid().isSame(RotaryFluids.LIQUID_NITROGEN.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    private int progress;
    private int operationTime = 1000;
    private int soundTicks;

    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> operationTime;
                case 2 -> tank.getFluidAmount();
                case 3 -> omega & 0xFFFF;
                case 4 -> omega >>> 16;
                case 5 -> torque & 0xFFFF;
                case 6 -> torque >>> 16;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
        }

        @Override
        public int getCount() {
            return RefrigeratorMenu.DATA_COUNT;
        }
    };

    public RefrigeratorBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.REFRIGERATOR.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public FluidTank tank() {
        return tank;
    }

    /** Pipes and buckets take the liquid nitrogen out; nothing goes in. */
    public IFluidHandler output() {
        return FluidAccess.drainOnly(tank);
    }

    public int producedNitrogen() {
        int over = torque / REQUIREMENT.minTorque();
        return Math.min(2000, 100 * over * over);
    }

    private boolean canProgress(boolean powered) {
        ItemStack dry = items.getStackInSlot(SLOT_DRY_ICE);
        return powered && items.getStackInSlot(SLOT_ICE).is(Items.ICE) && tank.getSpace() >= producedNitrogen()
                && (dry.isEmpty() || dry.getCount() < dry.getMaxStackSize());
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!canProgress(powered)) {
            progress = 0;
            soundTicks = 0;
            return;
        }
        if (++soundTicks >= 20) {
            soundTicks = 0;
            level.playSound(null, worldPosition, SoundEvents.SNOW_BREAK, SoundSource.BLOCKS, 0.5F, 0.88F);
        }
        operationTime = PowerRequirement.operationTime(1000, 80, omega);
        if (++progress < operationTime) {
            return;
        }
        progress = 0;
        cycle();
    }

    private void cycle() {
        items.extractItem(SLOT_ICE, 1, false);
        int amount = producedNitrogen();
        tank.fill(new FluidStack(RotaryFluids.LIQUID_NITROGEN.get(), amount), IFluidHandler.FluidAction.EXECUTE);
        if (amount <= 0) {
            return;
        }
        for (Direction d : Direction.values()) {
            if (level.getBlockEntity(worldPosition.relative(d)) instanceof RefrigeratorAttachment a) {
                a.onCompleteCycle(amount);
            }
        }
        if (level.random.nextInt(4) == 0) {
            int n = level.random.nextInt(20) == 0 ? 4 : (level.random.nextInt(4) == 0 ? 2 : 1);
            ItemStack out = items.getStackInSlot(SLOT_DRY_ICE);
            n = Math.min(n, RotaryItems.DRY_ICE.get().getDefaultMaxStackSize() - out.getCount());
            if (n > 0) {
                items.setStackInSlot(SLOT_DRY_ICE, new ItemStack(RotaryItems.DRY_ICE.get(), out.getCount() + n));
            }
        }
        setChanged();
    }

    /** A burst of cold when it is broken with a good amount of liquid nitrogen in it. */
    public void onBroken() {
        float fraction = tank.getFluidAmount() / (float) CAPACITY;
        if (fraction <= 0.1F) {
            return;
        }
        level.playSound(null, worldPosition, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 1.2F, 0.8F);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(5))) {
            e.hurt(level.damageSources().freeze(), fraction * 8);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.refrigerator");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RefrigeratorMenu(id, inventory, this, data);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        tank.readFromNBT(registries, tag.getCompound("tank"));
        progress = tag.getInt("progress");
    }
}
