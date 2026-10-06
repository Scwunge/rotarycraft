package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

/** A camera's container, laid out as the original: the coil on top, the three dyes that name it below. */
public class RemoteMenu extends AbstractContainerMenu {
    private final RemoteMachineBlockEntity camera;

    public RemoteMenu(int id, Inventory inventory, RemoteMachineBlockEntity camera) {
        super(SurveyRegistry.REMOTE_MENU.get(), id);
        this.camera = camera;
        ItemStackHandler items = camera.items();
        addSlot(new SlotItemHandler(items, 0, 80, 17));
        addSlot(new SlotItemHandler(items, 1, 62, 53));
        addSlot(new SlotItemHandler(items, 2, 80, 53));
        addSlot(new SlotItemHandler(items, 3, 98, 53));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static RemoteMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new RemoteMenu(id, inventory, (RemoteMachineBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();
            if (index < RemoteMachineBlockEntity.SIZE) {
                if (!moveItemStackTo(stack, RemoteMachineBlockEntity.SIZE, slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, RemoteMachineBlockEntity.SIZE, false)) {
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
        return !camera.isRemoved() && player.distanceToSqr(camera.getBlockPos().getCenter()) <= 64;
    }
}
