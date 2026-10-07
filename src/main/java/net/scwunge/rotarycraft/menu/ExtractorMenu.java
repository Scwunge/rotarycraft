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
import net.scwunge.rotarycraft.blockentity.ExtractorBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Extractor screen: four stage inputs on top, their outputs below, the bonus slot, and the player's inventory. */
public class ExtractorMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 14;
    private static final int MACHINE_SLOTS = 10;
    /** Slot positions from the original GUI: inputs 0-3 on top, outputs 4-7 below, bonus at the right. */
    private static final int[][] POSITIONS = {
            {26, 13}, {62, 13}, {98, 13}, {134, 13},
            {26, 55}, {62, 55}, {98, 55}, {134, 55},
            {152, 55}, {8, 34}};

    private final ContainerData data;
    private final BlockPos pos;

    public ExtractorMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private ExtractorMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof ExtractorBlockEntity e ? e.items() : new ItemStackHandler(MACHINE_SLOTS), pos, data);
    }

    public ExtractorMenu(int id, Inventory inventory, ExtractorBlockEntity extractor, ContainerData data) {
        this(id, inventory, extractor.items(), extractor.getBlockPos(), data);
    }

    private ExtractorMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.EXTRACTOR.get(), id);
        this.data = data;
        this.pos = pos;
        for (int slot = 0; slot < MACHINE_SLOTS; slot++) {
            boolean output = slot >= ExtractorBlockEntity.STAGES && slot != ExtractorBlockEntity.SLOT_DRILL;
            addSlot(new SlotItemHandler(items, slot, POSITIONS[slot][0], POSITIONS[slot][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return !output && super.mayPlace(stack);
                }

                /** The drill slot is there only when the extractorWear option is on. */
                @Override
                public boolean isActive() {
                    return getSlotIndex() != ExtractorBlockEntity.SLOT_DRILL || ExtractorBlockEntity.wears();
                }
            });
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

    public int progress(int stage) {
        return data.get(stage);
    }

    public int operationTime(int stage) {
        return Math.max(1, data.get(ExtractorBlockEntity.STAGES + stage));
    }

    public int water() {
        return data.get(8) & 0xFFFF;
    }

    public int torque() {
        return (data.get(9) & 0xFFFF) | (data.get(10) << 16);
    }

    public int omega() {
        return (data.get(11) & 0xFFFF) | (data.get(12) << 16);
    }

    /** The operations left in the drill in the machine, out of {@link ExtractorBlockEntity#DRILL_LIFE}. */
    public int drill() {
        return data.get(13);
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
        } else if (ExtractorBlockEntity.wears() && slots.get(ExtractorBlockEntity.SLOT_DRILL).mayPlace(stack)) {
            if (!moveItemStackTo(stack, ExtractorBlockEntity.SLOT_DRILL, ExtractorBlockEntity.SLOT_DRILL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, ExtractorBlockEntity.STAGES, false)) {
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
