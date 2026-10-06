package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.menu.BlastFurnaceMenu;
import net.scwunge.rotarycraft.menu.CentrifugeMenu;
import net.scwunge.rotarycraft.menu.ExtractorMenu;
import net.scwunge.rotarycraft.menu.FermenterMenu;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;
import net.scwunge.rotarycraft.menu.GrinderMenu;
import net.scwunge.rotarycraft.menu.OneSlotMenu;
import net.scwunge.rotarycraft.menu.RockMelterMenu;

public class RotaryMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, RotaryCraft.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<GrinderMenu>> GRINDER =
            MENUS.register("grinder", () -> IMenuTypeExtension.create(GrinderMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ExtractorMenu>> EXTRACTOR =
            MENUS.register("extractor", () -> IMenuTypeExtension.create(ExtractorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<BlastFurnaceMenu>> BLAST_FURNACE =
            MENUS.register("blast_furnace", () -> IMenuTypeExtension.create(BlastFurnaceMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FermenterMenu>> FERMENTER =
            MENUS.register("fermenter", () -> IMenuTypeExtension.create(FermenterMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<CentrifugeMenu>> CENTRIFUGE =
            MENUS.register("centrifuge", () -> IMenuTypeExtension.create(CentrifugeMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<FuelEngineMenu>> FUEL_ENGINE =
            fuelEngine("fuel_engine", 1);
    public static final DeferredHolder<MenuType<?>, MenuType<FuelEngineMenu>> PERFORMANCE_ENGINE = fuelEngine("performance_engine", 2);
    public static final DeferredHolder<MenuType<?>, MenuType<FuelEngineMenu>> TURBINE = fuelEngine("turbine", 0);
    public static final DeferredHolder<MenuType<?>, MenuType<FuelEngineMenu>> JET_ENGINE = fuelEngine("jet_engine", 0);
    public static final DeferredHolder<MenuType<?>, MenuType<OneSlotMenu>> MAGNETIZER = oneSlot("magnetizer");
    public static final DeferredHolder<MenuType<?>, MenuType<OneSlotMenu>> AC_ENGINE = oneSlot("ac_engine");
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.menu.FractionatorMenu>> FRACTIONATOR =
            MENUS.register("fractionator", () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.menu.FractionatorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<net.scwunge.rotarycraft.menu.CompactorMenu>> COMPACTOR =
            MENUS.register("compactor", () -> IMenuTypeExtension.create(net.scwunge.rotarycraft.menu.CompactorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<RockMelterMenu>> ROCK_MELTER =
            MENUS.register("rock_melter", () -> IMenuTypeExtension.create(RockMelterMenu::new));

    @SuppressWarnings("unchecked")
    private static DeferredHolder<MenuType<?>, MenuType<FuelEngineMenu>> fuelEngine(String name, int slots) {
        DeferredHolder<MenuType<?>, MenuType<FuelEngineMenu>>[] self = new DeferredHolder[1];
        self[0] = MENUS.register(name, () -> IMenuTypeExtension.create((id, inv, buf) -> new FuelEngineMenu(self[0].get(), slots, id, inv, buf)));
        return self[0];
    }

    /** One-slot machine screens share a menu class; each still gets its own type. */
    @SuppressWarnings("unchecked")
    private static DeferredHolder<MenuType<?>, MenuType<OneSlotMenu>> oneSlot(String name) {
        DeferredHolder<MenuType<?>, MenuType<OneSlotMenu>>[] self = new DeferredHolder[1];
        self[0] = MENUS.register(name, () -> IMenuTypeExtension.create((id, inv, buf) -> new OneSlotMenu(self[0].get(), id, inv, buf)));
        return self[0];
    }
}
