package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The sound events of the original's machine sounds (the weapons' own are in WeaponRegistry). The names are the keys of sounds.json, which
 * tools/gen_sounds.py writes with the sound files.
 */
public final class MachineSoundRegistry {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, RotaryCraft.MOD_ID);
    private static final Map<String, DeferredHolder<SoundEvent, SoundEvent>> EVENTS = new LinkedHashMap<>();

    private static final String[] NAMES = {"afterburner", "belt", "coil", "compress", "craft", "diesel", "dynamo", "elecengine", "fan", "flywheel", "friction",
            "fridge", "gasengine", "hydroengine", "ingest", "ingest_short", "jetengine", "jetstart", "knockback", "linebuild", "massivebang", "microengine",
            "pack", "piledriver", "pneu", "projector", "pulsejet", "pump", "rumble", "rumble2", "shortjet", "smokealarm", "spark", "sprinkler", "steamengine",
            "windengine", "note_bass_low", "note_bass", "note_bass_high", "note_harp_low", "note_harp", "note_harp_high", "note_pling_low", "note_pling",
            "note_pling_high"};

    static {
        for (String name : NAMES) {
            EVENTS.put(name, SOUNDS.register(name, () -> SoundEvent.createVariableRangeEvent(RotaryCraft.id(name))));
        }
    }

    private MachineSoundRegistry() {}

    public static DeferredHolder<SoundEvent, SoundEvent> get(String name) {
        DeferredHolder<SoundEvent, SoundEvent> event = EVENTS.get(name);
        if (event == null) {
            throw new IllegalArgumentException("no sound " + name);
        }
        return event;
    }

    public static Iterable<String> names() {
        return EVENTS.keySet();
    }

    public static void init(IEventBus modBus) {
        SOUNDS.register(modBus);
    }
}
