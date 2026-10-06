package net.scwunge.rotarycraft;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryBlocks;
import net.scwunge.rotarycraft.registry.RotaryItems;

@Mod(RotaryCraft.MOD_ID)
public class RotaryCraft {
    public static final String MOD_ID = "rotarycraft";

    public static net.minecraft.resources.ResourceLocation id(String path) {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public static final net.neoforged.neoforge.registries.DeferredRegister<com.mojang.serialization.MapCodec<? extends net.neoforged.neoforge.common.loot.IGlobalLootModifier>> LOOT_MODIFIERS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.neoforged.neoforge.registries.NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MOD_ID);
    static {
        LOOT_MODIFIERS.register("add_item", () -> net.scwunge.rotarycraft.loot.AddItemModifier.CODEC);
    }

    public RotaryCraft(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, RotaryConfig.SPEC, "rotarycraft-server.toml");
        // fluids add their blocks and buckets to the block and item registers, so they load first
        net.scwunge.rotarycraft.registry.RotaryFluids.TYPES.register(modBus);
        net.scwunge.rotarycraft.registry.RotaryFluids.FLUIDS.register(modBus);
        RotaryBlocks.BLOCKS.register(modBus);
        RotaryItems.ITEMS.register(modBus);
        net.scwunge.rotarycraft.registry.RotaryParts.init();
        RotaryItems.TABS.register(modBus);
        RotaryBlockEntities.TYPES.register(modBus);
        net.scwunge.rotarycraft.registry.RotaryRecipes.TYPES.register(modBus);
        net.scwunge.rotarycraft.registry.RotaryRecipes.SERIALIZERS.register(modBus);
        net.scwunge.rotarycraft.registry.RotaryMenus.MENUS.register(modBus);
        net.scwunge.rotarycraft.registry.RotaryComponents.COMPONENTS.register(modBus);
        LOOT_MODIFIERS.register(modBus);
        modBus.addListener(RotaryCraft::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RotaryBlockEntities.GENERATOR.get(),
                (be, side) -> side == be.inputSide() ? null : be.energy());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.PIPE.get(),
                (be, side) -> be.input());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.GEARBOX.get(),
                (be, side) -> be.lubricantHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.GRINDER.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.BLAST_FURNACE.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.FERMENTER.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.FERMENTER.get(),
                (be, side) -> side == be.inputSide() ? null : be.water());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.CENTRIFUGE.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.CENTRIFUGE.get(),
                (be, side) -> side == be.inputSide() ? null : be.tank());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.PERFORMANCE_ENGINE.get(),
                (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.PERFORMANCE_ENGINE.get(),
                (be, side) -> be.fuelHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.HYDRO_ENGINE.get(),
                (be, side) -> be.lubricantHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.JET_ENGINE.get(),
                (be, side) -> be.fuelHandler(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.MICROTURBINE.get(),
                (be, side) -> be.fuelHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.AC_ENGINE.get(),
                (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.MAGNETIZER.get(),
                (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.GAS_ENGINE.get(),
                (be, side) -> be.items());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.GAS_ENGINE.get(),
                (be, side) -> be.fuelHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.FRACTIONATOR.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.FRACTIONATOR.get(),
                (be, side) -> be.fluidHandler(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.ROCK_MELTER.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.ROCK_MELTER.get(),
                (be, side) -> be.output(side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RotaryBlockEntities.EXTRACTOR.get(),
                (be, side) -> be.automationItems());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.EXTRACTOR.get(),
                (be, side) -> side == be.inputSide() ? null : be.water());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.STEAM_ENGINE.get(),
                (be, side) -> side == be.facing() ? null : be.water());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RotaryBlockEntities.ELECTRIC_MOTOR.get(),
                (be, side) -> side == be.facing() ? null : be.energy());
    }
}
