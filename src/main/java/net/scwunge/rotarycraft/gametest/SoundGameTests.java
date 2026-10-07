package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.MachineBlock;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.config.SoundConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.sound.MachineSounds;

/** Tests of the machines' sounds. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class SoundGameTests {
    static final String TEMPLATE = RotaryGameTests.TEMPLATE;
    static final BlockPos AT = new BlockPos(2, 2, 2);

    static PowerBlockEntity spinning(GameTestHelper helper, net.minecraft.world.level.block.Block block, int torque, int omega) {
        helper.setBlock(AT, block.defaultBlockState().setValue(MachineBlock.FACING, Direction.UP));
        PowerBlockEntity be = helper.getBlockEntity(AT);
        CompoundTag tag = new CompoundTag();
        tag.putInt("torque", torque);
        tag.putInt("omega", omega);
        be.loadCustomOnly(tag, helper.getLevel().registryAccess());
        return be;
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "sounds")
    public static void everySoundHasAnEventAndAFile(GameTestHelper helper) {
        int n = 0;
        for (String name : MachineSoundRegistry.names()) {
            n++;
            helper.assertTrue(MachineSoundRegistry.get(name).getId().getPath().equals(name), name);
        }
        helper.assertTrue(n >= 45, "only " + n + " sounds");
        var json = SoundGameTests.class.getResourceAsStream("/assets/rotarycraft/sounds.json");
        helper.assertTrue(json != null, "no sounds.json");
        String text;
        try {
            text = new String(json.readAllBytes());
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        for (String name : MachineSoundRegistry.names()) {
            helper.assertTrue(text.contains("\"" + name + "\""), "sounds.json lacks " + name);
        }
        for (String file : new String[] {"fan", "jetstart", "windengine", "music/harp"}) {
            helper.assertTrue(SoundGameTests.class.getResource("/assets/rotarycraft/sounds/" + file + ".ogg") != null, "no file " + file);
        }
        helper.succeed();
    }

    /** The original's sound, volume, pitch and length for each machine. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "sounds")
    public static void machinesMakeTheirOriginalSounds(GameTestHelper helper) {
        var dc = MachineSounds.specFor(spinning(helper, RotaryBlocks.DC_ENGINE.get(), 4, 64));
        helper.assertTrue(dc != null && dc.sound().equals("elecengine") && dc.volume() == 0.125F && dc.length() == 75, "dc engine " + dc);
        var gas = MachineSounds.specFor(spinning(helper, RotaryBlocks.GAS_ENGINE.get(), 4, 64));
        helper.assertTrue(gas != null && gas.sound().equals("gasengine") && gas.pitch() == 0.9F, "gas engine " + gas);
        var motor = MachineSounds.specFor(spinning(helper, RotaryBlocks.ELECTRIC_MOTOR.get(), 4, 64));
        helper.assertTrue(motor != null && motor.pitch() == 0.51F && motor.lengthAtPitch() == Math.round(75 / 0.51F), "motor " + motor);
        var steam = MachineSounds.specFor(spinning(helper, RotaryBlocks.STEAM_ENGINE.get(), 4, 64));
        helper.assertTrue(steam != null && steam.sound().equals("steamengine"), "steam engine " + steam);
        var pump = MachineSounds.specFor(spinning(helper, RotaryBlocks.PUMP.get(), 4, 64));
        helper.assertTrue(pump != null && pump.sound().equals("pump"), "pump " + pump);
        var fan = MachineSounds.specFor(spinning(helper, FarmRegistry.FAN.get(), 64, 128));
        helper.assertTrue(fan != null && fan.sound().equals("fan") && fan.length() == 27, "fan " + fan);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "sounds")
    public static void aStoppedMachineIsSilent(GameTestHelper helper) {
        helper.assertTrue(MachineSounds.specFor(spinning(helper, RotaryBlocks.DC_ENGINE.get(), 4, 0)) == null, "a stopped engine made a sound");
        helper.assertTrue(MachineSounds.specFor(spinning(helper, FarmRegistry.FAN.get(), 1, 512)) == null, "a fan without power made a sound");
        helper.succeed();
    }

    /** It plays again when the last sound has ended, not before. */
    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "sounds")
    public static void aMachinePlaysAgainWhenItsSoundEnds(GameTestHelper helper) {
        var fan = spinning(helper, FarmRegistry.FAN.get(), 64, 128);
        long now = helper.getLevel().getGameTime();
        fan.nextSoundTick = 0;
        MachineSounds.tick(fan);
        helper.assertTrue(fan.nextSoundTick == now + 27, "next sound at " + (fan.nextSoundTick - now));
        long set = fan.nextSoundTick;
        MachineSounds.tick(fan);
        helper.assertTrue(fan.nextSoundTick == set, "played again too soon");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "sounds")
    public static void woolAboveAndBelowMuffles(GameTestHelper helper) {
        var fan = spinning(helper, FarmRegistry.FAN.get(), 64, 128);
        var spec = MachineSounds.specFor(fan);
        helper.assertTrue(!MachineSounds.isMuffled(helper.getLevel(), helper.absolutePos(AT)), "muffled in the open");
        helper.setBlock(AT.above(), Blocks.WHITE_WOOL);
        helper.assertTrue(!MachineSounds.isMuffled(helper.getLevel(), helper.absolutePos(AT)), "muffled by wool on one side");
        helper.setBlock(AT.below(), Blocks.RED_WOOL);
        helper.assertTrue(MachineSounds.isMuffled(helper.getLevel(), helper.absolutePos(AT)), "not muffled by wool on both");
        helper.assertTrue(MachineSounds.volumeOf(spec, true) == MachineSounds.volumeOf(spec, false) * 0.25F, "muffled volume");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40, batch = "sound_volume")
    public static void theVolumeOptionsScaleTheSound(GameTestHelper helper) {
        var engine = new MachineSounds.Spec("elecengine", 75, 0.5F, 1F, true);
        var machine = new MachineSounds.Spec("pump", 100, 0.5F, 1F, false);
        RotaryConfig.override(SoundConfig.MACHINE_VOLUME, 0.5);
        RotaryConfig.override(SoundConfig.ENGINE_VOLUME, 0.5);
        float e = MachineSounds.volumeOf(engine, false);
        float m = MachineSounds.volumeOf(machine, false);
        RotaryConfig.clearOverride(SoundConfig.MACHINE_VOLUME);
        RotaryConfig.clearOverride(SoundConfig.ENGINE_VOLUME);
        helper.assertTrue(e == 0.125F && m == 0.25F, "engine " + e + ", machine " + m);
        helper.succeed();
    }
}
