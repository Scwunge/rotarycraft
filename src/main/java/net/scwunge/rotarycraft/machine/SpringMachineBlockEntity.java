package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.power.PowerRequirement;

/**
 * A machine run by a wound coil in its first slot (TileEntitySpringPowered) rather than by the shaft: while the coil has charge the machine works, and
 * the coil unwinds one unit every base time times its stiffness, in ticks. An empty coil may be taken out; a charged one is held in.
 */
public abstract class SpringMachineBlockEntity extends InventoryMachineBlockEntity {
    /** Springs run on the coil, not the shaft: the shaft requirement can never be met. */
    private static final PowerRequirement NEVER = new PowerRequirement(Integer.MAX_VALUE, Integer.MAX_VALUE, Long.MAX_VALUE);

    private int unwindTicks;

    protected SpringMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, String menuKey) {
        super(type, pos, state, slots, menuKey);
    }

    /** Ticks one unit of charge lasts, for a coil of stiffness 1. */
    protected abstract int baseDischargeTime();

    /** One tick of the machine's work. */
    protected abstract void springTick(boolean hasCoil);

    @Override
    public PowerRequirement requirement() {
        return NEVER;
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return slot == 0 && stack.getItem() instanceof CoilItem;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return slot != 0 || CoilItem.charge(items.getStackInSlot(0)) == 0;
    }

    public boolean hasCoil() {
        ItemStack coil = items.getStackInSlot(0);
        return coil.getItem() instanceof CoilItem && CoilItem.charge(coil) > 0;
    }

    /** The ticks a unit of charge lasts in the coil in the slot. */
    public int unwindTime() {
        ItemStack coil = items.getStackInSlot(0);
        return baseDischargeTime() * (coil.getItem() instanceof CoilItem c ? c.stiffness() : 1);
    }

    /** Lets the coil run down by a tick; call while the machine is working. */
    protected void unwind() {
        if (!hasCoil()) {
            return;
        }
        if (++unwindTicks > unwindTime()) {
            unwindTicks = 0;
            ItemStack coil = items.getStackInSlot(0).copy();
            CoilItem.setCharge(coil, CoilItem.charge(coil) - 1);
            items.setStackInSlot(0, coil);
        }
    }

    @Override
    protected final void machineTick(boolean powered) {
        springTick(hasCoil());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("unwind", unwindTicks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        unwindTicks = tag.getInt("unwind");
    }
}
