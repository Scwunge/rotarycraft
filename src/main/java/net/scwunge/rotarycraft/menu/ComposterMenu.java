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
import net.scwunge.rotarycraft.blockentity.ComposterBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Composter screen, slots from the original: matter above yeast on the left, Compost out on the right. */
public class ComposterMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 2;

    private final ContainerData data;
    private final BlockPos pos;

    public ComposterMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private ComposterMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof ComposterBlockEntity c ? c.items()
                : new ItemStackHandler(ComposterBlockEntity.SLOTS), pos, data);
    }

    public ComposterMenu(int id, Inventory inventory, ComposterBlockEntity composter, ContainerData data) {
        this(id, inventory, composter.items(), composter.getBlockPos(), data);
    }

    private ComposterMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.COMPOSTER.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, ComposterBlockEntity.SLOT_INPUT, 55, 26));
        addSlot(new SlotItemHandler(items, ComposterBlockEntity.SLOT_YEAST, 55, 44));
        addSlot(new SlotItemHandler(items, ComposterBlockEntity.SLOT_OUTPUT, 116, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
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

    public int timer() {
        return data.get(0);
    }

    public int temperature() {
        return (short) data.get(1);
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
        if (index < ComposterBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, ComposterBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 2, false)) {
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
