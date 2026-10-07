package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.api.SodiumSolarUpgrades;
import net.scwunge.rotarycraft.registry.SolarRegistry;
import net.scwunge.rotarycraft.solar.SolarMirrorBlockEntity;
import net.scwunge.rotarycraft.solar.SolarPlant;
import net.scwunge.rotarycraft.solar.SolarTowerBlockEntity;

@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SolarGameTests {
    static final String ROOM = "empty20x8x7";
    /** The bottom block of the tower; it is three blocks tall, with a row of six mirrors joined to its foot. */
    static final BlockPos FOOT = new BlockPos(2, 1, 3);
    static final int MIRRORS = 6;

    /** A three block tower with six mirrors in a row against its foot, at noon in clear weather. */
    static SolarTowerBlockEntity plant(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        helper.getLevel().setWeatherParameters(100000, 0, false, false);
        for (int i = 0; i < 3; i++) {
            helper.setBlock(FOOT.above(i), SolarRegistry.SOLAR_TOWER.get().defaultBlockState());
        }
        for (int i = 1; i <= MIRRORS; i++) {
            helper.setBlock(FOOT.east(i), SolarRegistry.SOLAR_MIRROR.get().defaultBlockState());
        }
        return helper.getBlockEntity(FOOT);
    }

    static void fillAll(GameTestHelper helper) {
        for (int i = 0; i < 3; i++) {
            SolarTowerBlockEntity t = helper.getBlockEntity(FOOT.above(i));
            t.tank().fill(new FluidStack(Fluids.WATER, SolarTowerBlockEntity.TANK), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    @GameTest(template = ROOM, batch = "solar_plant", timeoutTicks = 60)
    public static void aPlantFindsItsTowerAndMirrors(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        helper.runAfterDelay(5, () -> {
            SolarPlant plant = foot.plant();
            helper.assertTrue(plant != null, "the tower found no plant");
            helper.assertTrue(plant.towerCount() == 1, "towers " + plant.towerCount());
            helper.assertTrue(plant.mirrorCount() == MIRRORS, "mirrors " + plant.mirrorCount());
            helper.assertTrue(plant.towerMultiplier() == 3, "tower multiplier " + plant.towerMultiplier());
            helper.assertTrue(foot.topOfTower() == helper.absolutePos(FOOT.above(2)).getY(), "top of the tower " + foot.topOfTower());
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_noon", timeoutTicks = 100)
    public static void aTowerMakesPowerFromWaterAndLight(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        fillAll(helper);
        helper.succeedWhen(() -> {
            SolarTowerBlockEntity topBlock = helper.getBlockEntity(FOOT.above(2));
            helper.assertTrue(foot.getOmega() == SolarTowerBlockEntity.GEN_OMEGA, "speed " + foot.getOmega() + ", torque " + foot.getTorque() + ", top size " + topBlock.arraySize() + ", top brightness " + topBlock.brightness() + ", light " + SolarPlant.lightLevel(helper.getLevel()) + ", water " + foot.tank().getFluidAmount() + ", plant " + foot.plant() + ", rain " + helper.getLevel().getRainLevel(1) + ", flow " + foot.currentConsumption());
            // six working mirrors in the light: tower height 3 * (6 + 1) * the brightness
            int expected = (int) (topBlock.brightness() * 3 * 7);
            helper.assertTrue(expected >= 15 && foot.getTorque() == expected, "torque " + foot.getTorque() + ", expected " + expected);
            helper.assertTrue(foot.getTorqueOut(Direction.DOWN) == expected && foot.getOmegaOut(Direction.DOWN) == SolarTowerBlockEntity.GEN_OMEGA, "output below");
            helper.assertTrue(foot.getTorqueOut(Direction.UP) == 0 && foot.getTorqueOut(Direction.NORTH) == 0, "power out of the wrong sides");
            helper.assertTrue(foot.currentConsumption() > 0, "no water used");
            helper.assertTrue(topBlock.getTorque() == 0, "only the foot of the tower should make power");
        });
    }

    @GameTest(template = ROOM, batch = "solar_dry", timeoutTicks = 60)
    public static void aTowerWithoutFluidMakesNothing(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(foot.getTorque() == 0 && foot.getOmega() == 0, "power with no water: " + foot.getTorque());
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_nomirrors", timeoutTicks = 60)
    public static void aTowerWithoutMirrorsMakesNothing(GameTestHelper helper) {
        helper.getLevel().setDayTime(6000);
        for (int i = 0; i < 3; i++) {
            helper.setBlock(FOOT.above(i), SolarRegistry.SOLAR_TOWER.get().defaultBlockState());
        }
        SolarTowerBlockEntity foot = helper.getBlockEntity(FOOT);
        fillAll(helper);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(foot.getTorque() == 0, "power with no mirrors: " + foot.getTorque());
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_night", timeoutTicks = 100)
    public static void lessLightMakesLessPower(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        helper.getLevel().setDayTime(18000);
        fillAll(helper);
        helper.runAfterDelay(40, () -> {
            helper.assertTrue(SolarPlant.lightLevel(helper.getLevel()) <= 3, "light at midnight " + SolarPlant.lightLevel(helper.getLevel()));
            helper.assertTrue(foot.getTorque() < 21, "torque at night " + foot.getTorque());
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_broken", timeoutTicks = 100)
    public static void aBrokenMirrorStopsCountingAndANewOneMendsIt(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        fillAll(helper);
        helper.runAfterDelay(10, () -> {
            SolarMirrorBlockEntity mirror = helper.getBlockEntity(FOOT.east(1));
            helper.assertTrue(mirror.isFunctional(), "a fresh mirror should work");
            mirror.breakMirror();
            helper.assertFalse(mirror.isFunctional(), "a broken mirror still works");
            helper.assertTrue(foot.plant().overallBrightness(helper.getLevel()) < 1, "brightness did not fall");
            mirror.repair();
            helper.assertTrue(mirror.isFunctional(), "a mended mirror does not work");
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_shade", timeoutTicks = 60)
    public static void aMirrorUnderAnotherIsShaded(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        helper.setBlock(FOOT.east(1).above(), SolarRegistry.SOLAR_MIRROR.get().defaultBlockState());
        helper.runAfterDelay(10, () -> {
            SolarMirrorBlockEntity lower = helper.getBlockEntity(FOOT.east(1));
            SolarMirrorBlockEntity upper = helper.getBlockEntity(FOOT.east(1).above());
            helper.assertFalse(lower.isFunctional(), "the mirror under another still counts");
            helper.assertTrue(upper.isFunctional(), "the top mirror does not count");
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_split", timeoutTicks = 60)
    public static void breakingABlockMakesTheRestFindTheirPlantAgain(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        helper.runAfterDelay(5, () -> {
            helper.assertTrue(foot.plant().mirrorCount() == MIRRORS, "start with " + foot.plant().mirrorCount());
            helper.setBlock(FOOT.east(MIRRORS), Blocks.AIR);
        });
        helper.runAfterDelay(15, () -> {
            helper.assertTrue(foot.plant() != null && foot.plant().mirrorCount() == MIRRORS - 1, "mirrors after breaking one: " + (foot.plant() == null ? "no plant" : foot.plant().mirrorCount()));
            helper.succeed();
        });
    }

    /** A chest standing in for a sodium receiver on top of the tower (the real ones are ReactorCraft's). */
    static class FakeReceiver extends ChestBlockEntity implements SodiumSolarUpgrades.SodiumSolarReceiver {
        boolean active = true;
        int ticks;
        int lastMirrors;

        FakeReceiver(BlockPos pos, BlockState state) {
            super(pos, state);
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public void tick(int mirrorCount, float totalBrightness) {
            ticks++;
            lastMirrors = mirrorCount;
        }

        @Override
        public int getTemperature() {
            return 900;
        }
    }

    @GameTest(template = ROOM, batch = "solar_receiver", timeoutTicks = 100)
    public static void aSodiumReceiverOnTopIsToldTheMirrorsEveryTick(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        BlockPos topPos = FOOT.above(3);
        helper.setBlock(topPos, Blocks.CHEST);
        FakeReceiver receiver = new FakeReceiver(helper.absolutePos(topPos), helper.getLevel().getBlockState(helper.absolutePos(topPos)));
        helper.getLevel().setBlockEntity(receiver);
        helper.runAfterDelay(30, () -> {
            helper.assertTrue(receiver.ticks > 5, "the receiver was ticked " + receiver.ticks + " times");
            helper.assertTrue(receiver.lastMirrors == MIRRORS, "told of " + receiver.lastMirrors + " mirrors");
            SolarTowerBlockEntity top = helper.getBlockEntity(FOOT.above(2));
            helper.assertTrue(top.temperature() == 900, "the tower did not take the receiver's temperature: " + top.temperature());
            // with a working receiver on top it takes sodium's tag, but not just any fluid
            helper.assertTrue(top.tank().fill(new FluidStack(Fluids.LAVA, 1000), IFluidHandler.FluidAction.SIMULATE) == 0, "took lava");
            helper.assertTrue(top.tank().fill(new FluidStack(Fluids.WATER, 1000), IFluidHandler.FluidAction.SIMULATE) == 1000, "refused water");
            helper.succeed();
        });
    }

    @GameTest(template = ROOM, batch = "solar_off", timeoutTicks = 60)
    public static void theConfigCanTurnTowersOff(GameTestHelper helper) {
        SolarTowerBlockEntity foot = plant(helper);
        fillAll(helper);
        net.scwunge.rotarycraft.config.RotaryConfig.override(net.scwunge.rotarycraft.config.RotaryConfig.SOLAR_TOWER, false);
        helper.runAfterDelay(30, () -> {
            try {
                helper.assertTrue(foot.getTorque() == 0, "an off tower made power");
                helper.succeed();
            } finally {
                net.scwunge.rotarycraft.config.RotaryConfig.clearOverride(net.scwunge.rotarycraft.config.RotaryConfig.SOLAR_TOWER);
            }
        });
    }

    @GameTest(template = ROOM, timeoutTicks = 40)
    public static void clientsReadTheSolarBlocksUpdatePackets(GameTestHelper helper) {
        SolarTowerBlockEntity tower = plant(helper);
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putInt("torque", 77);
        tag.putInt("omega", 512);
        tower.loadCustomOnly(tag, helper.getLevel().registryAccess());
        // loadCustomOnly reads what is saved, not the sync tag: build the sync tag by hand
        net.minecraft.nbt.CompoundTag sync = new net.minecraft.nbt.CompoundTag();
        sync.putInt("torque", 77);
        sync.putInt("omega", 512);
        SolarTowerBlockEntity client = new SolarTowerBlockEntity(tower.getBlockPos(), tower.getBlockState());
        client.onDataPacket(null, net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(tower, (be, registries) -> sync), helper.getLevel().registryAccess());
        helper.assertTrue(client.getTorque() == 77 && client.getOmega() == 512, "the client's tower did not take the packet: " + client.getTorque() + ", " + client.getOmega());
        SolarMirrorBlockEntity mirror = helper.getBlockEntity(FOOT.east(1));
        mirror.breakMirror();
        SolarMirrorBlockEntity clientMirror = new SolarMirrorBlockEntity(mirror.getBlockPos(), mirror.getBlockState());
        clientMirror.onDataPacket(null, (net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket) mirror.getUpdatePacket(), helper.getLevel().registryAccess());
        helper.assertTrue(clientMirror.isBroken(), "the client's mirror did not learn it was broken");
        helper.succeed();
    }
}
