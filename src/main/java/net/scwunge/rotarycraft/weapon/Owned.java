package net.scwunge.rotarycraft.weapon;

import net.minecraft.world.entity.player.Player;

/** A machine that remembers who placed it, so what it breaks can be checked against claims. */
public interface Owned {
    void setOwner(Player player);
}
