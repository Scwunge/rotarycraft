package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.blockentity.BorerBlockEntity;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * The Borer's menu: it has no slots; the screen's buttons come in as menu button clicks. Buttons 0 to 34 toggle a cell of the cut shape,
 * then {@link #TOGGLE_ALL}, {@link #RESET} and {@link #TOGGLE_DROPS}. The screen reads what it shows from the synced data words.
 */
public class BorerMenu extends AbstractContainerMenu {
    public static final int TOGGLE_ALL = 100;
    public static final int RESET = 101;
    public static final int TOGGLE_DROPS = 102;
    public static final int FLAG_JAMMED = 1;
    public static final int FLAG_DROPS = 2;
    public static final int FLAG_WORN = 4;

    @Nullable
    private final BorerBlockEntity borer;
    private final ContainerData data;
    private final BlockPos pos;

    public BorerMenu(int id, Inventory inventory, BorerBlockEntity borer) {
        super(WorldMachineRegistry.BORER_MENU.get(), id);
        this.borer = borer;
        this.pos = borer.getBlockPos();
        this.data = borer.data();
        addDataSlots(data);
    }

    public BorerMenu(int id, Inventory inventory, BlockPos pos) {
        super(WorldMachineRegistry.BORER_MENU.get(), id);
        this.borer = null;
        this.pos = pos;
        this.data = new SimpleContainerData(BorerBlockEntity.DATA_COUNT);
        addDataSlots(data);
    }

    public static BorerMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new BorerMenu(id, inventory, buf.readBlockPos());
    }

    public BlockPos pos() {
        return pos;
    }

    public int omega() {
        return data.get(0);
    }

    public int torque() {
        return data.get(1);
    }

    public int requiredPower() {
        return data.get(2);
    }

    public int requiredTorque() {
        return data.get(3);
    }

    public boolean jammed() {
        return (data.get(4) & FLAG_JAMMED) != 0;
    }

    public boolean drops() {
        return (data.get(4) & FLAG_DROPS) != 0;
    }

    public boolean worn() {
        return (data.get(4) & FLAG_WORN) != 0;
    }

    public long power() {
        return (long) torque() * omega();
    }

    public int step() {
        return data.get(7);
    }

    public boolean cell(int col, int row) {
        long mask = (data.get(5) & 0xFFFFFFFFL) | ((long) data.get(6) << 32);
        return (mask & (1L << (col * BorerBlockEntity.ROWS + row))) != 0;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (borer == null || borer.isRemoved()) {
            return false;
        }
        if (id >= 0 && id < BorerBlockEntity.COLS * BorerBlockEntity.ROWS) {
            borer.toggleCell(id / BorerBlockEntity.ROWS, id % BorerBlockEntity.ROWS);
            return true;
        }
        switch (id) {
            case TOGGLE_ALL -> borer.toggleAll();
            case RESET -> borer.reset();
            case TOGGLE_DROPS -> borer.toggleDrops();
            default -> {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return borer != null ? !borer.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64 : true;
    }
}
