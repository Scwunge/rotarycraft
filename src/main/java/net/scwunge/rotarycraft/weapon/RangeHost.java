package net.scwunge.rotarycraft.weapon;

/** A machine with an effect radius that is set on a screen (the original's "range" GUI). */
public interface RangeHost {
    /** The radius the player asked for. */
    int setRange();

    void setSetRange(int range);

    /** The radius it has now, held to what the power (and anything in the way) allows. */
    int range();

    int maxRange();
}
