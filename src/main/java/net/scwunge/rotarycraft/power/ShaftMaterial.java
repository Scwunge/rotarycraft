package net.scwunge.rotarycraft.power;

/**
 * Shaft materials and their load limits, computed from material properties the same way the original does: a shaft of
 * radius 0.0625 m fails when torque exceeds the torsional limit from the shear strength, or when speed exceeds the limit
 * from the tensile strength and density (with a per-material speed/force exponent).
 */
public enum ShaftMaterial {
    //       shear (Pa)  tensile (Pa)  density (kg/m^3)  speed exponent
    WOOD(    11.6e6,     20e6,         800,              1.00),
    STONE(   40e6,       100e6,        3000,             0.89),
    STEEL(   280e6,      400e6,        7800,             0.78),
    TUNGSTEN(400e6,      980e6,        7800 * 0.8 + 0.2 * 19300, 0.70),
    DIAMOND( 2.9e9,      5e9,          3500,             0.67),
    BEDROCK( Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, 7800, 1.00);

    private static final double RADIUS = 0.0625;

    public final double shear;
    public final double tensile;
    public final double density;
    public final double speedExponent;
    /** Largest torque (N*m) a shaft of this material carries before it breaks. */
    public final double maxTorque;
    /** Largest angular speed (rad/s) a shaft of this material carries before it breaks. */
    public final double maxSpeed;

    ShaftMaterial(double shear, double tensile, double density, double speedExponent) {
        this.shear = shear;
        this.tensile = tensile;
        this.density = density;
        this.speedExponent = speedExponent;
        if (Double.isInfinite(shear)) {
            this.maxTorque = Double.POSITIVE_INFINITY;
            this.maxSpeed = Double.POSITIVE_INFINITY;
        } else {
            this.maxTorque = 0.5 * Math.PI * RADIUS * RADIUS * RADIUS * shear / 16D;
            double base = Math.sqrt(2 * tensile / (density * RADIUS * RADIUS));
            this.maxSpeed = Math.pow(base, 1D / speedExponent);
        }
    }

    public boolean isUnbreakable() {
        return this == BEDROCK;
    }

    /** True when the load would break a shaft of this material. */
    public boolean fails(int torque, int omega) {
        return !isUnbreakable() && (torque > maxTorque || omega > maxSpeed);
    }

    public String id() {
        return name().toLowerCase(java.util.Locale.ROOT);
    }
}
