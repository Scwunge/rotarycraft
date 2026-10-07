package net.scwunge.rotarycraft.item;

/** Something the Magnetizer can magnetize: it adds a microtesla one cycle in {@link #chargeChance()}. */
public interface Magnetizable {
    int chargeChance();
}
