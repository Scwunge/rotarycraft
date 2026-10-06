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

    public RotaryCraft(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.SERVER, RotaryConfig.SPEC, "rotarycraft-server.toml");
        RotaryBlocks.BLOCKS.register(modBus);
        RotaryItems.ITEMS.register(modBus);
        RotaryItems.TABS.register(modBus);
        RotaryBlockEntities.TYPES.register(modBus);
        modBus.addListener(RotaryCraft::registerCapabilities);
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RotaryBlockEntities.GENERATOR.get(),
                (be, side) -> side == be.inputSide() ? null : be.energy());
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, RotaryBlockEntities.STEAM_ENGINE.get(),
                (be, side) -> side == be.facing() ? null : be.water());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, RotaryBlockEntities.ELECTRIC_MOTOR.get(),
                (be, side) -> side == be.facing() ? null : be.energy());
    }
}
