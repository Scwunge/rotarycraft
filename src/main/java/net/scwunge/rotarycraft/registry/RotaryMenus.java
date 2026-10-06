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
import net.scwunge.rotarycraft.menu.GrinderMenu;
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
    public static final DeferredHolder<MenuType<?>, MenuType<RockMelterMenu>> ROCK_MELTER =
            MENUS.register("rock_melter", () -> IMenuTypeExtension.create(RockMelterMenu::new));
}
