package net.scwunge.rotarycraft.handheld;

import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.HandheldRegistry;

/** The match filter's screen: one slot, whose item the filter in the player's hand keeps. */
public class MatchFilterMenu extends AbstractContainerMenu {
    private final Player player;
    private final InteractionHand hand;
    private final Container slot = new SimpleContainer(1) {
        @Override
        public void setChanged() {
            super.setChanged();
            store();
        }
    };

    public MatchFilterMenu(int id, Inventory inventory, InteractionHand hand) {
        super(HandheldRegistry.MATCH_FILTER_MENU.get(), id);
        this.player = inventory.player;
        this.hand = hand;
        ItemStack filter = inventory.player.getItemInHand(hand);
        if (filter.getItem() instanceof MatchFilterItem) {
            slot.setItem(0, MatchFilterItem.template(filter));
        }
        addSlot(new Slot(slot, 0, 80, 35) {
            @Override
            public int getMaxStackSize() {
                return 1;
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
    }

    private void store() {
        ItemStack filter = player.getItemInHand(hand);
        if (!player.level().isClientSide() && filter.getItem() instanceof MatchFilterItem) {
            MatchFilterItem.setTemplate(filter, slot.getItem(0));
        }
    }

    public InteractionHand hand() {
        return hand;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot clicked = slots.get(index);
        if (!clicked.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = clicked.getItem();
        ItemStack copy = stack.copy();
        if (index == 0 ? !moveItemStackTo(stack, 1, slots.size(), true) : !moveItemStackTo(stack, 0, 1, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            clicked.setByPlayer(ItemStack.EMPTY);
        } else {
            clicked.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return player.getItemInHand(hand).getItem() instanceof MatchFilterItem;
    }
}
