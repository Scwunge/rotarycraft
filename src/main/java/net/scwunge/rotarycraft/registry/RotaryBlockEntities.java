package net.scwunge.rotarycraft.registry;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.DynamometerBlockEntity;
import net.scwunge.rotarycraft.blockentity.EngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.blockentity.ShaftBlockEntity;

public class RotaryBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, RotaryCraft.MOD_ID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ShaftBlockEntity>> SHAFT = TYPES.register("shaft",
            () -> BlockEntityType.Builder.of(ShaftBlockEntity::new, RotaryBlocks.SHAFTS.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EngineBlockEntity>> DC_ENGINE = TYPES.register("dc_engine",
            () -> BlockEntityType.Builder.of(EngineBlockEntity::new, RotaryBlocks.DC_ENGINE.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GearboxBlockEntity>> GEARBOX = TYPES.register("gearbox",
            () -> BlockEntityType.Builder.of(GearboxBlockEntity::new, RotaryBlocks.GEARBOXES.values().stream().map(h -> (Block) h.get()).toArray(Block[]::new)).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<GeneratorBlockEntity>> GENERATOR = TYPES.register("generator",
            () -> BlockEntityType.Builder.of(GeneratorBlockEntity::new, RotaryBlocks.GENERATOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MotorBlockEntity>> ELECTRIC_MOTOR = TYPES.register("electric_motor",
            () -> BlockEntityType.Builder.of(MotorBlockEntity::new, RotaryBlocks.ELECTRIC_MOTOR.get()).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DynamometerBlockEntity>> DYNAMOMETER = TYPES.register("dynamometer",
            () -> BlockEntityType.Builder.of(DynamometerBlockEntity::new, RotaryBlocks.DYNAMOMETER.get()).build(null));
}
