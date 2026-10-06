package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.weapon.turret.SonicWeaponBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

/** The Sonic Weapon's block: a power machine that remembers who placed it (for claims). */
public class SonicBlock extends MachineBlock {
    public SonicBlock(Properties props, Supplier<? extends BlockEntityType<? extends SonicWeaponBlockEntity>> type) {
        super(props, type, SonicWeaponBlockEntity::new);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof SonicWeaponBlockEntity sonic) {
            sonic.setOwner(player);
        }
    }
}
