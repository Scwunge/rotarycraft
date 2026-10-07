package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.MultiClutchBlockEntity;
import org.jetbrains.annotations.Nullable;

/** The Multi-Clutch's container: no slots, sixteen buttons (0 to 15), each changing the side power leaves by for that redstone strength. */
public class MultiClutchMenu extends AbstractContainerMenu {
    @Nullable
    private final MultiClutchBlockEntity clutch;
    private final ContainerData data;
    private final BlockPos pos;

    public MultiClutchMenu(int id, Inventory inventory, MultiClutchBlockEntity clutch) {
        this(id, clutch, clutch.data(), clutch.getBlockPos());
    }

    private MultiClutchMenu(int id, @Nullable MultiClutchBlockEntity clutch, ContainerData data, BlockPos pos) {
        super(TransmissionRegistry.MULTI_CLUTCH_MENU.get(), id);
        this.clutch = clutch;
        this.data = data;
        this.pos = pos;
        addDataSlots(data);
    }

    public static MultiClutchMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new MultiClutchMenu(id, inventory.player.level().getBlockEntity(pos) instanceof MultiClutchBlockEntity c ? c : null,
                new SimpleContainerData(MultiClutchBlockEntity.DATA_COUNT), pos);
    }

    /** The side (an index into the directions) set for a redstone strength. */
    public int side(int state) {
        return data.get(state);
    }

    /** The redstone strength at the block now. */
    public int redstone() {
        return data.get(MultiClutchBlockEntity.STATES);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clutch == null || clutch.isRemoved() || id < 0 || id >= MultiClutchBlockEntity.STATES) {
            return false;
        }
        clutch.cycleState(id);
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return clutch == null || !clutch.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
