package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Blast Furnace screen, slot layout from the original: additives on the left, 3x3 grid, three outputs on the right. */
public class BlastFurnaceMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 3;
    private static final int MACHINE_SLOTS = BlastFurnaceBlockEntity.SLOTS;

    private final ContainerData data;
    private final BlockPos pos;

    public BlastFurnaceMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private BlastFurnaceMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof BlastFurnaceBlockEntity b ? b.items() : new ItemStackHandler(MACHINE_SLOTS), pos, data);
    }

    public BlastFurnaceMenu(int id, Inventory inventory, BlastFurnaceBlockEntity furnace, ContainerData data) {
        this(id, inventory, furnace.items(), furnace.getBlockPos(), data);
    }

    private BlastFurnaceMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.BLAST_FURNACE.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, BlastFurnaceBlockEntity.SLOT_CENTER_ADDITIVE, 26, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(items, 1 + row * 3 + col, 62 + col * 18, 17 + row * 18));
            }
        }
        addSlot(output(items, BlastFurnaceBlockEntity.SLOT_OUTPUT_CENTER, 148, 35));
        addSlot(new SlotItemHandler(items, BlastFurnaceBlockEntity.SLOT_LOWER_ADDITIVE, 26, 54));
        addSlot(output(items, BlastFurnaceBlockEntity.SLOT_OUTPUT_UPPER, 148, 17));
        addSlot(output(items, BlastFurnaceBlockEntity.SLOT_OUTPUT_LOWER, 148, 53));
        addSlot(new SlotItemHandler(items, BlastFurnaceBlockEntity.SLOT_UPPER_ADDITIVE, 26, 16));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        addDataSlots(data);
    }

    private static SlotItemHandler output(ItemStackHandler items, int slot, int x, int y) {
        return new SlotItemHandler(items, slot, x, y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        };
    }

    public int temperature() {
        return (short) data.get(0);
    }

    public int smeltTime() {
        return data.get(1);
    }

    public int operationTime() {
        return Math.max(1, data.get(2));
    }

    @Override
    public boolean stillValid(Player player) {
        return player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 10, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }
}
