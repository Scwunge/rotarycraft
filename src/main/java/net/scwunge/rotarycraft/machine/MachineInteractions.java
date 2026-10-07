package net.scwunge.rotarycraft.machine;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

/** What a machine does when something is used on it, beyond opening its screen: the screwdriver, a lens, a dye. Each returns true if it handled the use. */
public interface MachineInteractions {
    /** The screwdriver was used on the machine; false lets it turn the machine as usual. */
    default boolean onScrewdriver(UseOnContext context) {
        return false;
    }

    /** The block was broken or replaced: take away whatever the machine made in the world (light, a beam) and drop what it keeps outside its inventory. */
    default void onBroken(net.minecraft.server.level.ServerLevel server) {
    }

    /** What a comparator reads from the machine, 0 to 15; below 0 for a machine that gives none. */
    default int comparatorSignal() {
        return -1;
    }

    /** {@code stack} was used on the machine by right-click (not a fluid container, which the block handles first). */
    default boolean onItemUse(ItemStack stack, Player player, InteractionHand hand) {
        return false;
    }
}
