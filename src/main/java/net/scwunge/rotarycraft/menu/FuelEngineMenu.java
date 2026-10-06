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
    /** The engine, on the server only (button clicks act on it). */
    private final FuelEngineBlockEntity engine;

    public FuelEngineMenu(MenuType<?> type, int slots, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(type, id, inventory, buf.readBlockPos(), slots, new SimpleContainerData(FuelEngineBlockEntity.DATA_COUNT));
    }

    private FuelEngineMenu(MenuType<?> type, int id, Inventory inventory, BlockPos pos, int slots, ContainerData data) {
        this(type, id, inventory, inventory.player.level().getBlockEntity(pos) instanceof FuelEngineBlockEntity e ? e.items() : new ItemStackHandler(slots), pos, data, null);
    }

    public FuelEngineMenu(MenuType<?> type, int id, Inventory inventory, FuelEngineBlockEntity engine, ContainerData data) {
        this(type, id, inventory, engine.items(), engine.getBlockPos(), data, engine);
    }

    private FuelEngineMenu(MenuType<?> type, int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data, FuelEngineBlockEntity engine) {
        super(type, id);
        this.data = data;
        this.pos = pos;
        this.engine = engine;
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

    /** Jet Engine status word: afterburner fitted (bit 0), burner on (bit 1), FOD (bits 2+). */
    public boolean canAfterburn() {
        return (data.get(8) & 1) != 0;
    }

    public boolean burnerActive() {
        return (data.get(8) & 2) != 0;
    }

    public int fod() {
        return (data.get(8) & 0xFFFF) >> 2;
    }

    /** Button 0: the Jet Engine's afterburner switch. */
    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 && engine instanceof net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity jet && jet.canAfterburn()) {
            jet.setBurnerActive(!jet.burnerActive());
            return true;
        }
        return false;
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
