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

/** The Projector's container: the 24 slides in a ring, as the original lays them out, round the middle of the panel. */
public class ProjectorMenu extends AbstractContainerMenu {
    private static final int[][] RING = {{0, 0}, {1, 0}, {1, 1}, {2, 1}, {2, 2}, {3, 2}, {3, 3}, {3, 4}, {2, 4}, {2, 5}, {1, 5}, {1, 6}, {0, 6}, {-1, 6},
            {-1, 5}, {-2, 5}, {-2, 4}, {-3, 4}, {-3, 3}, {-3, 2}, {-2, 2}, {-2, 1}, {-1, 1}, {-1, 0}};
    private final ProjectorBlockEntity projector;

    public ProjectorMenu(int id, Inventory inventory, ProjectorBlockEntity projector) {
        super(SurveyRegistry.PROJECTOR_MENU.get(), id);
        this.projector = projector;
        for (int i = 0; i < ProjectorBlockEntity.SLOTS; i++) {
            addSlot(new SlotItemHandler(projector.items(), i, 80 + 18 * RING[i][0], 9 + 18 * RING[i][1]));
        }
        int dy = 56;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18 + dy));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142 + dy));
        }
    }

    public static ProjectorMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new ProjectorMenu(id, inventory, (ProjectorBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < ProjectorBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, ProjectorBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, ProjectorBlockEntity.SLOTS, false)) {
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
        return !projector.isRemoved() && player.distanceToSqr(projector.getBlockPos().getCenter()) <= 64;
    }
}
