package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scwunge.rotarycraft.registry.WeaponRegistry;

/** The screen container of a machine with a radius to set: no slots. */
public class RangeMenu extends AbstractContainerMenu {
    private final BlockEntity entity;
    private final RangeHost host;

    /** {@code entity} must be a {@link RangeHost}. */
    public RangeMenu(int id, Inventory inventory, BlockEntity entity) {
        super(WeaponRegistry.RANGE_MENU.get(), id);
        this.entity = entity;
        this.host = (RangeHost) entity;
    }

    public static RangeMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new RangeMenu(id, inventory, inventory.player.level().getBlockEntity(pos));
    }

    public RangeHost host() {
        return host;
    }

    public BlockPos pos() {
        return entity.getBlockPos();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !entity.isRemoved() && player.distanceToSqr(entity.getBlockPos().getCenter()) <= 64;
    }
}
