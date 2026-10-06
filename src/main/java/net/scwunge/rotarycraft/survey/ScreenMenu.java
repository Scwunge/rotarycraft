package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

/** The CCTV Screen's container: three dye slots, as the original lays them out. */
public class ScreenMenu extends AbstractContainerMenu {
    private final ScreenBlockEntity screen;

    public ScreenMenu(int id, Inventory inventory, ScreenBlockEntity screen) {
        super(SurveyRegistry.SCREEN_MENU.get(), id);
        this.screen = screen;
        addSlot(new SlotItemHandler(screen.items(), 0, 62, 35));
        addSlot(new SlotItemHandler(screen.items(), 1, 80, 35));
        addSlot(new SlotItemHandler(screen.items(), 2, 98, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static ScreenMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new ScreenMenu(id, inventory, (ScreenBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < 3) {
                if (!moveItemStackTo(stack, 3, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, 3, false)) {
                return ItemStack.EMPTY;
            }
            if (stack.isEmpty()) {
                slot.setByPlayer(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return !screen.isRemoved() && player.distanceToSqr(screen.getBlockPos().getCenter()) <= 64;
    }
}
