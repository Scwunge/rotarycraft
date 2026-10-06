package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.blockentity.CentrifugeBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Centrifuge screen, slots from the original: input on the left, a 3x3 output grid, the tank on the right. */
public class CentrifugeMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 8;

    private final ContainerData data;
    private final BlockPos pos;

    public CentrifugeMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private CentrifugeMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof CentrifugeBlockEntity c ? c.items()
                : new ItemStackHandler(CentrifugeBlockEntity.SLOTS), pos, data);
    }

    public CentrifugeMenu(int id, Inventory inventory, CentrifugeBlockEntity centrifuge, ContainerData data) {
        this(id, inventory, centrifuge.items(), centrifuge.getBlockPos(), data);
    }

    private CentrifugeMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.CENTRIFUGE.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, CentrifugeBlockEntity.SLOT_INPUT, 26, 38));
        for (int i = 0; i < 3; i++) {
            for (int k = 0; k < 3; k++) {
                addSlot(new SlotItemHandler(items, 1 + i * 3 + k, 85 + k * 18, 20 + i * 18) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
        }
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

    public int progress() {
        return data.get(0);
    }

    public int operationTime() {
        return Math.max(1, data.get(1));
    }

    public int fluidAmount() {
        return data.get(2) & 0xFFFF;
    }

    public int omega() {
        return (data.get(3) & 0xFFFF) | (data.get(4) << 16);
    }

    public int torque() {
        return (data.get(5) & 0xFFFF) | (data.get(6) << 16);
    }

    /** The fluid in the tank, sent as its registry id so the screen can draw and name it. */
    public Fluid fluid() {
        return BuiltInRegistries.FLUID.byId(data.get(7) & 0xFFFF);
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
        if (index < CentrifugeBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, CentrifugeBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 0, 1, false)) {
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
