package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The CVT's container, as the original's: its belts in rows of one, two, four, eight and sixteen (the sixteen in two rows of eight), the one
 * belt it needs in the corner, and the player's inventory. The more belts in a row from the first slot, the greater the ratio it can be set to.
 * Button {@link #MODE} changes how the ratio is chosen, {@link #FLIP} swaps speed for torque in manual mode, and {@link #STATE_ON} and
 * {@link #STATE_OFF} step the ratio for each redstone state. The numbers go by their own packets (see TransmissionNetwork).
 */
public class CvtMenu extends AbstractContainerMenu {
    public static final int MODE = 0;
    public static final int FLIP = 1;
    public static final int STATE_ON = 2;
    public static final int STATE_OFF = 3;

    @Nullable
    private final AdvancedGearBlockEntity gear;
    private final BlockPos pos;

    public CvtMenu(int id, Inventory inventory, AdvancedGearBlockEntity gear) {
        this(id, inventory, gear, gear.belts(), gear.getBlockPos());
    }

    private CvtMenu(int id, Inventory inventory, @Nullable AdvancedGearBlockEntity gear, ItemStackHandler belts, BlockPos pos) {
        super(TransmissionRegistry.CVT_MENU.get(), id);
        this.gear = gear;
        this.pos = pos;
        int slot = 0;
        for (int row = 0; row < 5; row++) {
            for (int col = 0; col < (1 << row); col++) {
                int x = 8 + col * 18;
                int y = 11 + row * 26;
                if (slot > 22) {
                    x -= 144;
                    y += 18;
                }
                addSlot(new SlotItemHandler(belts, slot, x, y));
                slot++;
            }
        }
        addSlot(new SlotItemHandler(belts, AdvancedGearBlockEntity.BELT_SLOTS - 1, 184, 7));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 39 + col * 18, 161 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 39 + col * 18, 215));
        }
    }

    public static CvtMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CvtMenu(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof AdvancedGearBlockEntity g ? g : null,
                new ItemStackHandler(AdvancedGearBlockEntity.BELT_SLOTS), pos);
    }

    public BlockPos pos() {
        return pos;
    }

    /** The top ratio the belts in the slots allow (the screen works it out from its own slots, since the client does not hold the machine's). */
    public int maxRatio() {
        int count = 0;
        for (int i = 0; i < AdvancedGearBlockEntity.BELT_SLOTS - 1; i++) {
            if (!getSlot(i).getItem().is(net.scwunge.rotarycraft.registry.RotaryParts.part("belt").get())) {
                break;
            }
            count++;
        }
        return Integer.highestOneBit(count + 1);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (gear == null || gear.isRemoved()) {
            return false;
        }
        switch (id) {
            case MODE -> gear.stepMode();
            case FLIP -> {
                if (gear.mode() != AdvancedGearBlockEntity.CvtMode.MANUAL) {
                    return false;
                }
                gear.flipRatio();
            }
            case STATE_ON -> gear.stepState(true);
            case STATE_OFF -> gear.stepState(false);
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = 32;
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
        return gear == null || !gear.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
