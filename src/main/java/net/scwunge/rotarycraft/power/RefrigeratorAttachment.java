package net.scwunge.rotarycraft.power;

/**
 * A block entity against a Refrigerator that wants to know when it finishes a cycle (ReactorCraft's Gas Collector, which
 * adds to its stock with the cold). The Refrigerator calls this on every adjacent block entity that implements it.
 */
public interface RefrigeratorAttachment {
    /** The Refrigerator has just produced {@code liquidNitrogen} mB of liquid nitrogen. */
    void onCompleteCycle(int liquidNitrogen);
}
