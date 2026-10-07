package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.decor.DecoTank;
import net.scwunge.rotarycraft.decor.DecoTankBlock;
import net.scwunge.rotarycraft.decor.DecoTankBlockEntity;
import net.scwunge.rotarycraft.decor.DecoTankItem;
import net.scwunge.rotarycraft.decor.DecoTankSettingsRecipe;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.List;

/** The Decorative Tank: the item, the block it places, and the recipe that changes its settings. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class DecoTankGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final BlockPos AT = new BlockPos(2, 2, 2);

    static ItemStack fullTank(int flags) {
        ItemStack tank = new ItemStack(DecorRegistry.DECO_TANK.get());
        tank.set(DecoTank.FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(Fluids.LAVA, DecoTank.FILL)));
        if (flags != 0) {
            tank.set(DecoTank.FLAGS.get(), flags);
        }
        return tank;
    }

    static BlockState place(GameTestHelper helper, ItemStack stack) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockPos abs = helper.absolutePos(AT);
        BlockPlaceContext context = new BlockPlaceContext(helper.getLevel(), player, InteractionHand.MAIN_HAND, stack,
                new BlockHitResult(Vec3.atCenterOf(abs), Direction.UP, abs.below(), false));
        BlockState state = DecorRegistry.DECO_TANK_BLOCK.get().getStateForPlacement(context);
        helper.setBlock(AT, state);
        DecorRegistry.DECO_TANK_BLOCK.get().setPlacedBy(helper.getLevel(), abs, state, player, stack);
        return state;
    }

    @GameTest(template = SMALL, batch = "tank_place", timeoutTicks = 40)
    public static void aFullTankPlacesAFullTankWithItsSettings(GameTestHelper helper) {
        BlockState state = place(helper, fullTank(DecoTank.Flag.LIGHTED.bit() | DecoTank.Flag.RESISTANT.bit()));
        DecoTankBlockEntity tank = helper.getBlockEntity(AT);
        helper.assertTrue(tank.fluid().is(Fluids.LAVA) && tank.fluid().getAmount() == DecoTank.FILL, "the tank should hold the 25 mB of lava");
        helper.assertTrue(state.getValue(DecoTank.Flag.LIGHTED.property) && state.getValue(DecoTank.Flag.RESISTANT.property)
                && !state.getValue(DecoTank.Flag.CLEAR.property) && !state.getValue(DecoTank.Flag.NOCOLOR.property), "settings " + state);
        helper.assertTrue(state.getLightEmission() == 15, "a glowing tank gives full light");
        helper.assertTrue(state.getBlock().getExplosionResistance(state, helper.getLevel(), helper.absolutePos(AT), null) == 600_000F, "resistant tanks shrug off explosions");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "tank_empty", timeoutTicks = 40)
    public static void anEmptyOrPartlyFilledTankPlacesAnEmptyOne(GameTestHelper helper) {
        ItemStack part = new ItemStack(DecorRegistry.DECO_TANK.get());
        part.set(DecoTank.FLUID.get(), SimpleFluidContent.copyOf(new FluidStack(Fluids.WATER, 10)));
        place(helper, part);
        DecoTankBlockEntity tank = helper.getBlockEntity(AT);
        helper.assertTrue(tank.fluid().isEmpty(), "ten mB is not a full tank");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "tank_drops", timeoutTicks = 40)
    public static void aBrokenTankGivesBackItsFluidAndSettings(GameTestHelper helper) {
        BlockState state = place(helper, fullTank(DecoTank.Flag.CLEAR.bit() | DecoTank.Flag.NOCOLOR.bit()));
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(state, helper.getLevel(), helper.absolutePos(AT), helper.getBlockEntity(AT));
        helper.assertTrue(drops.size() == 1, "one drop expected, " + drops);
        ItemStack back = drops.get(0);
        helper.assertTrue(back.is(DecorRegistry.DECO_TANK.get()) && DecoTankItem.isFull(back) && DecoTankItem.fluidOf(back).is(Fluids.LAVA), "the fluid should come back");
        helper.assertTrue(DecoTank.has(back, DecoTank.Flag.CLEAR) && DecoTank.has(back, DecoTank.Flag.NOCOLOR) && !DecoTank.has(back, DecoTank.Flag.LIGHTED), "settings " + DecoTank.flags(back));
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "tank_item", timeoutTicks = 40)
    public static void aTankItemFillsWithOneFluidUpToTwentyFiveMillibuckets(GameTestHelper helper) {
        ItemStack tank = new ItemStack(DecorRegistry.DECO_TANK.get());
        IFluidHandlerItem handler = tank.getCapability(Capabilities.FluidHandler.ITEM);
        helper.assertTrue(handler != null && handler.getTankCapacity(0) == DecoTank.FILL, "capacity");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.WATER, 100), IFluidHandler.FluidAction.EXECUTE) == DecoTank.FILL, "it takes 25 mB of 100");
        helper.assertTrue(DecoTankItem.isFull(handler.getContainer()) && DecoTankItem.fluidOf(handler.getContainer()).is(Fluids.WATER), "now full of water");
        helper.assertTrue(handler.fill(new FluidStack(Fluids.LAVA, 10), IFluidHandler.FluidAction.SIMULATE) == 0, "a full tank takes nothing more");
        helper.assertTrue(handler.drain(DecoTank.FILL, IFluidHandler.FluidAction.EXECUTE).getAmount() == DecoTank.FILL, "and can be emptied");
        helper.assertFalse(DecoTankItem.isFull(handler.getContainer()), "empty again");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "tank_recipe", timeoutTicks = 40)
    public static void theSettingsRecipeTogglesSettingsAndKeepsTheFluid(GameTestHelper helper) {
        DecoTankSettingsRecipe recipe = new DecoTankSettingsRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        var registries = helper.getLevel().registryAccess();
        ItemStack tank = fullTank(DecoTank.Flag.RESISTANT.bit());
        CraftingInput glow = CraftingInput.of(3, 3, List.of(tank, new ItemStack(Items.GLOWSTONE_DUST), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertTrue(recipe.matches(glow, helper.getLevel()), "a tank and glowstone dust");
        ItemStack out = recipe.assemble(glow, registries);
        helper.assertTrue(DecoTank.has(out, DecoTank.Flag.LIGHTED) && DecoTank.has(out, DecoTank.Flag.RESISTANT) && DecoTankItem.isFull(out), "glowing now, still resistant and full");
        CraftingInput off = CraftingInput.of(3, 3, List.of(out, new ItemStack(Items.OBSIDIAN), new ItemStack(Items.RED_DYE), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY));
        ItemStack out2 = recipe.assemble(off, registries);
        helper.assertTrue(!DecoTank.has(out2, DecoTank.Flag.RESISTANT) && DecoTank.has(out2, DecoTank.Flag.NOCOLOR) && DecoTank.has(out2, DecoTank.Flag.LIGHTED), "obsidian switches resistant off, a dye switches colour off");
        CraftingInput two = CraftingInput.of(3, 3, List.of(tank, tank, new ItemStack(Items.GLOWSTONE_DUST), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertFalse(recipe.matches(two, helper.getLevel()), "two tanks");
        CraftingInput alone = CraftingInput.of(3, 3, List.of(tank, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertFalse(recipe.matches(alone, helper.getLevel()), "a tank alone");
        CraftingInput stray = CraftingInput.of(3, 3, List.of(tank, new ItemStack(Items.STICK), ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY,
                ItemStack.EMPTY, ItemStack.EMPTY));
        helper.assertFalse(recipe.matches(stray, helper.getLevel()), "a stick is no setting");
        helper.succeed();
    }
}
