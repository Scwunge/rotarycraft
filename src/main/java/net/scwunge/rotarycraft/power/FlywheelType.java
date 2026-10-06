package net.scwunge.rotarycraft.power;

/** Flywheel materials with the original's torque rating, coast-down time, density and tensile strength. */
public enum FlywheelType {
    //       max torque         decay ticks  density  tensile strength (Pa)
    WOOD(    16,                2,           800,     20e6),
    STONE(   128,               5,           3000,    0.9 * 100e6),
    IRON(    512,               15,          7800,    5 * 200e6),
    GOLD(    4096,              40,          19300,   108e6),
    BEDROCK( Integer.MAX_VALUE, 200,         1.75 * 7800, Double.POSITIVE_INFINITY);

    public static final int MIN_TORQUE_RATIO = 4;
    private static final double RADIUS = 0.75;
    private static final double THICKNESS = 0.25;

    public final int maxTorque;
    public final int decayTicks;
    public final double density;
    public final double tensileStrength;
    /** Moment of inertia of a 0.75 m x 0.25 m disk of this material. */
    public final double inertia;

    FlywheelType(int maxTorque, int decayTicks, double density, double tensileStrength) {
        this.maxTorque = maxTorque;
        this.decayTicks = decayTicks;
        this.density = density;
        this.tensileStrength = tensileStrength;
        double mass = THICKNESS * RADIUS * RADIUS * Math.PI * density;
        this.inertia = mass * RADIUS * RADIUS / 2;
    }

    /** Input torque below this won't drive the flywheel. */
    public int minTorque() {
        return this == BEDROCK ? 16384 : maxTorque / MIN_TORQUE_RATIO;
    }

    /** Centrifugal stress at this speed exceeds the material's strength (x100 safety margin, as in the original). */
    public boolean fails(int omega) {
        if (Double.isInfinite(tensileStrength)) {
            return false;
        }
        double sigma = density * (double) omega * omega * RADIUS * RADIUS / 2;
        return sigma > 100 * tensileStrength;
    }

    /** Rotational kinetic energy (J) at this speed, used for the burst strength. */
    public double energyAt(int omega) {
        double volume = Math.PI * RADIUS * RADIUS * THICKNESS;
        double i = density * volume * (Math.PI / 4) * Math.pow(RADIUS, 4);
        return 0.5 * i * (double) omega * omega;
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
