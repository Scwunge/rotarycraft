package net.scwunge.rotarycraft.api;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.bus.api.Event;

/** Posted on the NeoForge event bus each time a Bedrock Breaker grinds a block of bedrock all the way to dust (the original's API event). */
public class BedrockDigEvent extends Event {
    private final BlockEntity breaker;
    private final BlockPos pos;

    public BedrockDigEvent(BlockEntity breaker, BlockPos pos) {
        this.breaker = breaker;
        this.pos = pos;
    }

    public BlockEntity breaker() {
        return breaker;
    }

    /** The bedrock that was ground away. */
    public BlockPos pos() {
        return pos;
    }
}
