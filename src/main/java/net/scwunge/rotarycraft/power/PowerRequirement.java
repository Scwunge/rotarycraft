package net.scwunge.rotarycraft.power;

/**
 * What a machine needs to run: at least this torque, this speed and this power, all at once (the original's
 * PowerReceivers values).
 */
public record PowerRequirement(int minTorque, int minOmega, long minPower) {
    public boolean isMetBy(int torque, int omega) {
        return torque >= minTorque && omega >= minOmega && (long) torque * omega >= minPower;
    }

    /**
     * The original's operation time: base - scale * log2(omega + 1) ticks, never under one tick. Faster shafts work faster.
     */
    public static int operationTime(int base, int scale, int omega) {
        double t = base - scale * (Math.log(Math.max(0, omega) + 1D) / Math.log(2));
        return (int) Math.max(1, t);
    }
}
