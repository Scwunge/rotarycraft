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
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.blockentity.FuelEngineBlockEntity;

/**
 * Liquid-fuel engine screens. No slots: the turbines (jet fuel through pipes only). One slot: the original ethanol engine layout. Two slots (Performance Engine): fuel and
 * additive slots, with water, temperature and additive gauges synced too.
 */
public class FuelEngineMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final BlockPos pos;
    private final int machineSlots;

    public FuelEngineMenu(MenuType<?> type, int slots, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(type, id, inventory, buf.readBlockPos(), slots, new SimpleContainerData(FuelEngineBlockEntity.DATA_COUNT));
    }

    private FuelEngineMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, int slots, ContainerData data) {
        this(type, id, inventory, inventory.player.level().getBlockEntity(pos) instanceof FuelEngineBlockEntity e ? e.items() : new ItemStackHandler(slots), pos, data);
    }

    public FuelEngineMenu(MenuType<?> type, int id, Inventory inventory, FuelEngineBlockEntity engine, ContainerData data) {
        this(type, id, inventory, engine.items(), engine.getBlockPos(), data);
    }

    private FuelEngineMenu(MenuType<?> type, int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(type, id);
        this.data = data;
        this.pos = pos;
        machineSlots = items.getSlots();
        if (machineSlots == 1) {
            addSlot(new SlotItemHandler(items, 0, 61, 36));
        } else if (machineSlots == 2) {
            addSlot(new SlotItemHandler(items, 0, 58, 36));
            addSlot(new SlotItemHandler(items, 1, 103, 36));
        }
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

    public int fuel() {
        return (data.get(0) & 0xFFFF) | (data.get(1) << 16);
    }

    public int omega() {
        return (data.get(2) & 0xFFFF) | (data.get(3) << 16);
    }

    public int torque() {
        return (data.get(4) & 0xFFFF) | (data.get(5) << 16);
    }

    public int water() {
        return data.get(6) & 0xFFFF;
    }

    public int temperature() {
        return (short) data.get(7);
    }

    public int additives() {
        return data.get(8) & 0xFFFF;
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
        if (index < machineSlots) {
            if (!moveItemStackTo(stack, machineSlots, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, machineSlots, false)) {
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
