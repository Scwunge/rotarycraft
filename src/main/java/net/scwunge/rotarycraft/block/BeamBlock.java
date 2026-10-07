package net.scwunge.rotarycraft.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A bit of the visible light beam a Floodlight makes in beam mode: glows at full brightness, has no collision, and is gone when the light is. */
public class BeamBlock extends Block {
    public BeamBlock() {
        super(BlockBehaviour.Properties.of().mapColor(MapColor.NONE).noCollission().noOcclusion().replaceable().instabreak().lightLevel(s -> 15)
                .pushReaction(PushReaction.DESTROY).isViewBlocking((s, l, p) -> false).isSuffocating((s, l, p) -> false));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state, BlockGetter level, BlockPos pos) {
        return true;
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1F;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }
}
