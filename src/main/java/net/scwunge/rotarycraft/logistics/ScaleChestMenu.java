package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.Nullable;

/**
 * The Scale-able Chest's screen: six rows of the page being shown, taken from the chest's 972 slots, and the player's inventory. The slots past what its power
 * gives are shut. The numbers it keeps in step are the slot count, the power changes (while it is being switched on and off it will not open) and the page.
 */
public class ScaleChestMenu extends AbstractContainerMenu {
    public static final int CHEST_SLOTS = ScaleChestBlockEntity.PAGE_SIZE;

    private final ItemStackHandler items;
    @Nullable
    private final ScaleChestBlockEntity chest;
    private final BlockPos pos;
    private final ContainerData data;
    private final DataSlot page = DataSlot.standalone();

    public ScaleChestMenu(MenuType<?> type, int id, Inventory inventory, ItemStackHandler items, @Nullable ScaleChestBlockEntity chest, BlockPos pos, ContainerData data) {
        super(type, id);
        this.items = items;
        this.chest = chest;
        this.pos = pos;
        this.data = data;
        for (int row = 0; row < ScaleChestBlockEntity.ROWS; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PageSlot(col + row * 9, 8 + col * 18, 18 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 198));
        }
        addDataSlots(data);
        addDataSlot(page);
        if (chest != null) {
            chest.opened();
        }
    }

    public static ContainerData dataFor(ScaleChestBlockEntity chest) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> chest.numberSlots();
                    case 1 -> chest.powerChanges();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    /** The client's menu: its slots fill from what the server sends. */
    public static ScaleChestMenu client(MenuType<?> type, int id, Inventory inventory, BlockPos pos) {
        return new ScaleChestMenu(type, id, inventory, new ItemStackHandler(ScaleChestBlockEntity.MAX_SIZE), null, pos, new SimpleContainerData(2));
    }

    public BlockPos pos() {
        return pos;
    }

    /** How many of the chest's slots are open. */
    public int openSlots() {
        return data.get(0);
    }

    public int powerChanges() {
        return data.get(1);
    }

    public int page() {
        return page.get();
    }

    public int pages() {
        return Math.max(1, Math.min(ScaleChestBlockEntity.MAX_SIZE / CHEST_SLOTS, (openSlots() + CHEST_SLOTS - 1) / CHEST_SLOTS));
    }

    public void setPage(int value) {
        page.set(Math.max(0, Math.min(pages() - 1, value)));
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id == 0 || id == 1) {
            setPage(page() + (id == 0 ? -1 : 1));
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        if (page() >= pages()) {
            setPage(pages() - 1);
        }
        super.broadcastChanges();
    }

    @Override
    public boolean stillValid(Player player) {
        if (chest == null) {
            return true;
        }
        return !chest.isRemoved() && chest.canOpen() && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (chest != null) {
            chest.closed();
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        if (index < CHEST_SLOTS) {
            if (!moveItemStackTo(stack, CHEST_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, CHEST_SLOTS, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    /** A place on the page: whichever of the chest's slots the page being shown puts there. */
    private class PageSlot extends Slot {
        private static final Container NONE = new SimpleContainer(0);
        private final int place;

        PageSlot(int place, int x, int y) {
            super(NONE, place, x, y);
            this.place = place;
        }

        private int target() {
            return page.get() * CHEST_SLOTS + place;
        }

        @Override
        public boolean isActive() {
            return target() < openSlots();
        }

        @Override
        public ItemStack getItem() {
            return isActive() ? items.getStackInSlot(target()) : ItemStack.EMPTY;
        }

        @Override
        public boolean hasItem() {
            return !getItem().isEmpty();
        }

        @Override
        public void set(ItemStack stack) {
            if (isActive()) {
                items.setStackInSlot(target(), stack);
            }
            setChanged();
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return isActive() && (chest == null || items.isItemValid(target(), stack));
        }

        @Override
        public ItemStack remove(int amount) {
            return isActive() ? items.extractItem(target(), amount, false) : ItemStack.EMPTY;
        }

        @Override
        public int getMaxStackSize() {
            return 64;
        }
    }
}
