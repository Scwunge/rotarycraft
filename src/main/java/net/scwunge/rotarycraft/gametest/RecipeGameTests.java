package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.BlastFurnaceBlockEntity;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryParts;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Every part and machine can be made: catches recipes that failed to load (a typo in an ingredient drops the recipe). */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class RecipeGameTests {
    static final String TEMPLATE = "empty5x4x5";

    private static Set<Item> craftableItems(GameTestHelper helper) {
        var level = helper.getLevel();
        Set<Item> out = new HashSet<>();
        for (RecipeHolder<?> h : level.getRecipeManager().getRecipes()) {
            Recipe<?> r = h.value();
            ItemStack result = r.getResultItem(level.registryAccess());
            if (!result.isEmpty()) {
                out.add(result.getItem());
            }
        }
        return out;
    }

    @GameTest(template = TEMPLATE)
    public static void everyPartAndMachineHasARecipe(GameTestHelper helper) {
        Set<Item> craftable = craftableItems(helper);
        List<String> missing = new ArrayList<>();
        List<DeferredItem<?>> wanted = new ArrayList<>(RotaryParts.PARTS.values());
        RotaryParts.RODS.values().forEach(wanted::add);
        RotaryParts.GEARS.values().forEach(wanted::add);
        RotaryParts.GEAR_UNITS.values().forEach(m -> wanted.addAll(m.values()));
        RotaryParts.SHAFT_CORES.values().forEach(wanted::add);
        RotaryParts.BEARINGS.forEach((m, b) -> {
            if (m != net.scwunge.rotarycraft.power.ShaftMaterial.WOOD) {
                wanted.add(b); // the original has no wooden bearing recipe
            }
        });
        for (DeferredItem<?> d : wanted) {
            if (!craftable.contains(d.get())) {
                missing.add(d.getId().getPath());
            }
        }
        for (var holder : RotaryItems.ITEMS.getEntries()) {
            Item item = holder.get();
            if (item instanceof net.minecraft.world.item.BlockItem bi && bi.getBlock() instanceof MachineBlock && !craftable.contains(item)) {
                missing.add(holder.getId().getPath());
            }
        }
        helper.assertTrue(missing.isEmpty(), "no recipe makes: " + missing);
        helper.succeed();
    }

    /** The High-Temperature Combustor is crafted in the Blast Furnace's grid at 1100 C, as in the original. */
    @GameTest(template = TEMPLATE, timeoutTicks = 300)
    public static void blastFurnaceCraftsTheHighCombustor(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, RotaryBlocks.BLAST_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BlastFurnaceBlockEntity f = (BlastFurnaceBlockEntity) helper.getBlockEntity(pos);
        f.setTemperature(1150);
        Item steel = RotaryItems.HSLA_STEEL_INGOT.get();
        Item redGold = RotaryParts.part("red_gold_ingot").get();
        Item[] grid = {steel, redGold, steel, redGold, Items.REDSTONE, redGold, steel, RotaryParts.part("igniter").get(), steel};
        for (int i = 0; i < 9; i++) {
            f.items().setStackInSlot(1 + i, new ItemStack(grid[i]));
        }
        helper.succeedWhen(() -> {
            BlastFurnaceBlockEntity b = (BlastFurnaceBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(b.items().getStackInSlot(BlastFurnaceBlockEntity.SLOT_OUTPUT_CENTER).is(RotaryParts.part("high_combustor").get()),
                    "no combustor yet at " + b.getTemperature() + " C");
            helper.assertTrue(b.items().getStackInSlot(5).isEmpty(), "grid not used up");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void tooColdForBlastCrafting(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(pos, RotaryBlocks.BLAST_FURNACE.get().defaultBlockState().setValue(MachineBlock.FACING, Direction.NORTH));
        BlastFurnaceBlockEntity f = (BlastFurnaceBlockEntity) helper.getBlockEntity(pos);
        f.setTemperature(900);
        Item steel = RotaryItems.HSLA_STEEL_INGOT.get();
        Item redGold = RotaryParts.part("red_gold_ingot").get();
        Item[] grid = {steel, redGold, steel, redGold, Items.REDSTONE, redGold, steel, RotaryParts.part("igniter").get(), steel};
        for (int i = 0; i < 9; i++) {
            f.items().setStackInSlot(1 + i, new ItemStack(grid[i]));
        }
        helper.runAfterDelay(150, () -> {
            BlastFurnaceBlockEntity b = (BlastFurnaceBlockEntity) helper.getBlockEntity(pos);
            helper.assertTrue(b.items().getStackInSlot(BlastFurnaceBlockEntity.SLOT_OUTPUT_CENTER).isEmpty(), "crafted below 1100 C");
            helper.succeed();
        });
    }
}
