package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.blockentity.WeatherControllerBlockEntity;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;

/** The Weather Controller's container: the original's two rows of nine slots, laid out like a small chest. */
public class WeatherMenu extends AbstractContainerMenu {
    public static final int ROWS = 2;
    private final WeatherControllerBlockEntity controller;

    public WeatherMenu(int id, Inventory inventory, WeatherControllerBlockEntity controller) {
        super(WorldMachineRegistry.WEATHER_MENU.get(), id);
        this.controller = controller;
        ItemStackHandler items = controller.items();
        for (int row = 0; row < ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new SlotItemHandler(items, col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        int top = 31 + ROWS * 18;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
        }
    }

    public static WeatherMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new WeatherMenu(id, inventory, (WeatherControllerBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = WeatherControllerBlockEntity.SLOTS;
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
        return !controller.isRemoved() && player.distanceToSqr(controller.getBlockPos().getCenter()) <= 64;
    }
}
