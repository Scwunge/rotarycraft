package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The menu of any machine described by a {@link GuiLayout}: its slots, then the player's inventory, and the numbers the screen draws. */
public class LayoutMenu extends AbstractContainerMenu {
    private final GuiLayout layout;
    private final ContainerData data;
    private final BlockPos pos;

    public LayoutMenu(MenuType<?> type, GuiLayout layout, int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(type, id);
        this.layout = layout;
        this.pos = pos;
        this.data = data;
        for (int i = 0; i < layout.slots().size(); i++) {
            GuiLayout.SlotPos p = layout.slots().get(i);
            addSlot(new SlotItemHandler(items, i, p.x(), p.y()));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, layout.inventoryX() + col * 18, layout.inventoryY() + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, layout.inventoryX() + col * 18, layout.inventoryY() + 58));
        }
        addDataSlots(data);
    }

    public GuiLayout layout() {
        return layout;
    }

    public BlockPos pos() {
        return pos;
    }

    public int omega() {
        return (data.get(0) & 0xFFFF) | (data.get(1) << 16);
    }

    public int torque() {
        return (data.get(2) & 0xFFFF) | (data.get(3) << 16);
    }

    public Fluid fluid(int tank) {
        return BuiltInRegistries.FLUID.byId(data.get(4 + 3 * tank) & 0xFFFF);
    }

    public int fluidAmount(int tank) {
        return (data.get(5 + 3 * tank) & 0xFFFF) | (data.get(6 + 3 * tank) << 16);
    }

    public int extra(int index) {
        int base = 4 + 3 * layout.tankCount() + 2 * index;
        return (data.get(base) & 0xFFFF) | (data.get(base + 1) << 16);
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
        int machineSlots = layout.slots().size();
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
