package net.scwunge.rotarycraft.registry;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.item.OreProduct;

public class RotaryComponents {
    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RotaryCraft.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<OreProduct>> ORE_PRODUCT = COMPONENTS.register("ore_product",
            () -> DataComponentType.<OreProduct>builder().persistent(OreProduct.CODEC).networkSynchronized(OreProduct.STREAM_CODEC).build());
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.scwunge.rotarycraft.item.GearboxState>> GEARBOX_STATE = COMPONENTS.register("gearbox_state",
            () -> DataComponentType.<net.scwunge.rotarycraft.item.GearboxState>builder().persistent(net.scwunge.rotarycraft.item.GearboxState.CODEC)
                    .networkSynchronized(net.scwunge.rotarycraft.item.GearboxState.STREAM_CODEC).build());
    /** A shaft core's magnetization in microtesla (Magnetizer charges it, the AC Engine uses it up). */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> MAGNETIZATION = COMPONENTS.register("magnetization",
            () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.intRange(1, Integer.MAX_VALUE))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT).build());
    /** What a Craft Pattern holds: its recipe, the kind of recipe and the input limit. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<net.scwunge.rotarycraft.crafting.CraftPattern>> CRAFT_PATTERN = COMPONENTS.register("craft_pattern",
            () -> DataComponentType.<net.scwunge.rotarycraft.crafting.CraftPattern>builder().persistent(net.scwunge.rotarycraft.crafting.CraftPattern.CODEC)
                    .networkSynchronized(net.scwunge.rotarycraft.crafting.CraftPattern.STREAM_CODEC).build());
}
