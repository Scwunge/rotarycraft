package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.weapon.AmmoMenu;
import org.jetbrains.annotations.Nullable;

/**
 * A turret with an ammunition store (the original's inventoried cannon). Hoppers and pipes may load it but never take from it.
 */
public abstract class AmmoTurretBlockEntity extends TurretBlockEntity implements MenuProvider {
    protected final ItemStackHandler items;
    private final IItemHandler automation;

    protected AmmoTurretBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, String weapon, int slots) {
        super(type, pos, state, weapon);
        items = new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return isAmmo(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        automation = new IItemHandler() {
            @Override
            public int getSlots() {
                return items.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return items.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return items.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return items.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return items.isItemValid(slot, stack);
            }
        };
    }

    public abstract boolean isAmmo(ItemStack stack);

    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    /** Takes one of the first stack matching {@code item}; false if there is none. */
    protected boolean useOne(net.minecraft.world.item.Item item) {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(item)) {
                items.extractItem(i, 1, false);
                return true;
            }
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new AmmoMenu(id, inventory, items, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
    }
}
