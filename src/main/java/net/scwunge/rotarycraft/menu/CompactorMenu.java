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
import net.scwunge.rotarycraft.blockentity.CompactorBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Compactor screen, slots from the original: four inputs in a column, the output to the right. */
public class CompactorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 9;

    private final ContainerData data;
    private final BlockPos pos;

    public CompactorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private CompactorMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof CompactorBlockEntity c ? c.items()
                : new ItemStackHandler(CompactorBlockEntity.SLOTS), pos, data);
    }

    public CompactorMenu(int id, Inventory inventory, CompactorBlockEntity compactor, ContainerData data) {
        this(id, inventory, compactor.items(), compactor.getBlockPos(), data);
    }

    private CompactorMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.COMPACTOR.get(), id);
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < 4; i++) {
            addSlot(new SlotItemHandler(items, i, 26, 8 + 18 * i));
        }
        addSlot(new SlotItemHandler(items, CompactorBlockEntity.SLOT_OUTPUT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
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

    public int progress() {
        return data.get(0);
    }

    public int operationTime() {
        return Math.max(1, data.get(1));
    }

    public int temperature() {
        return (short) data.get(2);
    }

    public int pressure() {
        return (data.get(3) & 0xFFFF) | (data.get(4) << 16);
    }

    public int torque() {
        return (data.get(5) & 0xFFFF) | (data.get(6) << 16);
    }

    public int omega() {
        return (data.get(7) & 0xFFFF) | (data.get(8) << 16);
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
        if (index < CompactorBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, CompactorBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 4, false)) {
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
