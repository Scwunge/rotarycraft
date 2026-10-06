package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity;

/** The TNT Cannon's container: its eleven TNT slots in a row, as the original lays them out, then the player's inventory. */
public class CannonMenu extends AbstractContainerMenu {
    private final TntCannonBlockEntity cannon;

    public CannonMenu(int id, Inventory inventory, TntCannonBlockEntity cannon) {
        super(WeaponRegistry.CANNON_MENU.get(), id);
        this.cannon = cannon;
        ItemStackHandler items = cannon.items();
        for (int i = 0; i < TntCannonBlockEntity.SLOTS; i++) {
            addSlot(new SlotItemHandler(items, i, 8 + i * 18 + 1, 132 + 1));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 26 + col * 18 + 1, 154 + row * 18 + 1));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 26 + col * 18 + 1, 212 + 1));
        }
    }

    public static CannonMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CannonMenu(id, inventory, (TntCannonBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public TntCannonBlockEntity cannon() {
        return cannon;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = TntCannonBlockEntity.SLOTS;
        if (index < own ? !moveItemStackTo(stack, own, slots.size(), true) : !moveItemStackTo(stack, 0, own, false)) {
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
        return !cannon.isRemoved() && player.distanceToSqr(cannon.getBlockPos().getCenter()) <= 64;
    }
}
