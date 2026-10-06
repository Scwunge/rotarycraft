package net.scwunge.rotarycraft.weapon;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Supplier;

/** The TNT Cannon's block: an ordinary power machine that remembers who placed it (for claims). */
public class CannonBlock extends MachineBlock {
    public CannonBlock(Properties props, Supplier<? extends BlockEntityType<? extends TntCannonBlockEntity>> type,
                       BiFunction<BlockPos, BlockState, ? extends TntCannonBlockEntity> factory) {
        super(props, type, factory);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (placer instanceof Player player && level.getBlockEntity(pos) instanceof TntCannonBlockEntity cannon) {
            cannon.setOwner(player);
        }
    }
}
