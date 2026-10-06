package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.scwunge.rotarycraft.registry.RotaryItems;

import java.util.List;

/**
 * Canola, as in the original: ten growth stages on hydrated farmland, growing one stage on a third of random ticks when
 * the light is at least 9 and nothing solid sits on it. It needs light 6 or sky to survive. Not bonemealable. Grown
 * plants drop (1-2) x (2-13) seeds; anything younger drops the one seed back.
 */
public class CanolaBlock extends CropBlock {
    public static final int GROWN = 9;
    public static final IntegerProperty AGE = IntegerProperty.create("age", 0, GROWN);
    private static final VoxelShape[] SHAPES = new VoxelShape[GROWN + 1];

    static {
        for (int i = 0; i <= GROWN; i++) {
            SHAPES[i] = Block.box(0, 0, 0, 16, Math.max(2, 16 * i / GROWN), 16);
        }
    }

    public CanolaBlock(Properties props) {
        super(props);
    }

    @Override
    public MapCodec<? extends CropBlock> codec() {
        return simpleCodec(CanolaBlock::new);
    }

    @Override
    protected IntegerProperty getAgeProperty() {
        return AGE;
    }

    @Override
    public int getMaxAge() {
        return GROWN;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(AGE);
    }

    @Override
    protected ItemLike getBaseSeedId() {
        return RotaryItems.CANOLA_SEEDS.get();
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPES[state.getValue(AGE)];
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getBlock() instanceof FarmBlock && state.getValue(FarmBlock.MOISTURE) > 0;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        boolean light = level.getRawBrightness(pos, 0) >= 6 || level.canSeeSky(pos);
        return light && mayPlaceOn(level.getBlockState(pos.below()), level, pos.below());
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!canSurvive(state, level, pos)) {
            level.destroyBlock(pos, true);
            return;
        }
        int age = state.getValue(AGE);
        if (age < GROWN && level.getRawBrightness(pos, 0) >= 9 && !blockedAbove(level, pos) && random.nextInt(3) == 0) {
            level.setBlock(pos, state.setValue(AGE, age + 1), Block.UPDATE_CLIENTS);
        }
    }

    private static boolean blockedAbove(LevelReader level, BlockPos pos) {
        BlockState above = level.getBlockState(pos.above());
        return above.isSolid() && !above.canOcclude();
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    /** The original's seed count for a grown plant: (1-2) x (2-13), raised by fortune. */
    public static int grownDrops(int fortune, RandomSource rand) {
        int n = Math.max(fortune * 2, (1 + rand.nextInt(2)) * (2 + rand.nextInt(8) + rand.nextInt(5)));
        if (fortune > 0) {
            n = Math.max(n, (int) (n * rand.nextDouble() * (1 + fortune)));
        }
        return n;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        int count = 1;
        if (state.getValue(AGE) == GROWN) {
            ItemStack tool = params.getOptionalParameter(LootContextParams.TOOL);
            int fortune = tool == null ? 0 : EnchantmentHelper.getItemEnchantmentLevel(
                    params.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(Enchantments.FORTUNE), tool);
            count = grownDrops(fortune, params.getLevel().random);
        }
        return List.of(new ItemStack(RotaryItems.CANOLA_SEEDS.get(), count));
    }
}
