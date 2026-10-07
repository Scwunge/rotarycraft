package net.scwunge.rotarycraft.gametest;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.neoforge.items.IItemHandler;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.crafting.AutoCrafterBlockEntity;
import net.scwunge.rotarycraft.crafting.CraftPattern;
import net.scwunge.rotarycraft.crafting.PatternMode;
import net.scwunge.rotarycraft.crafting.WorktableBlockEntity;
import net.scwunge.rotarycraft.menu.AutoCrafterMenu;
import net.scwunge.rotarycraft.menu.CraftPatternMenu;
import net.scwunge.rotarycraft.menu.WorktableMenu;
import net.scwunge.rotarycraft.recipe.WorktableRecipe;
import net.scwunge.rotarycraft.registry.CraftingRegistry;
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.ArrayList;
import java.util.List;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class CraftingGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos TABLE = new BlockPos(2, 2, 2);
    static final BlockPos CHEST = TABLE.above();

    /** A Craft Pattern holding the recipe these ingredients make; grid slots not named stay empty. */
    static ItemStack pattern(GameTestHelper helper, Object... slotThenItem) {
        List<ItemStack> grid = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            grid.add(ItemStack.EMPTY);
        }
        for (int i = 0; i < slotThenItem.length; i += 2) {
            grid.set((Integer) slotThenItem[i], new ItemStack((net.minecraft.world.item.Item) slotThenItem[i + 1]));
        }
        ItemStack stack = new ItemStack(CraftingRegistry.CRAFT_PATTERN.get());
        stack.set(RotaryComponents.CRAFT_PATTERN.get(), CraftPattern.write(helper.getLevel(), PatternMode.CRAFTING, grid, 64));
        return stack;
    }

    static ItemStack sticks(GameTestHelper helper) {
        return pattern(helper, 0, Items.OAK_PLANKS, 3, Items.OAK_PLANKS);
    }

    static WorktableBlockEntity table(GameTestHelper helper) {
        helper.setBlock(TABLE, CraftingRegistry.WORKTABLE.get().defaultBlockState());
        return helper.getBlockEntity(TABLE);
    }

    static AutoCrafterBlockEntity crafter(GameTestHelper helper, int torque, int omega) {
        if (torque > 0) {
            WeaponGameTests.spinningFlywheel(helper, TABLE.below(), torque, omega);
        }
        helper.setBlock(TABLE, CraftingRegistry.AUTO_CRAFTER.get().defaultBlockState());
        helper.setBlock(CHEST, Blocks.CHEST);
        return helper.getBlockEntity(TABLE);
    }

    static ChestBlockEntity chest(GameTestHelper helper) {
        return helper.getBlockEntity(CHEST);
    }

    // ---- the pattern ----

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aPatternWritesWhatItsRecipeMakesAndKeepsItWhenSaved(GameTestHelper helper) {
        ItemStack stack = sticks(helper);
        CraftPattern pattern = CraftPattern.of(stack);
        helper.assertTrue(pattern.output().is(Items.STICK) && pattern.output().getCount() == 4, "the recipe made " + pattern.output());
        helper.assertTrue(pattern.hasRecipe() && pattern.recipe(helper.getLevel()).isPresent(), "the recipe should be found again");
        var registries = helper.getLevel().registryAccess();
        ItemStack back = ItemStack.parse(registries, stack.save(registries)).orElseThrow();
        helper.assertTrue(CraftPattern.of(back).equals(pattern), "a saved pattern came back different");
        helper.assertTrue(ItemStack.isSameItemSameComponents(back, stack), "two patterns written alike should stack");
        CraftPattern other = CraftPattern.write(helper.getLevel(), PatternMode.CRAFTING, List.of(new ItemStack(Items.COBBLESTONE)), 64);
        helper.assertFalse(other.hasRecipe(), "one cobblestone makes nothing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aWorktableRecipeIsReadFromDataAndKnownOnlyToTheWorktable(GameTestHelper helper) {
        String json = "{\"type\":\"rotarycraft:worktable\",\"pattern\":[\"D\"],\"key\":{\"D\":{\"item\":\"minecraft:dirt\"}},\"result\":{\"id\":\"minecraft:diamond\",\"count\":2}}";
        RegistryOps<com.google.gson.JsonElement> ops = helper.getLevel().registryAccess().createSerializationContext(JsonOps.INSTANCE);
        Recipe<?> recipe = Recipe.CODEC.parse(ops, JsonParser.parseString(json)).getOrThrow();
        helper.assertTrue(recipe instanceof WorktableRecipe, "read " + recipe);
        CraftingInput dirt = CraftingInput.of(1, 1, List.of(new ItemStack(Items.DIRT)));
        helper.assertTrue(((WorktableRecipe) recipe).matches(dirt, helper.getLevel()), "one dirt should match");
        helper.assertFalse(PatternMode.CRAFTING.find(helper.getLevel(), dirt).isPresent(), "a crafting table does not know it");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void thePatternScreenWritesTheGridIntoThePatternInHand(GameTestHelper helper) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(CraftingRegistry.CRAFT_PATTERN.get()));
        CraftPatternMenu menu = new CraftPatternMenu(1, player.getInventory(), net.minecraft.world.InteractionHand.MAIN_HAND);
        menu.setCarried(new ItemStack(Items.OAK_PLANKS, 5));
        menu.clicked(0, 0, ClickType.PICKUP, player);
        menu.clicked(3, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().getCount() == 5, "the pattern screen took the planks from your hand");
        CraftPattern pattern = CraftPattern.of(player.getMainHandItem());
        helper.assertTrue(pattern.output().is(Items.STICK), "pattern output " + pattern.output());
        helper.assertTrue(menu.getSlot(9).getItem().is(Items.STICK), "the result slot should show the sticks");
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(3, 0, ClickType.PICKUP, player);
        helper.assertTrue(CraftPattern.of(player.getMainHandItem()).output().is(Items.OAK_BUTTON), "one plank alone should make a button");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        helper.assertFalse(CraftPattern.of(player.getMainHandItem()).hasRecipe(), "clearing a slot should clear the recipe");
        helper.assertTrue(menu.clickMenuButton(player, CraftPatternMenu.LIMIT_DOWN_1), "limit button");
        helper.assertTrue(CraftPattern.of(player.getMainHandItem()).limit() == 63, "limit " + CraftPattern.of(player.getMainHandItem()).limit());
        menu.clickMenuButton(player, CraftPatternMenu.LIMIT_DOWN_64);
        helper.assertTrue(CraftPattern.of(player.getMainHandItem()).limit() == 1, "limit should stop at 1");
        menu.clickMenuButton(player, CraftPatternMenu.LIMIT_UP_16);
        helper.assertTrue(CraftPattern.of(player.getMainHandItem()).limit() == 16, "from 1, +16 makes 16, as the original");
        menu.clickMenuButton(player, CraftPatternMenu.MODE);
        helper.assertTrue(CraftPattern.of(player.getMainHandItem()).mode() == PatternMode.WORKTABLE, "mode " + CraftPattern.of(player.getMainHandItem()).mode());
        menu.clickMenuButton(player, CraftPatternMenu.MODE);
        menu.clickMenuButton(player, CraftPatternMenu.MODE);
        helper.assertTrue(CraftPattern.of(player.getMainHandItem()).mode() == PatternMode.CRAFTING, "the modes should go round");
        helper.succeed();
    }

    // ---- the worktable ----

    private static void lay(WorktableBlockEntity table, int slot, ItemStack stack) {
        table.items().setStackInSlot(slot, stack);
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aWorktableCraftsWhenARedstoneSignalArrives(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        lay(table, 0, new ItemStack(Items.OAK_PLANKS, 3));
        lay(table, 3, new ItemStack(Items.OAK_PLANKS, 3));
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(table.items().getStackInSlot(WorktableBlockEntity.MAIN_OUTPUT).isEmpty(), "it crafted without a signal");
            helper.setBlock(TABLE.east(), Blocks.REDSTONE_BLOCK);
        });
        helper.runAfterDelay(6, () -> {
            ItemStack out = table.items().getStackInSlot(WorktableBlockEntity.MAIN_OUTPUT);
            helper.assertTrue(out.is(Items.STICK) && out.getCount() == 4, "output " + out);
            helper.assertTrue(table.items().getStackInSlot(0).getCount() == 2 && table.items().getStackInSlot(3).getCount() == 2, "it should use one of each");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aWorktableGivesBackTheEmptyBottle(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        lay(table, 0, new ItemStack(Items.HONEY_BOTTLE));
        helper.setBlock(TABLE.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(table.items().getStackInSlot(WorktableBlockEntity.MAIN_OUTPUT).is(Items.SUGAR), "no sugar: " + table.items().getStackInSlot(WorktableBlockEntity.MAIN_OUTPUT));
            helper.assertTrue(table.items().getStackInSlot(0).is(Items.GLASS_BOTTLE), "the bottle should go back in the grid, got " + table.items().getStackInSlot(0));
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aWorktableTakesApartALoneItemInTheMiddle(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        lay(table, 4, new ItemStack(Items.HOPPER));
        helper.setBlock(TABLE.east(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(4, () -> {
            helper.assertTrue(table.items().getStackInSlot(4).isEmpty(), "the hopper should be used up");
            // the hopper's recipe: iron at the corners of the top row, the sides and the bottom middle, a chest in the middle
            for (int i = 0; i < 9; i++) {
                ItemStack out = table.items().getStackInSlot(WorktableBlockEntity.FIRST_OUTPUT + i);
                boolean iron = i == 0 || i == 2 || i == 3 || i == 5 || i == 7;
                if (iron) {
                    helper.assertTrue(out.is(Items.IRON_INGOT) && out.getCount() == 1, "output " + i + " is " + out);
                } else if (i == 4) {
                    helper.assertTrue(out.is(Items.CHEST), "output 4 is " + out);
                } else {
                    helper.assertTrue(out.isEmpty(), "output " + i + " is " + out);
                }
            }
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aWorktableWontTakeApartADamagedOrEnchantedItem(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        ItemStack worn = new ItemStack(Items.IRON_PICKAXE);
        worn.setDamageValue(10);
        lay(table, 4, worn);
        helper.assertFalse(table.canUncraft(), "a damaged pickaxe can be taken apart");
        ItemStack fresh = new ItemStack(Items.IRON_PICKAXE);
        lay(table, 4, fresh);
        helper.assertTrue(table.canUncraft(), "a whole pickaxe can't be taken apart");
        lay(table, 0, new ItemStack(Items.STICK));
        helper.assertFalse(table.canUncraft(), "an item with company in the grid should be crafted, not taken apart");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aPatternInAWorktableLimitsWhatGoesWhere(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        table.items().setStackInSlot(WorktableBlockEntity.PATTERN, pattern(helper, 0, Items.OAK_PLANKS, 3, Items.OAK_PLANKS));
        IItemHandler hand = table.automation();
        helper.assertTrue(hand.insertItem(0, new ItemStack(Items.OAK_PLANKS, 10), false).isEmpty(), "planks should go in slot 0");
        helper.assertFalse(hand.insertItem(1, new ItemStack(Items.OAK_PLANKS, 10), false).isEmpty(), "planks should not go in slot 1");
        helper.assertFalse(hand.insertItem(3, new ItemStack(Items.DIRT, 10), false).isEmpty(), "dirt should not go in slot 3");
        helper.assertTrue(hand.insertItem(3, new ItemStack(Items.OAK_PLANKS, 10), false).isEmpty(), "planks should go in slot 3");
        helper.assertTrue(hand.extractItem(0, 5, false).isEmpty(), "automation took from the grid");
        table.items().setStackInSlot(WorktableBlockEntity.FIRST_OUTPUT, new ItemStack(Items.STICK, 4));
        helper.assertTrue(hand.extractItem(WorktableBlockEntity.FIRST_OUTPUT, 4, false).getCount() == 4, "automation should take from the outputs");
        helper.assertTrue(hand.insertItem(WorktableBlockEntity.FIRST_OUTPUT, new ItemStack(Items.STICK), false).getCount() == 1, "nothing goes into the outputs");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aPatternWithAnInputLimitKeepsAWorktableFromFillingUp(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        ItemStack stack = sticks(helper);
        stack.set(RotaryComponents.CRAFT_PATTERN.get(), CraftPattern.of(stack).withLimit(5));
        table.items().setStackInSlot(WorktableBlockEntity.PATTERN, stack);
        ItemStack rest = table.automation().insertItem(0, new ItemStack(Items.OAK_PLANKS, 20), false);
        helper.assertTrue(rest.getCount() == 15 && table.items().getStackInSlot(0).getCount() == 5, "left " + rest.getCount() + ", held " + table.items().getStackInSlot(0).getCount());
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void clickingTheMiddleOutputCraftsIntoYourHandAndShiftCraftsAll(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        lay(table, 0, new ItemStack(Items.OAK_PLANKS, 3));
        lay(table, 3, new ItemStack(Items.OAK_PLANKS, 3));
        WorktableMenu menu = new WorktableMenu(1, player.getInventory(), table);
        menu.refreshGhost(helper.getLevel());
        helper.assertTrue(menu.ghost().is(Items.STICK) && menu.ready(), "the screen should show sticks ready, ghost " + menu.ghost());
        menu.clicked(WorktableBlockEntity.MAIN_OUTPUT, 0, ClickType.PICKUP, player);
        helper.assertTrue(menu.getCarried().is(Items.STICK) && menu.getCarried().getCount() == 4, "carried " + menu.getCarried());
        menu.setCarried(ItemStack.EMPTY);
        menu.clicked(WorktableBlockEntity.MAIN_OUTPUT, 0, ClickType.QUICK_MOVE, player);
        int sticks = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (player.getInventory().getItem(i).is(Items.STICK)) {
                sticks += player.getInventory().getItem(i).getCount();
            }
        }
        helper.assertTrue(sticks == 8, "shift-click should craft the two batches left: " + sticks);
        helper.assertTrue(table.items().getStackInSlot(0).isEmpty() && table.items().getStackInSlot(3).isEmpty(), "the planks should be used up");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void aWorktableWithTheRedstoneUpgradeCraftsByItself(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        helper.assertFalse(table.hasRedstoneUpgrade(), "it should start without the upgrade");
        table.addRedstoneUpgrade();
        lay(table, 0, new ItemStack(Items.OAK_PLANKS, 2));
        lay(table, 3, new ItemStack(Items.OAK_PLANKS, 2));
        helper.succeedWhen(() -> {
            ItemStack out = table.items().getStackInSlot(WorktableBlockEntity.MAIN_OUTPUT);
            helper.assertTrue(out.is(Items.STICK) && out.getCount() == 8, "output " + out);
        });
    }

    // ---- the auto-crafter ----

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterMakesWhatItsPatternSaysFromTheChestAboveWhenPowered(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(0, sticks(helper));
        chest(helper).setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crafter.hasEnoughPower(), "no power");
            helper.assertTrue(crafter.request(0), "the request did nothing");
            ItemStack out = crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET);
            helper.assertTrue(out.is(Items.STICK) && out.getCount() == 4, "output " + out);
            helper.assertTrue(chest(helper).getItem(0).getCount() == 3, "the chest should lose two planks, has " + chest(helper).getItem(0).getCount());
            helper.assertTrue(crafter.flash(0) > 0, "the lamp should be lit");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterWithoutPowerDoesNothing(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 0, 0);
        crafter.items().setStackInSlot(0, sticks(helper));
        chest(helper).setItem(0, new ItemStack(Items.OAK_PLANKS, 5));
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(crafter.request(0), "it crafted with no power");
            helper.assertTrue(crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET).isEmpty() && chest(helper).getItem(0).getCount() == 5, "something changed");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void continuousModeKeepsCraftingUntilTheIngredientsRunOut(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(0, sticks(helper));
        crafter.setMode(AutoCrafterBlockEntity.Mode.CONTINUOUS);
        chest(helper).setItem(0, new ItemStack(Items.OAK_PLANKS, 6));
        helper.succeedWhen(() -> {
            ItemStack out = crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET);
            helper.assertTrue(out.is(Items.STICK) && out.getCount() == 12, "output " + out);
            helper.assertTrue(chest(helper).getItem(0).isEmpty(), "planks left over");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterMakesAMissingIngredientFromAnotherPattern(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(0, sticks(helper));
        crafter.items().setStackInSlot(1, pattern(helper, 0, Items.OAK_LOG));
        chest(helper).setItem(0, new ItemStack(Items.OAK_LOG, 1));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crafter.request(0), "it did not make the sticks through planks");
            ItemStack out = crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET);
            helper.assertTrue(out.is(Items.STICK) && out.getCount() == 4, "sticks " + out);
            ItemStack planks = crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 1);
            helper.assertTrue(planks.is(Items.OAK_PLANKS) && planks.getCount() == 2, "the spare planks should stay in the planks pattern's output, got " + planks);
            helper.assertTrue(chest(helper).getItem(0).isEmpty(), "the log should be used");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterUsesPlanksAlreadyMadeBeforeMakingMore(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(0, sticks(helper));
        crafter.items().setStackInSlot(1, pattern(helper, 0, Items.OAK_LOG));
        crafter.items().setStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 1, new ItemStack(Items.OAK_PLANKS, 2));
        chest(helper).setItem(0, new ItemStack(Items.OAK_LOG, 1));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crafter.request(0), "no sticks");
            helper.assertTrue(chest(helper).getItem(0).getCount() == 1, "it made planks it already had");
            helper.assertTrue(crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 1).isEmpty(), "the planks should be used up");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterLosesNothingWhenAnIngredientCannotBeMade(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        // a lever: a stick over cobblestone, which nothing here can make
        crafter.items().setStackInSlot(0, pattern(helper, 0, Items.STICK, 3, Items.COBBLESTONE));
        crafter.items().setStackInSlot(1, sticks(helper));
        crafter.items().setStackInSlot(2, pattern(helper, 0, Items.OAK_LOG));
        chest(helper).setItem(0, new ItemStack(Items.OAK_LOG, 1));
        helper.runAfterDelay(5, () -> {
            helper.assertFalse(crafter.request(0), "it made a lever from nothing");
            helper.assertTrue(crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET).isEmpty(), "a lever came out");
            ItemStack sticks = crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 1);
            helper.assertTrue(sticks.is(Items.STICK) && sticks.getCount() == 4, "the sticks it made on the way should still be there, got " + sticks);
            ItemStack planks = crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 2);
            helper.assertTrue(planks.is(Items.OAK_PLANKS) && planks.getCount() == 2, "the spare planks should still be there, got " + planks);
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterDoesNotLoopMakingBlocksFromIngotsFromBlocks(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(0, pattern(helper, 0, Items.IRON_INGOT, 1, Items.IRON_INGOT, 2, Items.IRON_INGOT, 3, Items.IRON_INGOT, 4, Items.IRON_INGOT,
                5, Items.IRON_INGOT, 6, Items.IRON_INGOT, 7, Items.IRON_INGOT, 8, Items.IRON_INGOT));
        crafter.items().setStackInSlot(1, pattern(helper, 0, Items.IRON_BLOCK));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crafter.items().getStackInSlot(0).is(CraftingRegistry.CRAFT_PATTERN.get()), "the pattern is not in");
            helper.assertFalse(crafter.request(0), "it made iron from nothing");
            helper.assertFalse(crafter.request(1), "it made ingots from nothing");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCraftersReturnedContainerMustBeTakenBeforeTheNextCraft(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(0, pattern(helper, 0, Items.HONEY_BOTTLE));
        chest(helper).setItem(0, new ItemStack(Items.HONEY_BOTTLE, 2));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(crafter.request(0), "no sugar");
            helper.assertTrue(crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET).is(Items.SUGAR), "no sugar in the output");
            helper.assertTrue(crafter.items().getStackInSlot(AutoCrafterBlockEntity.CONTAINER_OFFSET).is(Items.GLASS_BOTTLE), "the bottle should be kept");
            helper.assertFalse(crafter.request(0), "it crafted again with the bottle still in");
            helper.assertTrue(crafter.automation().extractItem(AutoCrafterBlockEntity.CONTAINER_OFFSET, 1, false).is(Items.GLASS_BOTTLE), "automation should take the bottle");
            helper.assertTrue(crafter.request(0), "it should craft again once the bottle is out");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterTakesOnlyUsablePatternsAndGivesOnlyWhatItMade(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 0, 0);
        IItemHandler hand = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, helper.absolutePos(TABLE), null);
        helper.assertTrue(hand != null, "no item handler");
        helper.assertFalse(hand.insertItem(0, new ItemStack(Items.DIRT), false).isEmpty(), "dirt was taken");
        helper.assertFalse(hand.insertItem(0, new ItemStack(CraftingRegistry.CRAFT_PATTERN.get()), false).isEmpty(), "a blank pattern was taken");
        ItemStack wrongKind = sticks(helper);
        wrongKind.set(RotaryComponents.CRAFT_PATTERN.get(), CraftPattern.of(wrongKind).withMode(PatternMode.BLAST_FURNACE));
        helper.assertFalse(hand.insertItem(0, wrongKind, false).isEmpty(), "a blast furnace pattern was taken");
        helper.assertTrue(hand.insertItem(0, sticks(helper), false).isEmpty(), "a good pattern was refused");
        helper.assertFalse(hand.insertItem(AutoCrafterBlockEntity.OUTPUT_OFFSET, new ItemStack(Items.STICK), false).isEmpty(), "something was put in an output");
        helper.assertTrue(hand.extractItem(0, 1, false).isEmpty(), "a pattern was taken out");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void theAutoCrafterScreenButtonsCraftAndChangeTheMode(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 64, 64);
        crafter.items().setStackInSlot(4, sticks(helper));
        chest(helper).setItem(0, new ItemStack(Items.OAK_PLANKS, 2));
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        AutoCrafterMenu menu = new AutoCrafterMenu(1, player.getInventory(), crafter);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(menu.clickMenuButton(player, 4), "button 4");
            helper.assertTrue(crafter.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 4).getCount() == 4, "no sticks from the button");
            helper.assertTrue(menu.mode() == AutoCrafterBlockEntity.Mode.REQUEST, "starts in request mode");
            menu.clickMenuButton(player, AutoCrafterMenu.MODE);
            helper.assertTrue(crafter.mode() == AutoCrafterBlockEntity.Mode.CONTINUOUS, "mode " + crafter.mode());
            helper.assertFalse(menu.clickMenuButton(player, 4), "the request bars do nothing in continuous mode");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void anAutoCrafterKeepsItsPatternsAndModeWhenSavedAndLoaded(GameTestHelper helper) {
        AutoCrafterBlockEntity crafter = crafter(helper, 0, 0);
        crafter.items().setStackInSlot(2, sticks(helper));
        crafter.items().setStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 2, new ItemStack(Items.STICK, 7));
        crafter.setMode(AutoCrafterBlockEntity.Mode.CONTINUOUS);
        var registries = helper.getLevel().registryAccess();
        var saved = crafter.saveWithFullMetadata(registries);
        helper.setBlock(TABLE, Blocks.AIR);
        helper.setBlock(TABLE, CraftingRegistry.AUTO_CRAFTER.get().defaultBlockState());
        AutoCrafterBlockEntity other = helper.getBlockEntity(TABLE);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.mode() == AutoCrafterBlockEntity.Mode.CONTINUOUS, "mode lost");
        helper.assertTrue(CraftPattern.of(other.items().getStackInSlot(2)).output().is(Items.STICK), "pattern lost");
        helper.assertTrue(other.items().getStackInSlot(AutoCrafterBlockEntity.OUTPUT_OFFSET + 2).getCount() == 7, "output lost");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 30)
    public static void aWorktableKeepsItsItemsAndUpgradeWhenSavedAndLoaded(GameTestHelper helper) {
        WorktableBlockEntity table = table(helper);
        table.addRedstoneUpgrade();
        lay(table, 5, new ItemStack(Items.COAL, 9));
        table.items().setStackInSlot(WorktableBlockEntity.PATTERN, sticks(helper));
        var registries = helper.getLevel().registryAccess();
        var saved = table.saveWithFullMetadata(registries);
        helper.setBlock(TABLE, Blocks.AIR);
        WorktableBlockEntity other = table(helper);
        other.loadWithComponents(saved, registries);
        helper.assertTrue(other.hasRedstoneUpgrade(), "the upgrade was lost");
        helper.assertTrue(other.items().getStackInSlot(5).getCount() == 9, "items lost");
        helper.assertTrue(CraftPattern.of(other.items().getStackInSlot(WorktableBlockEntity.PATTERN)).hasRecipe(), "pattern lost");
        helper.succeed();
    }
}
