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
import net.scwunge.rotarycraft.crafting.AutoCrafterBlockEntity;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * The Auto-Crafter's container, as the original's: two rows of nine pattern slots, each with the slot its output collects in just below (take
 * only), over the player's inventory. In Request mode a button above each pattern (menu buttons 0 to 17) makes one batch of it; button
 * {@link #MODE} changes the mode.
 */
public class AutoCrafterMenu extends AbstractContainerMenu {
    public static final int MODE = 100;

    @Nullable
    private final AutoCrafterBlockEntity crafter;
    private final ContainerData data;
    private final BlockPos pos;

    public AutoCrafterMenu(int id, Inventory inventory, AutoCrafterBlockEntity crafter) {
        this(id, inventory, crafter, crafter.items(), crafter.data(), crafter.getBlockPos());
    }

    private AutoCrafterMenu(int id, Inventory inventory, @Nullable AutoCrafterBlockEntity crafter, ItemStackHandler items, ContainerData data, BlockPos pos) {
        super(CraftingRegistry.AUTO_CRAFTER_MENU.get(), id);
        this.crafter = crafter;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < AutoCrafterBlockEntity.SIZE; i++) {
            int x = 8 + (i % 9) * 18;
            int y = i < 9 ? 19 : 81;
            addSlot(new SlotItemHandler(items, i, x, y));
            addSlot(new SlotItemHandler(items, i + AutoCrafterBlockEntity.OUTPUT_OFFSET, x, y + 27) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
                }
            });
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
    }

    public static AutoCrafterMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        AutoCrafterBlockEntity be = inventory.player.level().getBlockEntity(pos) instanceof AutoCrafterBlockEntity c ? c : null;
        return new AutoCrafterMenu(id, inventory, be, new ItemStackHandler(AutoCrafterBlockEntity.SLOTS), new SimpleContainerData(AutoCrafterBlockEntity.DATA_COUNT), pos);
    }

    public int torque() {
        return data.get(0);
    }

    public int omega() {
        return data.get(1);
    }

    public AutoCrafterBlockEntity.Mode mode() {
        return AutoCrafterBlockEntity.Mode.values()[Math.floorMod(data.get(2), AutoCrafterBlockEntity.Mode.values().length)];
    }

    /** How much longer pattern {@code slot}'s lamp stays lit. */
    public int flash(int slot) {
        return data.get(3 + slot);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (crafter == null || crafter.isRemoved()) {
            return false;
        }
        if (id == MODE) {
            crafter.setMode(crafter.mode().next());
            return true;
        }
        if (id >= 0 && id < AutoCrafterBlockEntity.SIZE && crafter.mode() == AutoCrafterBlockEntity.Mode.REQUEST) {
            crafter.request(id);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = AutoCrafterBlockEntity.SIZE * 2;
        boolean moved;
        if (index < own) {
            moved = moveItemStackTo(stack, own, slots.size(), true);
        } else {
            moved = false;
            for (int i = 0; i < own && !moved; i += 2) {
                moved = moveItemStackTo(stack, i, i + 1, false);
            }
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
        return crafter == null || !crafter.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
