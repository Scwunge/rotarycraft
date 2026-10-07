package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.crafting.CraftPattern;
import net.scwunge.rotarycraft.crafting.WorktableBlockEntity;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Worktable's container: the grid for the recipe, the nine output slots (take only), the pattern slot, and the player's inventory, laid out
 * as the original. Clicking the middle output slot while the outputs are empty crafts what the grid makes into your hand; shift-clicking it
 * crafts as many as will go into your inventory.
 */
public class WorktableMenu extends AbstractContainerMenu {
    private static final int PLAYER_START = WorktableBlockEntity.SLOTS;

    @Nullable
    private final WorktableBlockEntity table;
    private final BlockPos pos;
    private ItemStack ghost = ItemStack.EMPTY;

    public WorktableMenu(int id, Inventory inventory, WorktableBlockEntity table) {
        this(id, inventory, table, table.items(), table.getBlockPos());
    }

    private WorktableMenu(int id, Inventory inventory, @Nullable WorktableBlockEntity table, ItemStackHandler items, BlockPos pos) {
        super(CraftingRegistry.WORKTABLE_MENU.get(), id);
        this.table = table;
        this.pos = pos;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(items, row * 3 + col, 26 + col * 18, 17 + row * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new SlotItemHandler(items, WorktableBlockEntity.FIRST_OUTPUT + row * 3 + col, 98 + col * 18, 17 + row * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
        addSlot(new SlotItemHandler(items, WorktableBlockEntity.PATTERN, 6, 53));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
    }

    public static WorktableMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        WorktableBlockEntity be = inventory.player.level().getBlockEntity(pos) instanceof WorktableBlockEntity t ? t : null;
        return new WorktableMenu(id, inventory, be, new ItemStackHandler(WorktableBlockEntity.SLOTS), pos);
    }

    /** What the grid would make, worked out from the slots here (the screen draws it faintly in the output until you take it). */
    public ItemStack ghost() {
        return ghost;
    }

    /** Works the ghost out again; the screen asks once a tick. */
    public void refreshGhost(net.minecraft.world.level.Level level) {
        List<ItemStack> grid = new ArrayList<>(WorktableBlockEntity.MATRIX);
        for (int i = 0; i < WorktableBlockEntity.MATRIX; i++) {
            grid.add(slots.get(i).getItem());
        }
        ghost = WorktableBlockEntity.preview(level, grid);
    }

    /** True if the grid has a recipe and the outputs are clear, so the screen shows the arrow lit. */
    public boolean ready() {
        if (ghost.isEmpty()) {
            return false;
        }
        for (int i = WorktableBlockEntity.FIRST_OUTPUT; i < WorktableBlockEntity.PATTERN; i++) {
            if (slots.get(i).hasItem()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player player) {
        if (slotId == WorktableBlockEntity.MAIN_OUTPUT && table != null && !table.isRemoved() && !player.level().isClientSide()
                && !slots.get(slotId).hasItem() && table.outputsEmpty()) {
            if (type == ClickType.PICKUP && getCarried().isEmpty()) {
                table.craft(player);
            } else if (type == ClickType.QUICK_MOVE) {
                craftIntoInventory(player);
                return;
            }
        }
        super.clicked(slotId, button, type, player);
    }

    private void craftIntoInventory(Player player) {
        for (int n = 0; n < 64 && table.craft(player); n++) {
            ItemStack made = table.items().getStackInSlot(WorktableBlockEntity.MAIN_OUTPUT);
            moveItemStackTo(made, PLAYER_START, slots.size(), true);
            table.items().setStackInSlot(WorktableBlockEntity.MAIN_OUTPUT, made.isEmpty() ? ItemStack.EMPTY : made);
            if (!made.isEmpty()) {
                break;
            }
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
        boolean moved;
        if (index < PLAYER_START) {
            moved = moveItemStackTo(stack, PLAYER_START, slots.size(), true);
        } else if (CraftPattern.isPattern(stack) && moveItemStackTo(stack, WorktableBlockEntity.PATTERN, WorktableBlockEntity.PATTERN + 1, false)) {
            moved = true;
        } else {
            moved = moveItemStackTo(stack, 0, WorktableBlockEntity.MATRIX, false);
        }
        if (!moved) {
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
        return table == null || !table.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
