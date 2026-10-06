package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.rotarycraft.blockentity.ShaftBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

/** A shaft of one material: a thin rod along its facing axis. */
public class ShaftBlock extends MachineBlock {
    private static final VoxelShape ALONG_X = box(0, 6, 6, 16, 10, 10);
    private static final VoxelShape ALONG_Y = box(6, 0, 6, 10, 16, 10);
    private static final VoxelShape ALONG_Z = box(6, 6, 0, 10, 10, 16);

    private final ShaftMaterial material;

    public ShaftBlock(Properties props, ShaftMaterial material) {
        super(props, RotaryBlockEntities.SHAFT, ShaftBlockEntity::new);
        this.material = material;
    }

    public ShaftMaterial material() {
        return material;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new ShaftBlock(p, material));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return switch (state.getValue(FACING).getAxis()) {
            case X -> ALONG_X;
            case Y -> ALONG_Y;
            case Z -> ALONG_Z;
        };
    }
}
