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
import net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Refrigerator screen, slots from the original: ice in the middle, dry ice below right; the inventory sits 22 lower. */
public class RefrigeratorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 7;

    private final ContainerData data;
    private final BlockPos pos;

    public RefrigeratorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private RefrigeratorMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof RefrigeratorBlockEntity r ? r.items()
                : new ItemStackHandler(RefrigeratorBlockEntity.SLOTS), pos, data);
    }

    public RefrigeratorMenu(int id, Inventory inventory, RefrigeratorBlockEntity fridge, ContainerData data) {
        this(id, inventory, fridge.items(), fridge.getBlockPos(), data);
    }

    private RefrigeratorMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.REFRIGERATOR.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, RefrigeratorBlockEntity.SLOT_ICE, 99, 58));
        addSlot(new SlotItemHandler(items, RefrigeratorBlockEntity.SLOT_DRY_ICE, 132, 72) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 106 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 164));
        }
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int operationTime() {
        return Math.max(1, data.get(1));
    }

    public int fluidAmount() {
        return data.get(2) & 0xFFFF;
    }

    public int omega() {
        return (data.get(3) & 0xFFFF) | (data.get(4) << 16);
    }

    public int torque() {
        return (data.get(5) & 0xFFFF) | (data.get(6) << 16);
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
        if (index < RefrigeratorBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, RefrigeratorBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
