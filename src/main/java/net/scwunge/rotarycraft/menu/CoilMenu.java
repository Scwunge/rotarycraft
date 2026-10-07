package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.TransmissionRegistry;
import net.scwunge.rotarycraft.transmission.AdvancedGearBlockEntity;
import org.jetbrains.annotations.Nullable;

/** The energy coil's container: no slots; the speed and torque it releases at are sent by their own packets (see TransmissionNetwork). */
public class CoilMenu extends AbstractContainerMenu {
    @Nullable
    private final AdvancedGearBlockEntity coil;
    private final BlockPos pos;

    public CoilMenu(int id, Inventory inventory, AdvancedGearBlockEntity coil) {
        this(id, coil, coil.getBlockPos());
    }

    private CoilMenu(int id, @Nullable AdvancedGearBlockEntity coil, BlockPos pos) {
        super(TransmissionRegistry.COIL_MENU.get(), id);
        this.coil = coil;
        this.pos = pos;
    }

    public static CoilMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CoilMenu(id, inventory.player.level().getBlockEntity(pos) instanceof AdvancedGearBlockEntity c ? c : null, pos);
    }

    public BlockPos pos() {
        return pos;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return coil == null || !coil.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
