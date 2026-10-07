package net.scwunge.rotarycraft.farm;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;

/** Views of a machine's slots for pipes and hoppers. */
public final class Handlers {
    private Handlers() {}

    /** Things can be put in, and nothing comes out. */
    public static IItemHandler insertOnly(IItemHandler h) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return h.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return h.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return h.insertItem(slot, stack, simulate);
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public int getSlotLimit(int slot) {
                return h.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return h.isItemValid(slot, stack);
            }
        };
    }

    /** Things can be taken out, and nothing goes in. */
    public static IItemHandler extractOnly(IItemHandler h) {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return h.getSlots();
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return h.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return h.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return h.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }
}
