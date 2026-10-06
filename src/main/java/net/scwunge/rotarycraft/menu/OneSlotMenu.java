package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * The original's one-slot machine screen (Magnetizer, AC Engine, ...): one slot in the middle, plus the speed, torque
 * and a status flag word synced for the screen.
 */
public class OneSlotMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 5;
    public static final int FLAG_AC = 1;

    /** A block entity that can show this screen. */
    public interface Host {
        ItemStackHandler items();
    }

    private final ContainerData data;
    private final BlockPos pos;

    public OneSlotMenu(MenuType<?> type, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(type, id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private OneSlotMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(type, id, inventory, inventory.player.level().getBlockEntity(pos) instanceof Host h ? h.items() : new ItemStackHandler(1), pos, data);
    }

    public <T extends BlockEntity & Host> OneSlotMenu(MenuType<?> type, int id, Inventory inventory, T host, ContainerData data) {
        this(type, id, inventory, host.items(), host.getBlockPos(), data);
    }

    private OneSlotMenu(MenuType<?> type, int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(type, id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, 0, 80, 35));
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

    /** Packs speed, torque and flags for {@link #addDataSlots}. */
    public static ContainerData data(java.util.function.IntSupplier omega, java.util.function.IntSupplier torque, java.util.function.IntSupplier flags) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> omega.getAsInt() & 0xFFFF;
                    case 1 -> omega.getAsInt() >>> 16;
                    case 2 -> torque.getAsInt() & 0xFFFF;
                    case 3 -> torque.getAsInt() >>> 16;
                    case 4 -> flags.getAsInt();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    public int omega() {
        return (data.get(0) & 0xFFFF) | (data.get(1) << 16);
    }

    public int torque() {
        return (data.get(2) & 0xFFFF) | (data.get(3) << 16);
    }

    public boolean alternating() {
        return (data.get(4) & FLAG_AC) != 0;
    }

    public ItemStack item() {
        return slots.get(0).getItem();
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
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
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
