package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;

/** A survey or display machine that is not driven by a shaft (the cameras, the display and the projector): it ticks, and may hold items. */
public abstract class SurveyBlockEntity extends BlockEntity {
    protected SurveyBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Once per server tick. */
    public abstract void serverTick();

    /** What it holds, which is dropped when it is broken (null if nothing). */
    public ItemStackHandler items() {
        return null;
    }
}
