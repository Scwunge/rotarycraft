package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.GearUnit;
import net.scwunge.rotarycraft.transmission.PowerBusBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The Power Bus container: a slot for a gear unit on each of its four sides (north, south, west, east; a side that faces another bus block
 * has none), over the player inventory, laid out on the original screen. Buttons 0 to 3 switch that side between torque mode and speed mode.
 */
public class PowerBusMenu extends AbstractContainerMenu {
    private static final int[] X = {102, 102, 66, 138};
    private static final int[] Y = {33, 105, 69, 69};

    @Nullable
    private final PowerBusBlockEntity bus;
    private final ContainerData data;
    private final BlockPos pos;

    public PowerBusMenu(int id, Inventory inventory, PowerBusBlockEntity bus) {
        this(id, inventory, bus, bus.items(), data(bus), bus.getBlockPos());
    }

    private static ContainerData data(PowerBusBlockEntity bus) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? bus.modeBits() : bus.slotBits();
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

    private PowerBusMenu(int id, Inventory inventory, @Nullable PowerBusBlockEntity bus, ItemStackHandler items, ContainerData data, BlockPos pos) {
        super(TransmissionRegistry.POWER_BUS_MENU.get(), id);
        this.bus = bus;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < 4; i++) {
            int bit = 1 << i;
            addSlot(new SlotItemHandler(items, i, X[i], Y[i]) {
                @Override
                public boolean isActive() {
                    return (PowerBusMenu.this.data.get(1) & bit) != 0;
                }

                @Override
                public boolean mayPlace(ItemStack stack) {
                    return isActive() && GearUnit.of(stack) != null;
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 30 + col * 18, 141 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 30 + col * 18, 195));
        }
        addDataSlots(data);
    }

    public static PowerBusMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new PowerBusMenu(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof PowerBusBlockEntity b ? b : null, new ItemStackHandler(4),
                new SimpleContainerData(2), pos);
    }

    /** True if the side set by this index (north, south, west, east) is in speed mode. */
    public boolean speedMode(int index) {
        return (data.get(0) & 1 << index) != 0;
    }

    /** True if that side can hold a gear unit. */
    public boolean hasSlot(int index) {
        return (data.get(1) & 1 << index) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (bus == null || bus.isRemoved() || id < 0 || id >= 4) {
            return false;
        }
        Direction side = PowerBusBlockEntity.SIDES[id];
        bus.setSideSpeedMode(side, !bus.isSideSpeedMode(side));
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
        if (index < 4 ? !moveItemStackTo(stack, 4, slots.size(), true) : !moveItemStackTo(stack, 0, 4, false)) {
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
        return bus == null || !bus.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
