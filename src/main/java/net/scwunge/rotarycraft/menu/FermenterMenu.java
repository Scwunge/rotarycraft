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
import net.scwunge.rotarycraft.blockentity.FermenterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Fermenter screen, slots from the original: two inputs stacked on the left, output on the right. */
public class FermenterMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 8;

    private final ContainerData data;
    private final BlockPos pos;

    public FermenterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private FermenterMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof FermenterBlockEntity f ? f.items() : new ItemStackHandler(3), pos, data);
    }

    public FermenterMenu(int id, Inventory inventory, FermenterBlockEntity fermenter, ContainerData data) {
        this(id, inventory, fermenter.items(), fermenter.getBlockPos(), data);
    }

    private FermenterMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.FERMENTER.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, FermenterBlockEntity.SLOT_A, 55, 17));
        addSlot(new SlotItemHandler(items, FermenterBlockEntity.SLOT_B, 55, 53));
        addSlot(new SlotItemHandler(items, FermenterBlockEntity.SLOT_OUT, 116, 35) {
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

    public int cookTime() {
        return data.get(0);
    }

    public int operationTime() {
        return Math.max(1, data.get(1));
    }

    public int temperature() {
        return (short) data.get(2);
    }

    public int water() {
        return data.get(3) & 0xFFFF;
    }

    public int omega() {
        return (data.get(4) & 0xFFFF) | (data.get(5) << 16);
    }

    public int torque() {
        return (data.get(6) & 0xFFFF) | (data.get(7) << 16);
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
        if (index < 3) {
            if (!moveItemStackTo(stack, 3, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
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
