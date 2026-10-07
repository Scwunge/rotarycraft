package net.scwunge.rotarycraft.registry;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.neoforged.neoforge.registries.DeferredItem;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.item.EngineUpgradeItem;
import net.scwunge.rotarycraft.item.GearUpgradeItem;

import java.util.EnumMap;
import java.util.Map;

/**
 * The engine upgrades (performance, the five magnetostatic tiers, efficiency, flux, redstone and lodestone; the afterburner is with the items) and the integrated
 * gearbox upgrade in its five forms.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public final class UpgradeRegistry {
    public static final Map<EngineUpgradeItem.Kind, DeferredItem<EngineUpgradeItem>> UPGRADES = new EnumMap<>(EngineUpgradeItem.Kind.class);
    /** The gearbox items by exponent: the frame, then ratios 2, 4, 8 and 16. */
    public static final DeferredItem<GearUpgradeItem>[] GEARS;

    static {
        for (EngineUpgradeItem.Kind kind : EngineUpgradeItem.Kind.values()) {
            String name = switch (kind) {
                case MAGNETOSTATIC1, MAGNETOSTATIC2, MAGNETOSTATIC3, MAGNETOSTATIC4, MAGNETOSTATIC5 -> "magnetostatic_upgrade_" + kind.tier();
                default -> kind.id() + "_upgrade";
            };
            UPGRADES.put(kind, RotaryItems.add(RotaryItems.ITEMS.register(name, () -> new EngineUpgradeItem(new Item.Properties(), kind))));
        }
        @SuppressWarnings("unchecked")
        DeferredItem<GearUpgradeItem>[] gears = new DeferredItem[5];
        for (int i = 0; i < 5; i++) {
            int exponent = i;
            gears[i] = RotaryItems.add(RotaryItems.ITEMS.register(i == 0 ? "gear_upgrade_frame" : "gear_upgrade_" + (1 << i) + "x", () -> new GearUpgradeItem(new Item.Properties(), exponent)));
        }
        GEARS = gears;
    }

    private UpgradeRegistry() {}

    public static void init(IEventBus modBus) {
    }

    public static Item[] gearItems() {
        Item[] items = new Item[GEARS.length];
        for (int i = 0; i < items.length; i++) {
            items[i] = GEARS[i].get();
        }
        return items;
    }

    public static EngineUpgradeItem upgrade(EngineUpgradeItem.Kind kind) {
        return UPGRADES.get(kind).get();
    }

    /** Gearboxes take lubricant and liquid nitrogen only, 500 mB, and keep it in the item. */
    private static final class GearTank extends FluidHandlerItemStack {
        GearTank(ItemStack container) {
            super(RotaryComponents.ITEM_FLUID, container, GearUpgradeItem.CAPACITY);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return GearUpgradeItem.accepts(stack) && (getFluid().isEmpty() || getFluid().getFluid() == stack.getFluid());
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return isFluidValid(0, fluid);
        }

        @Override
        public boolean canDrainFluidType(FluidStack fluid) {
            return false;
        }
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        for (int i = 1; i < GEARS.length; i++) {
            event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new GearTank(stack), GEARS[i].get());
        }
    }
}
