package net.scwunge.rotarycraft.api;

/**
 * The blocks that can stand on top of, or beside, the Solar Tower to make it run on liquid sodium (ReactorCraft's solar exchanger and solar
 * top are the original's). The tower talks to them through these.
 */
public interface SodiumSolarUpgrades {
    /** Whether the block is working (the tower will not use sodium while it is not). */
    boolean isActive();

    /** What sits on top of the tower: it is told, every tick, how many mirrors point at it and how bright the light on them is. */
    interface SodiumSolarReceiver extends SodiumSolarUpgrades {
        /** @param mirrorCount mirrors aimed at the receiver; @param totalBrightness the light they gather, summed */
        void tick(int mirrorCount, float totalBrightness);

        /** The receiver's temperature, which decides how well the tower runs. */
        int getTemperature();
    }

    /** What the tower's heated sodium is pumped into. */
    interface SodiumSolarOutput extends SodiumSolarUpgrades {
        /**
         * Takes sodium from the tower.
         *
         * @param amt millibuckets offered
         * @return how many it did not take
         */
        int receiveSodium(int amt);
    }
}
