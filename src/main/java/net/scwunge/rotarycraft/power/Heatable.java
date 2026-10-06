package net.scwunge.rotarycraft.power;

/** A machine whose temperature other machines (the Friction Heater) can raise. */
public interface Heatable {
    int getTemperature();

    int getMaxTemperature();

    void addTemperature(int amount);

    /** How strongly outside heat is taken up (1 = fully). */
    default float heatMultiplier() {
        return 1;
    }
}
