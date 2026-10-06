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
import net.scwunge.rotarycraft.blockentity.GrinderBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Grinder screen contents: input, output, the player's inventory, and the progress/power values. */
public class GrinderMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 6;
    private static final int MACHINE_SLOTS = 2;

    private final ContainerData data;
    private final BlockPos pos;

    /** Client side: built from the position the server sent. */
    public GrinderMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, clientItems(inventory, buf), new SimpleContainerData(DATA_COUNT));
    }

    private GrinderMenu(int id, Inventory inventory, ClientSide side, ContainerData data) {
        this(id, inventory, side.items, side.pos, data);
    }

    public GrinderMenu(int id, Inventory inventory, GrinderBlockEntity grinder, ContainerData data) {
        this(id, inventory, grinder.items(), grinder.getBlockPos(), data);
    }

    private GrinderMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.GRINDER.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, GrinderBlockEntity.SLOT_INPUT, 76, 35));
        addSlot(new SlotItemHandler(items, GrinderBlockEntity.SLOT_OUTPUT, 136, 35) {
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

    private record ClientSide(ItemStackHandler items, BlockPos pos) {
    }

    private static ClientSide clientItems(Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        if (inventory.player.level().getBlockEntity(pos) instanceof GrinderBlockEntity grinder) {
            return new ClientSide(grinder.items(), pos);
        }
        return new ClientSide(new ItemStackHandler(MACHINE_SLOTS), pos);
    }

    public int progress() {
        return data.get(0);
    }

    public int operationTime() {
        return Math.max(1, data.get(1));
    }

    public int torque() {
        return (data.get(2) & 0xFFFF) | (data.get(3) << 16);
    }

    public int omega() {
        return (data.get(4) & 0xFFFF) | (data.get(5) << 16);
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
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, GrinderBlockEntity.SLOT_INPUT, GrinderBlockEntity.SLOT_INPUT + 1, false)) {
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
