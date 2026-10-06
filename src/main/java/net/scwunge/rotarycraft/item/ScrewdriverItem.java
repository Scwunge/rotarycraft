package net.scwunge.rotarycraft.item;

import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.MachineBlock;

/**
 * Rotates RotaryCraft machines: right-click to turn the output to the next direction, sneak-right-click to point the
 * output at the face you clicked.
 */
public class ScrewdriverItem extends Item {
    public ScrewdriverItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!(state.getBlock() instanceof MachineBlock)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            Direction current = state.getValue(MachineBlock.FACING);
            Direction next = context.getPlayer() != null && context.getPlayer().isShiftKeyDown()
                    ? context.getClickedFace()
                    : Direction.from3DDataValue((current.get3DDataValue() + 1) % 6);
            level.setBlock(context.getClickedPos(), state.setValue(MachineBlock.FACING, next), 3);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
