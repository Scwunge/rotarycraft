package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.Pig;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.AerosolizerBlockEntity;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/** The Aerosolizer, which empties potions into a store and spreads their effect over everything in the room. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class AerosolizerGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final BlockPos MACHINE = new BlockPos(2, 2, 2);

    static ItemStack potion(net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> potion) {
        return PotionContents.createItemStack(Items.POTION, potion);
    }

    /** A pig on a block of stone beside the aerosolizer, inside the space it fills (the test has no floor, so a pig would fall out of it). */
    static Pig pigAt(GameTestHelper helper, int x, int z) {
        helper.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        return helper.spawnWithNoFreeWill(EntityType.PIG, new Vec3(x + 0.5, 2, z + 0.5));
    }

    /** 128 * 128 = 16 kW, just what it needs, from the flywheel under it (it takes power from any side). */
    static AerosolizerBlockEntity aerosolizer(GameTestHelper helper, boolean powered) {
        if (powered) {
            WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 128, 128);
        }
        helper.setBlock(MACHINE, DecorRegistry.AEROSOLIZER.block().get().defaultBlockState());
        return helper.getBlockEntity(MACHINE);
    }

    @GameTest(template = SMALL, batch = "aero_store", timeoutTicks = 40)
    public static void aerosolizerEmptiesPotionsIntoItsStoreAndLeavesBottles(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, false);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        aero.items().setStackInSlot(1, potion(Potions.LONG_SWIFTNESS));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(aero.items().getStackInSlot(0).is(Items.GLASS_BOTTLE) && aero.items().getStackInSlot(1).is(Items.GLASS_BOTTLE), "the potions were not emptied");
            helper.assertTrue(aero.level(0) == 1, "a potion is one unit, not " + aero.level(0));
            helper.assertTrue(aero.level(1) == 3, "an extended potion is three units, not " + aero.level(1));
            helper.assertTrue(aero.automationItems().extractItem(0, 1, false).is(Items.GLASS_BOTTLE), "the bottle should come out");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_instant", timeoutTicks = 40)
    public static void aerosolizerKeepsInstantAndPlainPotionsOut(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, false);
        aero.items().setStackInSlot(0, potion(Potions.HEALING));
        aero.items().setStackInSlot(1, potion(Potions.WATER));
        aero.items().setStackInSlot(2, potion(Potions.SWIFTNESS));
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(aero.items().getStackInSlot(0).is(Items.POTION) && aero.level(0) == 0, "an instant potion was taken");
            helper.assertTrue(aero.items().getStackInSlot(1).is(Items.POTION) && aero.level(1) == 0, "water was taken");
            helper.assertFalse(aero.automationItems().extractItem(0, 1, true).is(Items.POTION), "a potion was taken out by a pipe");
            helper.assertFalse(aero.automationItems().isItemValid(0, new ItemStack(Items.STICK)), "only potions go in");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_mix", timeoutTicks = 40)
    public static void aSlotHoldsOnlyOneKindOfPotion(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, false);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        helper.runAfterDelay(3, () -> aero.items().setStackInSlot(0, potion(Potions.STRENGTH)));
        helper.runAfterDelay(8, () -> {
            helper.assertTrue(aero.items().getStackInSlot(0).is(Items.POTION), "a different potion was mixed in");
            helper.assertTrue(aero.level(0) == 1, "level " + aero.level(0));
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_effect", timeoutTicks = 100)
    public static void aerosolizerGivesItsEffectToCreaturesInTheRoom(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, true);
        Pig pig = pigAt(helper, 3, 3);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        helper.succeedWhen(() -> {
            MobEffectInstance effect = pig.getEffect(MobEffects.MOVEMENT_SPEED);
            helper.assertTrue(effect != null, "the pig has no speed effect");
            helper.assertTrue(effect.getAmplifier() == 0, "amplifier " + effect.getAmplifier());
        });
    }

    @GameTest(template = SMALL, batch = "aero_nopower", timeoutTicks = 60)
    public static void aerosolizerNeedsSixteenKilowatts(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, false);
        WeaponGameTests.spinningFlywheel(helper, MACHINE.below(), 128, 127);
        Pig pig = pigAt(helper, 3, 3);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(aero.level(0) == 1, "it should still have stored the potion");
            helper.assertTrue(pig.getEffect(MobEffects.MOVEMENT_SPEED) == null, "it worked on too little power");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_side", timeoutTicks = 100)
    public static void aerosolizerTakesPowerFromAnySide(GameTestHelper helper) {
        helper.setBlock(MACHINE, DecorRegistry.AEROSOLIZER.block().get().defaultBlockState());
        AerosolizerBlockEntity aero = helper.getBlockEntity(MACHINE);
        WeaponGameTests.spinningFlywheel(helper, MACHINE.above(), 64, 128, Direction.DOWN);
        WeaponGameTests.spinningFlywheel(helper, MACHINE.east(), 64, 128, Direction.WEST);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(aero.hasEnoughPower(), "two shafts of 8 kW each should add up to 16 kW (" + aero.getTorque() + " N*m, " + aero.getOmega() + " rad/s)");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_strength", timeoutTicks = 100)
    public static void severalSlotsOfThePotionStrengthenIt(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, true);
        Pig pig = pigAt(helper, 3, 3);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        aero.items().setStackInSlot(1, potion(Potions.SWIFTNESS));
        helper.succeedWhen(() -> {
            MobEffectInstance effect = pig.getEffect(MobEffects.MOVEMENT_SPEED);
            helper.assertTrue(effect != null, "the pig has no speed effect");
            helper.assertTrue(aero.multiplier(0) == 2, "multiplier " + aero.multiplier(0));
            helper.assertTrue(effect.getAmplifier() == 1, "two slots should give speed II, not " + effect.getAmplifier());
        });
    }

    @GameTest(template = SMALL, batch = "aero_wall", timeoutTicks = 100)
    public static void aerosolizerOnlyFillsTheSpaceItIsIn(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, true);
        // a wall one block out to the east, and the pig behind it
        for (int y = 1; y <= 3; y++) {
            for (int z = 0; z <= 4; z++) {
                helper.setBlock(new BlockPos(3, y, z), Blocks.STONE);
            }
        }
        Pig pig = pigAt(helper, 4, 3);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        helper.runAfterDelay(50, () -> {
            helper.assertTrue(aero.level(0) == 1, "not stored");
            helper.assertTrue(pig.getEffect(MobEffects.MOVEMENT_SPEED) == null, "the effect went through a wall");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_drain", timeoutTicks = 60)
    public static void aStoredUnitLastsTwoMinutes(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, true);
        aero.items().setStackInSlot(0, potion(Potions.LONG_SWIFTNESS));
        helper.runAfterDelay(5, () -> {
            HolderLookup.Provider registries = helper.getLevel().registryAccess();
            CompoundTag tag = aero.saveWithoutMetadata(registries);
            tag.putInt("drain", AerosolizerBlockEntity.DRAIN_TICKS - 5);
            aero.loadCustomOnly(tag, registries);
            helper.assertTrue(aero.level(0) == 3, "level " + aero.level(0));
        });
        helper.runAfterDelay(20, () -> {
            helper.assertTrue(aero.level(0) == 2, "it should have used up a unit, level " + aero.level(0));
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "aero_comparator", timeoutTicks = 40)
    public static void aerosolizerComparatorReadsHowFullItIs(GameTestHelper helper) {
        AerosolizerBlockEntity aero = aerosolizer(helper, false);
        helper.assertTrue(aero.comparatorSignal() == 0, "empty should read 0");
        HolderLookup.Provider registries = helper.getLevel().registryAccess();
        CompoundTag tag = aero.saveWithoutMetadata(registries);
        ListTag brews = new ListTag();
        for (int i = 0; i < AerosolizerBlockEntity.SLOTS; i++) {
            CompoundTag one = new CompoundTag();
            one.putInt("level", AerosolizerBlockEntity.CAPACITY);
            one.putInt("color", 0xFF00FF);
            one.putInt("strength", 1);
            ListTag effects = new ListTag();
            CompoundTag effect = new CompoundTag();
            effect.putString("id", "minecraft:speed");
            effect.putInt("amp", 0);
            effects.add(effect);
            one.put("effects", effects);
            brews.add(one);
        }
        tag.put("brews", brews);
        aero.loadCustomOnly(tag, registries);
        helper.assertTrue(aero.comparatorSignal() == 15, "full should read 15, not " + aero.comparatorSignal());
        helper.assertTrue(helper.getLevel().getBlockState(helper.absolutePos(MACHINE)).getAnalogOutputSignal(helper.getLevel(), helper.absolutePos(MACHINE)) == 15,
                "the block should hand it to comparators");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "aero_switch", timeoutTicks = 60)
    public static void aerosolizerSwitchedOffDoesNothing(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("aerosolizer");
        AerosolizerBlockEntity aero = aerosolizer(helper, true);
        Pig pig = pigAt(helper, 3, 3);
        aero.items().setStackInSlot(0, potion(Potions.SWIFTNESS));
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(pig.getEffect(MobEffects.MOVEMENT_SPEED) == null, "a switched-off aerosolizer worked");
            helper.succeed();
        });
    }
}
