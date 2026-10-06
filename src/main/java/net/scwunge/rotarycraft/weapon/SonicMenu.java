package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity;

/** The Sonic Weapon's container: no slots, only the screen where its volume is set. */
public class SonicMenu extends AbstractContainerMenu {
    private final SonicWeaponBlockEntity sonic;

    public SonicMenu(int id, Inventory inventory, SonicWeaponBlockEntity sonic) {
        super(WeaponRegistry.SONIC_MENU.get(), id);
        this.sonic = sonic;
    }

    public static SonicMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new SonicMenu(id, inventory, (SonicWeaponBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public SonicWeaponBlockEntity sonic() {
        return sonic;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !sonic.isRemoved() && player.distanceToSqr(sonic.getBlockPos().getCenter()) <= 64;
    }
}
