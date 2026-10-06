package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.scwunge.rotarycraft.blockentity.FlywheelBlockEntity;
import net.scwunge.rotarycraft.power.FlywheelType;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;

public class FlywheelBlock extends MachineBlock {
    private final FlywheelType type;

    public FlywheelBlock(Properties props, FlywheelType type) {
        super(props, RotaryBlockEntities.FLYWHEEL, FlywheelBlockEntity::new);
        this.type = type;
    }

    public FlywheelType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new FlywheelBlock(p, type));
    }
}
