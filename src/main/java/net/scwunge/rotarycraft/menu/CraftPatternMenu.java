package net.scwunge.rotarycraft.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.crafting.CraftPattern;
import net.scwunge.rotarycraft.crafting.PatternMode;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.ArrayList;
import java.util.List;

/**
 * The Craft Pattern's screen: a 3 by 3 grid to lay a recipe out in (clicking a grid slot with something in hand writes one of it there,
 * clicking with nothing clears it; nothing is taken from you), the recipe's result beside it, a button for the kind of recipe, and buttons
 * for the input limit. Every change is written into the pattern in your hand at once.
 */
public class CraftPatternMenu extends AbstractContainerMenu {
    public static final int MODE = 0;
    public static final int LIMIT_UP_1 = 1;
    public static final int LIMIT_DOWN_1 = 2;
    public static final int LIMIT_UP_16 = 3;
    public static final int LIMIT_DOWN_16 = 4;
    public static final int LIMIT_UP_64 = 5;
    public static final int LIMIT_DOWN_64 = 6;

    private final Player player;
    private final InteractionHand hand;
    private final SimpleContainer grid = new SimpleContainer(CraftPattern.GRID);
    private final SimpleContainer result = new SimpleContainer(1);
    private PatternMode mode = PatternMode.CRAFTING;
    private int limit = CraftPattern.NO_LIMIT;

    public CraftPatternMenu(int id, Inventory inventory, InteractionHand hand) {
        super(CraftingRegistry.CRAFT_PATTERN_MENU.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof net.scwunge.rotarycraft.crafting.CraftPatternItem) {
            CraftPattern pattern = CraftPattern.of(held);
            mode = pattern.mode();
            limit = pattern.limit();
            for (int i = 0; i < CraftPattern.GRID; i++) {
                grid.setItem(i, pattern.inputs().get(i).copy());
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                addSlot(new Display(grid, row * 3 + col, 30 + col * 18, 17 + row * 18));
            }
        }
        addSlot(new Display(result, 0, 124, 35));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, 142));
        }
        refreshResult();
    }

    public static CraftPatternMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new CraftPatternMenu(id, inventory, buf.readEnum(InteractionHand.class));
    }

    public PatternMode mode() {
        return mode;
    }

    public int limit() {
        return limit;
    }

    /** A slot that only shows what is in it: the grid and the result are written by clicking, not by moving stacks. */
    private static class Display extends Slot {
        Display(SimpleContainer container, int index, int x, int y) {
            super(container, index, x, y);
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

    private void setGrid(int slot, ItemStack stack) {
        grid.setItem(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
        save();
    }

    @Override
    public void clicked(int slotId, int button, ClickType type, Player clicker) {
        if (slotId >= 0 && slotId < CraftPattern.GRID) {
            if (type == ClickType.PICKUP) {
                setGrid(slotId, getCarried());
            } else if (type == ClickType.SWAP) {
                setGrid(slotId, button == 40 ? clicker.getOffhandItem() : button >= 0 && button < 9 ? clicker.getInventory().getItem(button) : ItemStack.EMPTY);
            } else if (type == ClickType.CLONE || type == ClickType.THROW) {
                setGrid(slotId, ItemStack.EMPTY);
            }
            return;
        }
        if (slotId == CraftPattern.GRID) {
            return;
        }
        super.clicked(slotId, button, type, clicker);
    }

    private void refreshResult() {
        result.setItem(0, ItemStack.EMPTY);
        if (player.level() != null) {
            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < CraftPattern.GRID; i++) {
                inputs.add(grid.getItem(i));
            }
            result.setItem(0, CraftPattern.write(player.level(), mode, inputs, limit).output());
        }
    }

    /** Writes the grid, mode and limit into the pattern in hand. */
    private void save() {
        refreshResult();
        ItemStack held = player.getItemInHand(hand);
        if (held.getItem() instanceof net.scwunge.rotarycraft.crafting.CraftPatternItem) {
            List<ItemStack> inputs = new ArrayList<>();
            for (int i = 0; i < CraftPattern.GRID; i++) {
                inputs.add(grid.getItem(i));
            }
            held.set(RotaryComponents.CRAFT_PATTERN.get(), CraftPattern.write(player.level(), mode, inputs, limit));
        }
    }

    @Override
    public boolean clickMenuButton(Player clicker, int id) {
        if (id == MODE) {
            mode = mode.next();
            grid.clearContent();
            save();
            return true;
        }
        int change = switch (id) {
            case LIMIT_UP_1 -> 1;
            case LIMIT_DOWN_1 -> -1;
            case LIMIT_UP_16 -> 16;
            case LIMIT_DOWN_16 -> -16;
            case LIMIT_UP_64 -> 64;
            case LIMIT_DOWN_64 -> -64;
            default -> 0;
        };
        if (change == 0) {
            return false;
        }
        if (change > 1 && limit == 1) {
            change--;
        }
        limit = Math.max(1, Math.min(CraftPattern.NO_LIMIT, limit + change));
        save();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player clicker, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player clicker) {
        return clicker.getItemInHand(hand).getItem() instanceof net.scwunge.rotarycraft.crafting.CraftPatternItem;
    }
}
