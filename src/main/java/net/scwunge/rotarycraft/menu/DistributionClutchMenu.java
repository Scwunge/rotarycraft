package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.DistributionClutchBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The Distribution Clutch's container: no slots. Buttons 0 to 3 turn the north, south, west and east sides on and off (in GUI mode), and
 * {@link #MODE} changes how the sides are chosen. The torque each side asks for is sent by its own packet (see TransmissionNetwork).
 */
public class DistributionClutchMenu extends AbstractContainerMenu {
    public static final int MODE = 4;

    @Nullable
    private final DistributionClutchBlockEntity clutch;
    private final BlockPos pos;

    public DistributionClutchMenu(int id, Inventory inventory, DistributionClutchBlockEntity clutch) {
        this(id, clutch, clutch.getBlockPos());
    }

    private DistributionClutchMenu(int id, @Nullable DistributionClutchBlockEntity clutch, BlockPos pos) {
        super(TransmissionRegistry.DISTRIBUTION_CLUTCH_MENU.get(), id);
        this.clutch = clutch;
        this.pos = pos;
    }

    public static DistributionClutchMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new DistributionClutchMenu(id, inventory.player.level().getBlockEntity(pos) instanceof DistributionClutchBlockEntity c ? c : null, pos);
    }

    @Nullable
    public DistributionClutchBlockEntity clutch() {
        return clutch;
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (clutch == null || clutch.isRemoved()) {
            return false;
        }
        if (id == MODE) {
            clutch.stepControl();
            return true;
        }
        if (id >= 0 && id < 4) {
            clutch.setSideEnabled(DistributionClutchBlockEntity.SIDES[id], !clutch.isSideEnabled(DistributionClutchBlockEntity.SIDES[id]));
            return true;
        }
        return false;
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
