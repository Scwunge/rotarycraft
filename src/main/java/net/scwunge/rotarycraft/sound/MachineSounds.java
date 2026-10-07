package net.scwunge.rotarycraft.sound;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.Level;
import net.scwunge.rotarycraft.blockentity.ACEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.CrystallizerBlockEntity;
import net.scwunge.rotarycraft.blockentity.DCEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.FlywheelBlockEntity;
import net.scwunge.rotarycraft.blockentity.FrictionHeaterBlockEntity;
import net.scwunge.rotarycraft.blockentity.FuelEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.GasEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.HydroEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.JetEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.MicroturbineBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.blockentity.PerformanceEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.blockentity.PulseFurnaceBlockEntity;
import net.scwunge.rotarycraft.blockentity.PumpBlockEntity;
import net.scwunge.rotarycraft.blockentity.RefrigeratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.SteamEngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.WindEngineBlockEntity;
import net.scwunge.rotarycraft.config.SoundConfig;
import net.scwunge.rotarycraft.farm.FanBlockEntity;
import net.scwunge.rotarycraft.farm.SprinklerBaseBlockEntity;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;
import net.scwunge.rotarycraft.transmission.BeltHubBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The sounds machines make while they work, as the original's: each machine plays its sound again each time it ends (the lengths below are the
 * original's, in ticks), at the volume and pitch the original used, quietened by the machine volume options, and by a third as much again
 * if wool is both above and below it (the original's muffling).
 */
public final class MachineSounds {
    private MachineSounds() {}

    /** One machine's sound. */
    public record Spec(String sound, int length, float volume, float pitch, boolean engine) {
        /** The same sound with the length of the sound at a different pitch (the lower the pitch, the longer it lasts). */
        public int lengthAtPitch() {
            return Math.max(1, Math.round(length / pitch));
        }
    }

    /** The sound a machine makes while it runs, or null if it makes none or is not running. */
    @Nullable
    public static Spec specFor(PowerBlockEntity be) {
        if (be.getOmega() <= 0) {
            return null;
        }
        if (be instanceof DCEngineBlockEntity || be instanceof ACEngineBlockEntity) {
            return new Spec("elecengine", 75, 0.125F, 1F, true);
        }
        if (be instanceof MotorBlockEntity) {
            return new Spec("elecengine", 75, 0.2F, 0.51F, true);
        }
        if (be instanceof GasEngineBlockEntity || be instanceof PerformanceEngineBlockEntity) {
            return new Spec("gasengine", 80, 0.33F, 0.9F, true);
        }
        if (be instanceof FuelEngineBlockEntity) {
            return new Spec("diesel", 20, 1F, 0.4F, true);
        }
        if (be instanceof SteamEngineBlockEntity) {
            return new Spec("steamengine", 51, 0.7F, 1F, true);
        }
        if (be instanceof HydroEngineBlockEntity) {
            return new Spec("hydroengine", 53, 1F, 0.9F, true);
        }
        if (be instanceof WindEngineBlockEntity) {
            return new Spec("windengine", 107, 1.1F, 1F, true);
        }
        if (be instanceof MicroturbineBlockEntity) {
            return new Spec("microengine", 20, 1F, 1F, true);
        }
        if (be instanceof JetEngineBlockEntity jet) {
            return jet.isAfterburning() ? new Spec("afterburner", 50, 0.9F, 1F, true) : new Spec("jetengine", 80, 1F, 1F, true);
        }
        if (be instanceof FlywheelBlockEntity) {
            return new Spec("flywheel", 69, 0.4F, 1F, false);
        }
        if (be instanceof PumpBlockEntity) {
            return new Spec("pump", 100, 0.5F, 1F, false);
        }
        if (be instanceof RefrigeratorBlockEntity) {
            return new Spec("fridge", 18, 1F, 0.88F, false);
        }
        if (be instanceof FrictionHeaterBlockEntity) {
            return new Spec("friction", 50, 0.5F, 1F, false);
        }
        if (be instanceof CrystallizerBlockEntity) {
            return new Spec("fan", 27, 0.4F, 0.6F, false);
        }
        if (be instanceof PulseFurnaceBlockEntity) {
            return new Spec("pulsejet", 18, 1F, 1F, false);
        }
        if (be instanceof BeltHubBlockEntity) {
            return new Spec("belt", 26, 0.6F, 1F, false);
        }
        if (be instanceof FanBlockEntity fan) {
            return fan.getPower() >= FanBlockEntity.REQUIREMENT.minPower() ? new Spec("fan", 27, 0.5F, 1F, false) : null;
        }
        return null;
    }

    /** The sound of a sprinkler that is spraying. */
    @Nullable
    public static Spec specFor(SprinklerBaseBlockEntity sprinkler) {
        return sprinkler.isWorking() ? new Spec("sprinkler", 40, 1F, 1F, false) : null;
    }

    /** Whether wool is both above and below. */
    public static boolean isMuffled(Level level, BlockPos pos) {
        return level.getBlockState(pos.above()).is(BlockTags.WOOL) && level.getBlockState(pos.below()).is(BlockTags.WOOL);
    }

    /** The volume the sound is played at, with the options and muffling applied. */
    public static float volumeOf(Spec spec, boolean muffled) {
        float volume = spec.volume() * SoundConfig.machineVolume() * (spec.engine() ? SoundConfig.engineVolume() : 1F);
        return muffled ? volume * 0.25F : volume;
    }

    /** A one-off sound from a machine at the machine volume (the borer's rumble as it digs, for one). */
    public static void playOnce(ServerLevel server, BlockPos pos, String sound, float volume, float pitch) {
        float scaled = volume * SoundConfig.machineVolume() * (isMuffled(server, pos) ? 0.25F : 1F);
        if (scaled > 0) {
            server.playSound(null, pos, MachineSoundRegistry.get(sound).get(), SoundSource.BLOCKS, scaled, pitch);
        }
    }

    /** Called once a tick for each machine on the server: plays its sound if it is time for the next. */
    public static void tick(PowerBlockEntity be) {
        if (!(be.getLevel() instanceof ServerLevel server)) {
            return;
        }
        long now = server.getGameTime();
        if (now < be.nextSoundTick) {
            return;
        }
        Spec spec = be instanceof SprinklerBaseBlockEntity sprinkler ? specFor(sprinkler) : specFor(be);
        if (spec == null) {
            be.nextSoundTick = now + 1;
            be.soundStarted = false;
            return;
        }
        if (be instanceof JetEngineBlockEntity && !be.soundStarted) {
            be.soundStarted = true;
            server.playSound(null, be.getBlockPos(), MachineSoundRegistry.get("jetstart").get(), SoundSource.BLOCKS, volumeOf(new Spec("jetstart", 480, 1F, 1F, true), false), 1F);
        }
        float volume = volumeOf(spec, isMuffled(server, be.getBlockPos()));
        if (volume > 0) {
            server.playSound(null, be.getBlockPos(), MachineSoundRegistry.get(spec.sound()).get(), SoundSource.BLOCKS, volume, spec.pitch());
        }
        be.nextSoundTick = now + spec.lengthAtPitch();
    }
}
