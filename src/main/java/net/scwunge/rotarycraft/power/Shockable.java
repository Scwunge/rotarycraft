package net.scwunge.rotarycraft.power;

/**
 * Something a Van de Graaff Generator's discharge can hit (ReactorCraft's Electrolyzer, for one). The generator checks
 * this when choosing a target: the discharge has to be at least {@link #getMinDischarge()}, and long-range hits only
 * count if {@link #canDischargeLongRange()}. The aim point is where the bolt is drawn to.
 */
public interface Shockable {
    /** A bolt of {@code charge} (the generator's charge when it fired) hits from {@code range} blocks away. */
    void onDischarge(int charge, double range);

    /** The smallest discharge this accepts. */
    int getMinDischarge();

    boolean canDischargeLongRange();

    float getAimX();

    float getAimY();

    float getAimZ();
}
