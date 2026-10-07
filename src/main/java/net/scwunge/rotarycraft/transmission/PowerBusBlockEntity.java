package net.scwunge.rotarycraft.transmission;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.ClutchBlockEntity;
import net.scwunge.rotarycraft.menu.PowerBusMenu;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A Power Bus block, as the original: part of a network that starts at a Bus Controller. A gear unit in each of its four sides (set in its
 * screen, or by right-clicking the side with one) makes that side an output: the network's input power is shared out evenly over every output
 * side, then each side's gear unit changes it by its ratio, either trading speed for torque (torque mode) or the other way (speed mode). A
 * side that asks more of its gear unit than the unit's material can take breaks it.
 */
public class PowerBusBlockEntity extends BlockEntity implements IShaftPowerOutput, MenuProvider {
    /** The four sides that have a gear unit slot, in the original's order. */
    public static final Direction[] SIDES = DistributionClutchBlockEntity.SIDES;

    private final ItemStackHandler items = new ItemStackHandler(4) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            BusControllerBlockEntity hub = controller();
            if (hub != null) {
                hub.markDirty();
            }
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return GearUnit.of(stack) != null && canHaveItemInSlot(SIDES[slot]);
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private final boolean[] speedMode = new boolean[4];
    private final int[] torqueOut = new int[4];
    private final int[] omegaOut = new int[4];
    @Nullable
    private Direction inputSide;
    @Nullable
    private BlockPos hub;

    public PowerBusBlockEntity(BlockPos pos, BlockState state) {
        super(TransmissionRegistry.POWER_BUS_BE.get(), pos, state);
    }

    public ItemStackHandler items() {
        return items;
    }

    /** What automation sees: gear units can be taken out but not put in (the original refused both). */
    public IItemHandler automation() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 4;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return 1;
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    // ---- the network ----

    /** Joins the network of {@code controller}, fed from {@code toward} (the side facing the block it gets its power from). */
    void attach(BusControllerBlockEntity controller, Direction toward) {
        hub = controller.getBlockPos();
        inputSide = toward;
    }

    void detach() {
        hub = null;
        inputSide = null;
        java.util.Arrays.fill(torqueOut, 0);
        java.util.Arrays.fill(omegaOut, 0);
    }

    @Nullable
    public BusControllerBlockEntity controller() {
        return hub != null && level != null && level.isLoaded(hub) && level.getBlockEntity(hub) instanceof BusControllerBlockEntity c ? c : null;
    }

    public boolean isOnBus() {
        return controller() != null;
    }

    public Direction inputSide() {
        return inputSide;
    }

    /** True unless the side faces another bus block or a controller, which would take power from the bus itself. */
    public boolean canHaveItemInSlot(Direction side) {
        if (level == null) {
            return true;
        }
        BlockEntity neighbour = level.getBlockEntity(worldPosition.relative(side));
        return !(neighbour instanceof PowerBusBlockEntity) && !(neighbour instanceof BusControllerBlockEntity);
    }

    public int ratio(Direction side) {
        int i = DistributionClutchBlockEntity.indexOf(side);
        return i < 0 ? 0 : GearUnit.ratioOf(items.getStackInSlot(i));
    }

    public boolean isSideSpeedMode(Direction side) {
        int i = DistributionClutchBlockEntity.indexOf(side);
        return i >= 0 && speedMode[i];
    }

    public void setSideSpeedMode(Direction side, boolean speed) {
        int i = DistributionClutchBlockEntity.indexOf(side);
        if (i >= 0) {
            speedMode[i] = speed;
            setChanged();
        }
    }

    /** A side counts as an output if power is not coming in through it, it has a gear unit, and what is beside it is not a clutch that is off. */
    public boolean canOutputToSide(Direction side) {
        if (side == inputSide || ratio(side) == 0 || !canHaveItemInSlot(side)) {
            return false;
        }
        return !(level != null && level.getBlockEntity(worldPosition.relative(side)) instanceof ClutchBlockEntity clutch && !clutch.isEngaged());
    }

    public int outputSideCount() {
        int count = 0;
        for (Direction side : SIDES) {
            if (canOutputToSide(side)) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTorqueOut(Direction side) {
        int i = DistributionClutchBlockEntity.indexOf(side);
        return i < 0 ? 0 : torqueOut[i];
    }

    @Override
    public int getOmegaOut(Direction side) {
        int i = DistributionClutchBlockEntity.indexOf(side);
        return i < 0 ? 0 : omegaOut[i];
    }

    /** What a side gives with {@code torque} and {@code omega} coming in for its own share (already divided among the sides). */
    private void work(BusControllerBlockEntity controller) {
        int sides = controller.sides();
        for (int i = 0; i < 4; i++) {
            Direction side = SIDES[i];
            torqueOut[i] = 0;
            omegaOut[i] = 0;
            GearUnit unit = GearUnit.of(items.getStackInSlot(i));
            if (unit == null || !canOutputToSide(side)) {
                continue;
            }
            int base = controller.getTorque() / sides;
            int speed = controller.getOmega();
            int ratio = unit.ratio();
            int torque = speedMode[i] ? base / ratio : base * ratio;
            int omega = speedMode[i] ? speed * ratio : speed / ratio;
            if (unit.material().fails(torque, omega)) {
                items.setStackInSlot(i, ItemStack.EMPTY);
                level.playSound(null, worldPosition, SoundEvents.ITEM_BREAK, SoundSource.BLOCKS, 2, 1);
                controller.markDirty();
                continue;
            }
            if (torque > 0 && omega > 0) {
                torqueOut[i] = torque;
                omegaOut[i] = omega;
            }
        }
    }

    public void serverTick() {
        if (hub == null) {
            return;
        }
        BusControllerBlockEntity controller = controller();
        if (controller == null) {
            if (level.isLoaded(hub)) {
                detach();
            }
            return;
        }
        work(controller);
    }

    /** The power coming through to the bus now, for the screen to show. */
    public long inputPower() {
        BusControllerBlockEntity controller = controller();
        return controller == null ? 0 : controller.getPower();
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int i = 0; i < 4; i++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), items.getStackInSlot(i));
        }
    }

    // ---- screen ----

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new PowerBusMenu(id, inventory, this);
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    /** The speed-mode switches and which sides can hold a gear unit, as bits of one number each, for the screen. */
    public int modeBits() {
        int bits = 0;
        for (int i = 0; i < 4; i++) {
            if (speedMode[i]) {
                bits |= 1 << i;
            }
        }
        return bits;
    }

    public int slotBits() {
        int bits = 0;
        for (int i = 0; i < 4; i++) {
            if (canHaveItemInSlot(SIDES[i])) {
                bits |= 1 << i;
            }
        }
        return bits;
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("modes", modeBits());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        int bits = tag.getInt("modes");
        for (int i = 0; i < 4; i++) {
            speedMode[i] = (bits & 1 << i) != 0;
        }
    }
}
