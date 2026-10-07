package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.weapon.turret.OmniConsumerBlockEntity;

/**
 * Item Refresher (TileEntityItemRefresher): keeps dropped items from despawning, in a cube round it: with 16 kW from any side its range is four blocks, and a
 * block more for each kilowatt over, up to 128. Any item within reach has its lifetime topped up so it is always twenty ticks (a second) from despawning, and
 * items lying still hop up, as the original's do.
 */
public class ItemRefresherBlockEntity extends OmniConsumerBlockEntity {
    public static final int MIN_POWER = 16384;
    public static final int FALLOFF = 1024;
    public static final int MAX_RANGE = 128;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);

    public ItemRefresherBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.ITEM_REFRESHER.type().get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public int range() {
        return (int) Math.min(MAX_RANGE, 4 + Math.max(0, getPower() - MIN_POWER) / FALLOFF);
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("itemRefresher")) {
            return;
        }
        int range = range();
        AABB box = new AABB(worldPosition).inflate(range);
        for (ItemEntity item : server.getEntitiesOfClass(ItemEntity.class, box)) {
            if (item.lifespan < item.getAge() + 20) {
                item.lifespan = item.getAge() + 20;
            }
            if (item.onGround() && item.getDeltaMovement().y <= 0) {
                item.setDeltaMovement(item.getDeltaMovement().x, 0.4, item.getDeltaMovement().z);
            }
        }
    }
}
