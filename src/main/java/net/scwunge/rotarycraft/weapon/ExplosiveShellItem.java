package net.scwunge.rotarycraft.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** The explosive shell: rail gun ammunition that needs no torque and explodes (power 8, with fire) where it lands. A rail gun uses it only when it has no other ammunition. */
public class ExplosiveShellItem extends RailgunAmmoItem {
    public static final float EXPLOSION = 8F;

    public ExplosiveShellItem(Properties properties) {
        super(properties, -1);
    }

    @Override
    public boolean explosive() {
        return true;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.rotarycraft.explosive_shell"));
    }
}
