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
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.farm.FarmUi;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * The container of the farming machines that keep items: rows of nine slots like a chest (the original's "basic storage" screen) over the
 * player's inventory. The machine hands the screen a few numbers (torque, speed, and up to six of its own) which the screen writes out.
 */
public class FarmMenu extends AbstractContainerMenu {
    private final FarmBlockEntity machine;
    private final ContainerData data;
    private final FarmUi ui;

    public FarmMenu(int id, Inventory inventory, FarmBlockEntity machine) {
        this(id, inventory, machine, machine.items(), machine.data(), machine.ui());
    }

    private FarmMenu(int id, Inventory inventory, @Nullable FarmBlockEntity machine, ItemStackHandler items, ContainerData data, FarmUi ui) {
        super(FarmRegistry.FARM_MENU.get(), id);
        this.machine = machine;
        this.data = data;
        this.ui = ui;
        for (int i = 0; i < ui.slotCount(); i++) {
            addSlot(newSlot(machine, items, i, ui.slots()[2 * i], ui.slots()[2 * i + 1]));
        }
        int top = ui.inventoryY();
        if (top >= 0) {
            for (int row = 0; row < 3; row++) {
                for (int col = 0; col < 9; col++) {
                    addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, top + row * 18));
                }
            }
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col, 8 + col * 18, top + 58));
            }
        }
        addDataSlots(data);
    }

    public static FarmMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        FarmBlockEntity be = inventory.player.level().getBlockEntity(pos) instanceof FarmBlockEntity m ? m : null;
        FarmUi ui = be == null || be.ui() == null ? FarmUi.storage(1) : be.ui();
        return new FarmMenu(id, inventory, be, new ItemStackHandler(ui.slotCount()), new SimpleContainerData(FarmBlockEntity.DATA_COUNT), ui);
    }

    public FarmUi ui() {
        return ui;
    }

    public int rows() {
        return ui.storageRows();
    }

    private static Slot newSlot(@Nullable FarmBlockEntity machine, ItemStackHandler items, int index, int x, int y) {
        return machine == null ? new SlotItemHandler(items, index, x, y) : machine.slot(items, index, x, y);
    }

    @Nullable
    public FarmBlockEntity machine() {
        return machine;
    }

    /** The machine's key in the farm config, for the screen to choose its words by. */
    public String kind() {
        return machine == null ? "" : machine.kind();
    }

    public int torque() {
        return data.get(0);
    }

    public int omega() {
        return data.get(1);
    }

    /** One of the machine's own numbers, 0 to 5. */
    public int value(int index) {
        return data.get(2 + index);
    }

    /** A pattern slot shows a copy of what is held over it instead of taking it. */
    public static class GhostSlot extends SlotItemHandler {
        public GhostSlot(ItemStackHandler handler, int index, int x, int y) {
            super(handler, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }
    }

    @Override
    public void clicked(int slotId, int button, net.minecraft.world.inventory.ClickType type, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) instanceof GhostSlot ghost) {
            if (type == net.minecraft.world.inventory.ClickType.PICKUP || type == net.minecraft.world.inventory.ClickType.SWAP) {
                ItemStack carried = getCarried();
                ghost.getItemHandler();
                ((ItemStackHandler) ghost.getItemHandler()).setStackInSlot(ghost.getSlotIndex(), carried.isEmpty() ? ItemStack.EMPTY : carried.copyWithCount(1));
            }
            return;
        }
        super.clicked(slotId, button, type, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        return machine != null && machine.menuButton(player, id);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (slot instanceof GhostSlot || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = ui.slotCount();
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
        return machine == null || !machine.isRemoved() && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64;
    }
}
