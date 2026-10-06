package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** The screen of a machine with a single slot for its coil in the middle, as the original's one-slot machines. */
public class CoilMenu extends AbstractContainerMenu {
    private final BlockEntity entity;

    public CoilMenu(MenuType<?> type, int id, Inventory inventory, SurveyBlockEntity entity) {
        this(type, id, inventory, entity, entity.items());
    }

    private CoilMenu(MenuType<?> type, int id, Inventory inventory, BlockEntity entity, ItemStackHandler items) {
        super(type, id);
        this.entity = entity;
        addSlot(new SlotItemHandler(items, 0, 80, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static CoilMenu fromNetwork(MenuType<?> type, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CoilMenu(type, id, inventory, (SurveyBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index == 0) {
            if (!moveItemStackTo(stack, 1, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return !entity.isRemoved() && player.distanceToSqr(entity.getBlockPos().getCenter()) <= 64;
    }
}
