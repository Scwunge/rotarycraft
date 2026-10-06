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
import net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/** Crystallizer screen, slots from the original: the output in the middle and the dry ice to its right; the tank down the left. */
public class CrystallizerMenu extends AbstractContainerMenu {
    public static final int DATA_COUNT = 10;

    private final ContainerData data;
    private final BlockPos pos;

    public CrystallizerMenu(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        this(id, inventory, buf.readBlockPos(), new SimpleContainerData(DATA_COUNT));
    }

    private CrystallizerMenu(int id, Inventory inventory, BlockPos pos, ContainerData data) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof CrystallizerBlockEntity c ? c.items()
                : new ItemStackHandler(CrystallizerBlockEntity.SLOTS), pos, data);
    }

    public CrystallizerMenu(int id, Inventory inventory, CrystallizerBlockEntity crystallizer, ContainerData data) {
        this(id, inventory, crystallizer.items(), crystallizer.getBlockPos(), data);
    }

    private CrystallizerMenu(int id, Inventory inventory, ItemStackHandler items, BlockPos pos, ContainerData data) {
        super(RotaryMenus.CRYSTALLIZER.get(), id);
        this.data = data;
        this.pos = pos;
        addSlot(new SlotItemHandler(items, CrystallizerBlockEntity.SLOT_OUTPUT, 80, 35) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return false;
            }
        });
        addSlot(new SlotItemHandler(items, CrystallizerBlockEntity.SLOT_DRY_ICE, 125, 35));
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

    public Fluid fluid() {
        return BuiltInRegistries.FLUID.byId(data.get(3) & 0xFFFF);
    }

    public int temperature() {
        return (short) data.get(4);
    }

    public int freezingPoint() {
        return (short) data.get(5);
    }

    public int omega() {
        return (data.get(6) & 0xFFFF) | (data.get(7) << 16);
    }

    public int torque() {
        return (data.get(8) & 0xFFFF) | (data.get(9) << 16);
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
        if (index < CrystallizerBlockEntity.SLOTS) {
            if (!moveItemStackTo(stack, CrystallizerBlockEntity.SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(stack, 1, 2, false)) {
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
