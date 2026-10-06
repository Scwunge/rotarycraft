package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A machine's storage shown as rows of nine (the original's basic storage container), with the player's inventory below. Slots
 * only take what the machine accepts.
 */
public class AmmoMenu extends AbstractContainerMenu {
    private final IItemHandlerModifiable store;
    @Nullable
    private final BlockEntity machine;
    private final int rows;

    public AmmoMenu(int id, Inventory inventory, IItemHandlerModifiable store, @Nullable BlockEntity machine) {
        super(WeaponRegistry.AMMO_MENU.get(), id);
        this.store = store;
        this.machine = machine;
        this.rows = (store.getSlots() + 8) / 9;
        for (int i = 0; i < store.getSlots(); i++) {
            addSlot(new SlotItemHandler(store, i, 8 + i % 9 * 18, 18 + i / 9 * 18));
        }
        int top = 18 + rows * 18 + 14;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
        }
    }

    public static AmmoMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        BlockEntity be = inventory.player.level().getBlockEntity(pos);
        IItemHandlerModifiable store = be instanceof net.scwunge.rotarycraft.weapon.turret.AmmoTurretBlockEntity t ? t.items() : new ItemStackHandler(9);
        return new AmmoMenu(id, inventory, store, be);
    }

    public int rows() {
        return rows;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int machineSlots = store.getSlots();
        if (index < machineSlots ? !moveItemStackTo(stack, machineSlots, slots.size(), true) : !moveItemStackTo(stack, 0, machineSlots, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return machine == null || !machine.isRemoved() && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64;
    }
}
