package net.scwunge.rotarycraft.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * One of the original's engine upgrades. They are used on the machine they fit: the Gas Engine to a Performance Engine, the converter engines (Magnetic Motor,
 * Steam Turbine, Pneumatic Engine) a tier each, in order, and more efficient, the Dynamo and AC Engine and others something of their own. The second
 * magnetostatic upgrade has to be magnetized to 720 microteslas in the Magnetizer before it can be fitted.
 */
public class EngineUpgradeItem extends Item implements Magnetizable {
    public enum Kind {
        PERFORMANCE, MAGNETOSTATIC1, MAGNETOSTATIC2, MAGNETOSTATIC3, MAGNETOSTATIC4, MAGNETOSTATIC5, EFFICIENCY, FLUX, REDSTONE, LODESTONE;

        /** The converter tier this upgrade takes a machine to, or 0 if it is not one of the tier upgrades. */
        public int tier() {
            return ordinal() >= MAGNETOSTATIC1.ordinal() && ordinal() <= MAGNETOSTATIC5.ordinal() ? ordinal() : 0;
        }

        public String id() {
            return name().toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** What the second magnetostatic upgrade must be magnetized to, in microteslas. */
    public static final int REQUIRED_MAGNETIZATION = 720;

    private final Kind kind;

    public EngineUpgradeItem(Properties properties, Kind kind) {
        super(properties.stacksTo(16));
        this.kind = kind;
    }

    public Kind kind() {
        return kind;
    }

    @Override
    public int chargeChance() {
        return kind == Kind.MAGNETOSTATIC2 ? 4 : 0;
    }

    public static boolean isMagnetized(ItemStack stack) {
        return ShaftCoreItem.magnetization(stack) >= REQUIRED_MAGNETIZATION;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("upgrade.rotarycraft." + kind.id()).withStyle(ChatFormatting.GRAY));
        if (kind == Kind.MAGNETOSTATIC2) {
            int m = ShaftCoreItem.magnetization(stack);
            if (m > 0) {
                tooltip.add(Component.translatable("upgrade.rotarycraft.magnetized_to", m).withStyle(ChatFormatting.AQUA));
            }
            if (m < REQUIRED_MAGNETIZATION) {
                tooltip.add(Component.translatable("upgrade.rotarycraft.must_be_magnetized", REQUIRED_MAGNETIZATION).withStyle(ChatFormatting.RED));
            }
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return kind == Kind.MAGNETOSTATIC2 && ShaftCoreItem.magnetization(stack) > 0;
    }
}
