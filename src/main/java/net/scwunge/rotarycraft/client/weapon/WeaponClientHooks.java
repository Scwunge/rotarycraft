package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

import java.util.List;

/** Client side of the weapons' packets. Only called from handlers that run on the client. */
public final class WeaponClientHooks {
    private WeaponClientHooks() {}

    public static void openSafePlayers(BlockPos pos, List<String> names) {
        Minecraft.getInstance().setScreen(new SafePlayersScreen(pos, names));
    }
}
