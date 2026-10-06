package net.scwunge.rotarycraft.power;

/** A machine whose temperature other machines (the Friction Heater) can raise. */
public interface Heatable {
    int getTemperature();

    int getMaxTemperature();

    void addTemperature(int amount);

    /** Whether a Friction Heater facing it can warm it (the original allows the furnace-like machines only). */
    default boolean canBeFrictionHeated() {
        return true;
    }

    /** Whether a Cooling Fin against it can draw heat out (steam engines, gearboxes, the Compactor...). */
    default boolean canBeCooledWithFins() {
        return false;
    }

    /** How strongly outside heat is taken up (1 = fully). */
    default float heatMultiplier() {
        return 1;
    }
}
