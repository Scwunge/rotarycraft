package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Sorting Machine (TileEntitySorting): takes the items from the inventory above it, or lying on top of it, and sends each out of the side its pattern says: the
 * three rows of nine pattern slots on its screen are the three sides other than the one it takes power from, and an item goes out of the side whose row has that
 * item in it, or out of the bottom if no row has. Into an inventory there if it can, otherwise as a dropped item thrown out. It sorts one item a tick at 1 kW,
 * and up to 64 at 16 kW.
 */
public class SortingBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "sorting";
    public static final int LENGTH = 9;
    public static final int MIN_POWER = 1024;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    private static final Direction[] SIDES = {Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST};
    public static final GuiLayout LAYOUT = buildLayout();

    private static GuiLayout buildLayout() {
        GuiLayout.Builder b = GuiLayout.named(NAME).size(176, 180).inventoryAt(8, 98).ghostSlots();
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < LENGTH; col++) {
                b.slot(8 + col * 18, 18 + row * 22);
            }
        }
        return b.build();
    }

    public SortingBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.SORTING.type().get(), pos, state, LENGTH * 3, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** Each item is in at most one pattern slot. */
    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        for (int i = 0; i < items.getSlots(); i++) {
            if (i != slot && items.getStackInSlot(i).is(stack.getItem())) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    /** The patterns are not items: nothing is dropped when it is broken. */
    @Override
    public boolean dropsInventory() {
        return false;
    }

    /** The side the row of a pattern slot stands for: the horizontal sides but the one the power comes in at, in the order north, south, west, east. */
    public Direction rowSide(int slot) {
        List<Direction> sides = new ArrayList<>(List.of(SIDES));
        sides.remove(inputSide());
        return sides.get(Math.min(sides.size() - 1, slot / LENGTH));
    }

    public Direction sideFor(ItemStack stack) {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(stack.getItem())) {
                return rowSide(i);
            }
        }
        return Direction.DOWN;
    }

    /** How many items it sorts a tick at the power it has. */
    public int cyclesPerTick() {
        long frac = getPower() / MIN_POWER;
        if (frac >= 16) {
            return 64;
        } else if (frac >= 12) {
            return 32;
        } else if (frac >= 8) {
            return 16;
        } else if (frac >= 4) {
            return 4;
        } else if (frac >= 2) {
            return 2;
        }
        return 1;
    }

    /** Sends one of an item out of {@code dir}: into the inventory there, or thrown out. */
    private void dump(ServerLevel server, ItemStack one, Direction dir) {
        IItemHandler there = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
        if (there != null) {
            one = ItemHandlerHelper.insertItem(there, one, false);
            if (one.isEmpty()) {
                return;
            }
        }
        ItemEntity entity = new ItemEntity(server, worldPosition.getX() + 0.5 + dir.getStepX() * 0.75, worldPosition.getY() + 0.5 + dir.getStepY() * 0.75,
                worldPosition.getZ() + 0.5 + dir.getStepZ() * 0.75, one);
        entity.setDeltaMovement(new Vec3(dir.getStepX(), dir.getStepY(), dir.getStepZ()).scale(0.1));
        server.addFreshEntity(entity);
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("sortingMachine")) {
            return;
        }
        int cycles = cyclesPerTick();
        IItemHandler above = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.above(), Direction.DOWN);
        if (above != null) {
            for (int slot = 0; slot < above.getSlots(); slot++) {
                for (int i = 0; i < cycles; i++) {
                    ItemStack one = above.extractItem(slot, 1, false);
                    if (one.isEmpty()) {
                        break;
                    }
                    dump(server, one, sideFor(one));
                }
            }
            return;
        }
        AABB box = new AABB(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + 1.25, worldPosition.getZ() + 1);
        for (ItemEntity entity : server.getEntitiesOfClass(ItemEntity.class, box, e -> e.isAlive() && !e.getItem().isEmpty())) {
            for (int i = 0; i < cycles && !entity.getItem().isEmpty(); i++) {
                ItemStack one = entity.getItem().split(1);
                if (entity.getItem().isEmpty()) {
                    entity.discard();
                }
                dump(server, one, sideFor(one));
            }
        }
    }
}
