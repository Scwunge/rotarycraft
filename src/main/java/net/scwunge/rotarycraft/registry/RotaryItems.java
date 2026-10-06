package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.item.MeterItem;
import net.scwunge.rotarycraft.item.OreProductItem;
import net.scwunge.rotarycraft.item.ScrewdriverItem;

import java.util.ArrayList;
import java.util.List;

public class RotaryItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(RotaryCraft.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, RotaryCraft.MOD_ID);

    private static final List<DeferredItem<? extends Item>> TAB_ORDER = new ArrayList<>();

    public static final DeferredItem<ScrewdriverItem> SCREWDRIVER = add(ITEMS.register("screwdriver", () -> new ScrewdriverItem(new Item.Properties().stacksTo(1))));
    public static final DeferredItem<MeterItem> METER = add(ITEMS.register("angular_transducer", () -> new MeterItem(new Item.Properties().stacksTo(1))));
    /** HSLA steel, the mod's structural metal (tagged c:ingots/steel so other mods' steel works too). */
    public static final DeferredItem<Item> HSLA_STEEL_INGOT = add(ITEMS.registerSimpleItem("hsla_steel_ingot"));
    public static final DeferredItem<Item> SAWDUST = add(ITEMS.registerSimpleItem("sawdust"));
    public static final DeferredItem<Item> FLOUR = add(ITEMS.registerSimpleItem("flour"));
    public static final DeferredItem<Item> NETHERRACK_DUST = add(ITEMS.registerSimpleItem("netherrack_dust"));
    public static final DeferredItem<Item> TAR = add(ITEMS.registerSimpleItem("tar"));
    public static final DeferredItem<Item> SILVER_INGOT = add(ITEMS.registerSimpleItem("silver_ingot"));
    public static final DeferredItem<Item> TUNGSTEN_INGOT = add(ITEMS.registerSimpleItem("tungsten_ingot"));
    public static final DeferredItem<OreProductItem> ORE_DUST = ITEMS.register("ore_dust", () -> new OreProductItem(new Item.Properties(), OreProductItem.Stage.DUST));
    public static final DeferredItem<OreProductItem> ORE_SLURRY = ITEMS.register("ore_slurry", () -> new OreProductItem(new Item.Properties(), OreProductItem.Stage.SLURRY));
    public static final DeferredItem<OreProductItem> ORE_SOLUTION = ITEMS.register("ore_solution", () -> new OreProductItem(new Item.Properties(), OreProductItem.Stage.SOLUTION));
    public static final DeferredItem<OreProductItem> ORE_FLAKES = ITEMS.register("ore_flakes", () -> new OreProductItem(new Item.Properties(), OreProductItem.Stage.FLAKES));

    static {
        blockItem(RotaryBlocks.DC_ENGINE);
        blockItem(RotaryBlocks.WIND_ENGINE);
        blockItem(RotaryBlocks.STEAM_ENGINE);
        RotaryBlocks.SHAFTS.values().forEach(RotaryItems::blockItem);
        RotaryBlocks.GEARBOXES.values().forEach(RotaryItems::blockItem);
        blockItem(RotaryBlocks.BEVEL_GEAR);
        blockItem(RotaryBlocks.SPLITTER);
        blockItem(RotaryBlocks.CLUTCH);
        RotaryBlocks.FLYWHEELS.values().forEach(RotaryItems::blockItem);
        blockItem(RotaryBlocks.DYNAMOMETER);
        blockItem(RotaryBlocks.GRINDER);
        blockItem(RotaryBlocks.EXTRACTOR);
        blockItem(RotaryBlocks.GENERATOR);
        blockItem(RotaryBlocks.ELECTRIC_MOTOR);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.rotarycraft"))
            .icon(() -> new ItemStack(RotaryBlocks.DC_ENGINE.get()))
            .displayItems((params, output) -> TAB_ORDER.forEach(i -> output.accept(i.get())))
            .build());

    private static <T extends Item> DeferredItem<T> add(DeferredItem<T> item) {
        TAB_ORDER.add(item);
        return item;
    }

    private static void blockItem(DeferredBlock<?> block) {
        add(ITEMS.registerSimpleBlockItem(block));
    }
}
