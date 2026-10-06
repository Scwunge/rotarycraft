package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.blockentity.RockMelterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Rock Melter screen, slots from the original: a 3x3 input grid, the tank on the right. */
public class RockMelterMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 7;

    private final ContainerData data;
    private final BlockPos pos;

    public RockMelterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private RockMelterMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof RockMelterBlockEntity m ? m.items()
                : new ItemStackHandler(RockMelterBlockEntity.SLOTS), pos, data);
    }

    public RockMelterMenu(int id, Inventory inventory, RockMelterBlockEntity melter, ContainerData data) {
        this(id, inventory, melter.items(), melter.getBlockPos(), data);
    }

    private RockMelterMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.ROCK_MELTER.get(), id);
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < RockMelterBlockEntity.SLOTS; i++) {
            addSlot(new SlotItemHandler(items, i, 26 + 18 * (i % 3), 17 + 18 * (i / 3)));
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

    public int temperature() {
        return (short) data.get(0);
    }

    public int fluidAmount() {
        return data.get(1) & 0xFFFF;
    }

    public Fluid fluid() {
        return BuiltInRegistries.FLUID.byId(data.get(2) & 0xFFFF);
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
        if (index < RockMelterBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, RockMelterBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, RockMelterBlockEntity.SLOTS, false)) {
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
