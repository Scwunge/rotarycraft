package net.scwunge.rotarycraft.api;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge event bus when a Weather Controller changes the weather (the original's API event). */
public class WeatherControlEvent extends Event {
    private final BlockEntity controller;
    private final boolean rain;
    private final boolean thunder;
    private final boolean superStorm;

    public WeatherControlEvent(BlockEntity controller, boolean rain, boolean thunder, boolean superStorm) {
        this.controller = controller;
        this.rain = rain;
        this.thunder = thunder;
        this.superStorm = superStorm;
    }

    public BlockEntity controller() {
        return controller;
    }

    public boolean setRain() {
        return rain;
    }

    public boolean setThunder() {
        return thunder;
    }

    public boolean setSuperStorm() {
        return superStorm;
    }
}
