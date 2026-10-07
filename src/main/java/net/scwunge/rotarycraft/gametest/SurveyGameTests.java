package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import net.scwunge.rotarycraft.survey.GprBlockEntity;
import net.scwunge.rotarycraft.survey.MobRadarBlockEntity;

/** Tests of the survey machines (Mob Radar, GPR, Cave Scanner, CCTV, Spy Cam). */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SurveyGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    static MobRadarBlockEntity radar(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), torque, omega);
        helper.setBlock(MACHINE, SurveyRegistry.MOB_RADAR.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    /** 8 blocks at the minimum power, one more for each 1024 W above it, to 256. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void mobRadarRangeFollowsPower(GameTestHelper helper) {
        var radar = radar(helper, 64, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(radar.range() == 8, "range " + radar.range() + " on 8 kW");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 128, 128);
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(radar.range() == 8 + (16384 - 8192) / 1024, "range " + radar.range() + " on 16 kW");
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 4096, 256);
        });
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(radar.range() == 256, "range " + radar.range() + " not capped at 256");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void mobRadarNeedsItsMinimumPower(GameTestHelper helper) {
        var radar = radar(helper, 32, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(radar.range() == 0 && radar.scan().blips().isEmpty(), "saw something on 4 kW");
            helper.succeed();
        });
    }

    /** A cow is on the map, in the right place, with the cow's face. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void mobRadarShowsCreaturesWhereTheyAre(GameTestHelper helper) {
        var radar = radar(helper, 64, 128);
        helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(2, 2, 4));
        helper.runAfterDelay(10, () -> {
            var scan = radar.scan();
            helper.assertTrue(scan.blips().stream().anyMatch(b -> b.icon() == 92 && b.dz() > 0 && Math.abs(b.dx()) <= 20), "no cow south of the radar: " + scan.blips());
            helper.succeed();
        });
    }

    static GprBlockEntity gpr(GameTestHelper helper, int torque, int omega) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), torque, omega);
        helper.setBlock(MACHINE, SurveyRegistry.GPR.get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    /** 2 * log2 of the power above the 32 kW minimum, either side of the middle. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gprRadiusFollowsPower(GameTestHelper helper) {
        var gpr = gpr(helper, 512, 128);
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(gpr.range() == 30, "range " + gpr.range() + " on 64 kW, not 30");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void gprNeedsItsMinimumPower(GameTestHelper helper) {
        var gpr = gpr(helper, 256, 64);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(gpr.scannedRange() < 0, "scanned on 16 kW");
            helper.succeed();
        });
    }

    /** What is below it shows up in the slice, at the depth it is at. */
    @GameTest(template = TEMPLATE, timeoutTicks = 100)
    public static void gprScansTheGroundBelow(GameTestHelper helper) {
        var gpr = gpr(helper, 512, 128);
        helper.setBlock(new BlockPos(2, 0, 2), net.minecraft.world.level.block.Blocks.GOLD_BLOCK);
        helper.succeedWhen(() -> {
            helper.assertTrue(gpr.scannedRange() == 30, "not scanned yet");
            byte[] columns = gpr.columns(30);
            int goldColor = net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultMapColor().col;
            int dd = 2;
            int middle = 30;
            helper.assertTrue(gpr.palette().get(columns[middle * GprBlockEntity.MAX_HEIGHT + (dd - 1)] & 255) == goldColor, "no gold two blocks down");
        });
    }

    /** The plane moves along the way the screen looks, and back. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void gprPlaneShiftsAndResets(GameTestHelper helper) {
        var gpr = gpr(helper, 512, 128);
        gpr.setDirection(true);
        helper.assertTrue(gpr.guiDirection() == net.minecraft.core.Direction.SOUTH, "a plane along x looks south");
        gpr.shift(1);
        gpr.shift(1);
        helper.assertTrue(gpr.centre().equals(helper.absolutePos(MACHINE).offset(0, 0, 2)), "not shifted two south: " + gpr.centre());
        gpr.shift(0);
        helper.assertTrue(gpr.centre().equals(helper.absolutePos(MACHINE)), "not back");
        gpr.flipDirection();
        helper.assertTrue(gpr.guiDirection() == net.minecraft.core.Direction.EAST, "a plane along z looks east");
        helper.succeed();
    }

    /** On from the minimum power; the aim moves four blocks the way asked, and starts on the machine. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void caveScannerIsOnWithPowerAndAimsWhereMoved(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 1024, 256);
        helper.setBlock(MACHINE, SurveyRegistry.CAVE_SCANNER.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.CaveScannerBlockEntity scanner = helper.getBlockEntity(MACHINE);
        helper.assertTrue(scanner.source().equals(helper.absolutePos(MACHINE)), "not aimed at itself at first");
        scanner.moveSource(4, net.minecraft.core.Direction.EAST);
        scanner.moveSource(-4, net.minecraft.core.Direction.UP);
        helper.assertTrue(scanner.source().equals(helper.absolutePos(MACHINE).offset(4, -4, 0)), "aim " + scanner.source());
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(scanner.isOn(), "off with 262 kW");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void caveScannerStaysOffBelowItsMinimum(GameTestHelper helper) {
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 256, 256);
        helper.setBlock(MACHINE, SurveyRegistry.CAVE_SCANNER.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.CaveScannerBlockEntity scanner = helper.getBlockEntity(MACHINE);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(scanner.isOn(), "on with 65 kW");
            helper.succeed();
        });
    }

    static net.scwunge.rotarycraft.survey.CctvBlockEntity cctv(GameTestHelper helper, BlockPos at, net.minecraft.world.item.DyeColor a, net.minecraft.world.item.DyeColor b,
                                                                net.minecraft.world.item.DyeColor c) {
        helper.setBlock(at, SurveyRegistry.CCTV.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.CctvBlockEntity cam = helper.getBlockEntity(at);
        net.minecraft.world.item.ItemStack coil = new net.minecraft.world.item.ItemStack(net.scwunge.rotarycraft.registry.WeaponRegistry.SPRING.get());
        net.scwunge.rotarycraft.item.CoilItem.setCharge(coil, 100);
        cam.items().setStackInSlot(0, coil);
        cam.items().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.DyeItem.byColor(a)));
        cam.items().setStackInSlot(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.DyeItem.byColor(b)));
        cam.items().setStackInSlot(3, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.DyeItem.byColor(c)));
        return cam;
    }

    /** A camera runs while it has a wound coil, and the coil winds down. */
    @GameTest(template = TEMPLATE, timeoutTicks = 400)
    public static void cctvRunsOnItsCoilAndUnwinds(GameTestHelper helper) {
        var cam = cctv(helper, MACHINE, net.minecraft.world.item.DyeColor.RED, net.minecraft.world.item.DyeColor.BLUE, net.minecraft.world.item.DyeColor.GREEN);
        helper.runAfterDelay(5, () -> helper.assertTrue(cam.isOn(), "off with a wound coil"));
        helper.succeedWhen(() -> helper.assertTrue(net.scwunge.rotarycraft.item.CoilItem.charge(cam.items().getStackInSlot(0)) < 100, "the coil did not unwind"));
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cctvIsOffWithoutACoil(GameTestHelper helper) {
        var cam = cctv(helper, MACHINE, net.minecraft.world.item.DyeColor.RED, net.minecraft.world.item.DyeColor.BLUE, net.minecraft.world.item.DyeColor.GREEN);
        cam.items().setStackInSlot(0, net.minecraft.world.item.ItemStack.EMPTY);
        helper.runAfterDelay(10, () -> {
            helper.assertFalse(cam.isOn(), "on without a coil");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void cctvAimWrapsAtTheTiltLimit(GameTestHelper helper) {
        var cam = cctv(helper, MACHINE, net.minecraft.world.item.DyeColor.RED, net.minecraft.world.item.DyeColor.BLUE, net.minecraft.world.item.DyeColor.GREEN);
        cam.aim(false);
        cam.aim(false);
        helper.assertTrue(cam.phi() == 10, "pan " + cam.phi());
        for (int i = 0; i < 12; i++) {
            cam.aim(true);
        }
        helper.assertTrue(cam.theta() == -60, "tilt " + cam.theta());
        cam.aim(true);
        helper.assertTrue(cam.theta() == 60, "tilt did not wrap: " + cam.theta());
        helper.succeed();
    }

    /** A powered screen with the same three dyes in the same order finds the camera, and one with a different order does not. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void screenFindsTheCameraWithTheSameDyes(GameTestHelper helper) {
        var cam = cctv(helper, new BlockPos(0, 2, 0), net.minecraft.world.item.DyeColor.RED, net.minecraft.world.item.DyeColor.BLUE, net.minecraft.world.item.DyeColor.GREEN);
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 64, 64);
        helper.setBlock(MACHINE, SurveyRegistry.SCREEN.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.ScreenBlockEntity screen = helper.getBlockEntity(MACHINE);
        screen.items().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE));
        screen.items().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_DYE));
        screen.items().setStackInSlot(2, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.GREEN_DYE));
        helper.runAfterDelay(10, () -> {
            helper.assertTrue(screen.findCamera() != null && java.util.Arrays.equals(screen.findCamera().colors(), cam.colors()), "camera not found");
            screen.items().setStackInSlot(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BLUE_DYE));
            screen.items().setStackInSlot(1, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE));
            helper.assertTrue(screen.findCamera() == null, "found a camera with the dyes in another order");
            helper.succeed();
        });
    }

    /** The Spy Cam's map has the top block's colour, and a creature standing on the ground. */
    @GameTest(template = TEMPLATE, timeoutTicks = 60)
    public static void spyCamMapsTheGroundAndTheCreatures(GameTestHelper helper) {
        helper.setBlock(MACHINE, SurveyRegistry.SPY_CAM.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.SpyCamBlockEntity cam = helper.getBlockEntity(MACHINE);
        helper.setBlock(new BlockPos(3, 1, 2), net.minecraft.world.level.block.Blocks.GOLD_BLOCK);
        helper.spawnWithNoFreeWill(EntityType.COW, new BlockPos(1, 2, 2));
        helper.runAfterDelay(10, () -> {
            var view = cam.view();
            int side = net.scwunge.rotarycraft.survey.SpyCamBlockEntity.SIDE, range = net.scwunge.rotarycraft.survey.SpyCamBlockEntity.RANGE;
            helper.assertTrue(view.colors()[(range + 1) * side + range] != 0, "no colour for the gold block");
            helper.assertTrue(view.mobs().size() >= 3 && view.mobs().get(2) == 92, "no cow on the map: " + view.mobs());
            helper.succeed();
        });
    }

    static final String WIDE = "empty20x8x7";
    static final BlockPos MIDDLE = new BlockPos(10, 1, 3);

    static net.scwunge.rotarycraft.survey.DisplayBlockEntity display(GameTestHelper helper) {
        helper.setBlock(MIDDLE, SurveyRegistry.DISPLAY.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.DisplayBlockEntity display = helper.getBlockEntity(MIDDLE);
        net.minecraft.world.item.ItemStack coil = new net.minecraft.world.item.ItemStack(net.scwunge.rotarycraft.registry.WeaponRegistry.SPRING.get());
        net.scwunge.rotarycraft.item.CoilItem.setCharge(coil, 100);
        display.items().setStackInSlot(0, coil);
        return display;
    }

    /** It lights with a wound coil and clear space above it; a block in the board's way puts it out. */
    @GameTest(template = WIDE, timeoutTicks = 80)
    public static void displayShowsOnlyWithACoilAndRoom(GameTestHelper helper) {
        var display = display(helper);
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(display.isShowing(), "dark with a coil and room (lit " + display.isLit() + ", clear " + display.hasClearBoard() + ", in the way " + display.obstruction() + ")");
            helper.setBlock(new BlockPos(10, 3, 3), net.minecraft.world.level.block.Blocks.STONE);
        });
        helper.runAfterDelay(35, () -> {
            helper.assertFalse(display.isShowing(), "lit with a block in the way");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, timeoutTicks = 20)
    public static void displayColoursAndMessages(GameTestHelper helper) {
        var display = display(helper);
        helper.assertTrue(display.dye() == null && display.fill() == 0x0080FF, "not argon blue to start");
        display.setDye(net.minecraft.world.item.DyeColor.RED);
        helper.assertTrue(display.dye() == net.minecraft.world.item.DyeColor.RED, "dye not set");
        display.setMessage("Hello");
        helper.assertTrue(display.message().equals("Hello"), "message not set");
        var book = new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WRITTEN_BOOK);
        book.set(net.minecraft.core.component.DataComponents.WRITTEN_BOOK_CONTENT, new net.minecraft.world.item.component.WrittenBookContent(
                net.minecraft.server.network.Filterable.passThrough("Title"), "Author", 0, java.util.List.of(
                        net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("One")),
                        net.minecraft.server.network.Filterable.passThrough(net.minecraft.network.chat.Component.literal("Two"))), true));
        helper.assertTrue(net.scwunge.rotarycraft.survey.DisplayBlock.pagesOf(book).equals(java.util.List.of("One", "Two")), "book pages not read");
        helper.assertTrue(net.scwunge.rotarycraft.survey.DisplayBlock.pagesOf(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)) == null,
                "a stick is not a book");
        helper.succeed();
    }

    static net.scwunge.rotarycraft.survey.ProjectorBlockEntity projector(GameTestHelper helper, int slide, boolean wall) {
        // power comes in from behind (the south), the picture goes out to the north
        BlockPos at = new BlockPos(10, 2, 5);
        WeaponGameTests.spinningFlywheel(helper, at.south(), 64, 64, net.minecraft.core.Direction.NORTH);
        helper.setBlock(at, SurveyRegistry.PROJECTOR.get().defaultBlockState());
        net.scwunge.rotarycraft.survey.ProjectorBlockEntity projector = helper.getBlockEntity(at);
        projector.items().setStackInSlot(0, new net.minecraft.world.item.ItemStack(SurveyRegistry.SLIDES.get(slide).get()));
        if (wall) {
            for (int x = 7; x <= 13; x++) {
                for (int y = 2; y <= 6; y++) {
                    helper.setBlock(new BlockPos(x, y, 2), net.minecraft.world.level.block.Blocks.STONE);
                }
            }
        }
        return projector;
    }

    /** With power and a big enough wall in front, it shows the slide in its first slot, and says how far the wall is. */
    @GameTest(template = WIDE, timeoutTicks = 80)
    public static void projectorShowsTheFirstSlideOnAWall(GameTestHelper helper) {
        var projector = projector(helper, 7, true);
        helper.succeedWhen(() -> {
            helper.assertTrue(projector.isShowing(), "not showing (distance " + projector.distance() + ", slide " + projector.slide() + ", power " + projector.hasEnoughPower() + ")");
            helper.assertTrue(projector.distance() == 3, "wall at " + projector.distance());
            helper.assertTrue(projector.slide() == 7, "slide " + projector.slide());
        });
    }

    @GameTest(template = WIDE, timeoutTicks = 40)
    public static void projectorNeedsAWall(GameTestHelper helper) {
        var projector = projector(helper, 7, false);
        helper.runAfterDelay(20, () -> {
            helper.assertFalse(projector.isShowing(), "projecting into the air");
            helper.succeed();
        });
    }

    @GameTest(template = WIDE, timeoutTicks = 20)
    public static void projectorMovesOnToTheNextSlide(GameTestHelper helper) {
        var projector = projector(helper, 1, false);
        projector.items().setStackInSlot(1, new net.minecraft.world.item.ItemStack(SurveyRegistry.SLIDES.get(2).get()));
        projector.cycle();
        helper.assertTrue(((net.scwunge.rotarycraft.survey.SlideItem) projector.items().getStackInSlot(0).getItem()).index() == 2, "next slide not in front");
        helper.assertTrue(((net.scwunge.rotarycraft.survey.SlideItem) projector.items().getStackInSlot(23).getItem()).index() == 1, "old slide not at the back");
        helper.succeed();
    }

    /** A dye moves a slide on by the dye's step. */
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void aDyeChangesASlide(GameTestHelper helper) {
        var recipe = new net.scwunge.rotarycraft.survey.SlideDyeRecipe(net.minecraft.world.item.crafting.CraftingBookCategory.MISC);
        var input = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(new net.minecraft.world.item.ItemStack(SurveyRegistry.SLIDES.get(3).get()),
                new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.RED_DYE)));
        helper.assertTrue(recipe.matches(input, helper.getLevel()), "recipe does not match a dye and a slide");
        var out = recipe.assemble(input, helper.getLevel().registryAccess());
        helper.assertTrue(((net.scwunge.rotarycraft.survey.SlideItem) out.getItem()).index() == 7, "slide 3 and red should give slide 7, not " + out);
        var two = net.minecraft.world.item.crafting.CraftingInput.of(2, 1, java.util.List.of(new net.minecraft.world.item.ItemStack(SurveyRegistry.SLIDES.get(3).get()),
                new net.minecraft.world.item.ItemStack(SurveyRegistry.SLIDES.get(4).get())));
        helper.assertFalse(recipe.matches(two, helper.getLevel()), "two slides matched");
        helper.succeed();
    }
}
