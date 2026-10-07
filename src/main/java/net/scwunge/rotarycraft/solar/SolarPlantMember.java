package net.scwunge.rotarycraft.solar;

import org.jetbrains.annotations.Nullable;

/** A block entity that belongs to a {@link SolarPlant}: a tower block or a mirror. */
public interface SolarPlantMember {
    @Nullable
    SolarPlant plant();

    void setPlant(@Nullable SolarPlant plant);

    /** Finds the plant this block is part of, if it has none yet. */
    void searchForPlant();
}
