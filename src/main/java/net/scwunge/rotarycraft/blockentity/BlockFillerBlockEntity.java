package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.machine.AreaFillerBlockEntity;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Block Filler (TileEntityBlockFiller): holds a stack of one kind of block and, with power, fills the space beneath it with it, a block at a time, lowest
 * layer first (see {@link AreaFillerBlockEntity}). Soft blocks take 512 W, ordinary ones 1024 W, stone 2048 W and metal 4096 W. Off unless the server enables it.
 */
public class BlockFillerBlockEntity extends AreaFillerBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final String NAME = "block_filler";
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(80, 35).build();

    public BlockFillerBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.BLOCK_FILLER.type().get(), pos, state, 1, NAME, "blockFiller");
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Nullable
    private static BlockState blockOf(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item)) {
            return null;
        }
        BlockState state = item.getBlock().defaultBlockState();
        return state.isAir() ? null : state;
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return blockOf(stack) != null;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    @Override
    protected boolean allowFluidOverwrite() {
        return true;
    }

    @Override
    protected boolean isFluidBlock(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    @Override
    protected boolean hasRemainingBlocks() {
        return blockOf(items.getStackInSlot(0)) != null;
    }

    @Nullable
    @Override
    protected BlockState nextBlock() {
        return blockOf(items.getStackInSlot(0));
    }

    @Override
    protected void onBlockPlaced() {
        items.extractItem(0, 1, false);
    }

    /** getRequiredPower: soft blocks 512, metal 4096, stone 2048, anything else 1024. */
    @Override
    protected long requiredPower() {
        BlockState state = nextBlock();
        if (state == null || !state.blocksMotion()) {
            return 512;
        }
        SoundType sound = state.getSoundType();
        if (sound == SoundType.METAL || sound == SoundType.ANVIL || sound == SoundType.NETHERITE_BLOCK || sound == SoundType.COPPER) {
            return 4096;
        }
        return state.is(BlockTags.MINEABLE_WITH_PICKAXE) && sound != SoundType.WOOD ? 2048 : 1024;
    }
}
