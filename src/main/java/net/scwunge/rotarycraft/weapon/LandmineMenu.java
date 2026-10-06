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
import net.scwunge.rotarycraft.weapon.turret.LandmineBlockEntity;

/** The Landmine's container, laid out as the original: the coil in the middle, gunpowder to its left, modifiers to its right. */
public class LandmineMenu extends AbstractContainerMenu {
    private static final int[][] SLOTS = {{80, 34}, {16, 25}, {34, 25}, {16, 43}, {34, 43}, {126, 25}, {144, 25}, {126, 43}, {144, 43}};
    private final LandmineBlockEntity mine;

    public LandmineMenu(int id, Inventory inventory, LandmineBlockEntity mine) {
        super(WeaponRegistry.LANDMINE_MENU.get(), id);
        this.mine = mine;
        ItemStackHandler items = mine.items();
        for (int i = 0; i < LandmineBlockEntity.SLOTS; i++) {
            addSlot(new SlotItemHandler(items, i, SLOTS[i][0], SLOTS[i][1]));
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static LandmineMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new LandmineMenu(id, inventory, (LandmineBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = LandmineBlockEntity.SLOTS;
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
        return !mine.isRemoved() && player.distanceToSqr(mine.getBlockPos().getCenter()) <= 64;
    }
}
