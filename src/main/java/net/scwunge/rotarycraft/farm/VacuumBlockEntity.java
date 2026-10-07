package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;

import java.util.List;

/**
 * The Item Vacuum, as the original: 16 kW (from any side) makes it pull dropped items and experience orbs from a cube round it (8 blocks
 * and one more for each 4096 W, to the configured limit, never under 32) to its middle, drawing harder on more power; what reaches
 * it goes into its 54 slots, or into its stored experience. It also empties the inventories beside it (one item from each slot, a whole
 * stack on 64 kW). When it is broken it spills its experience as orbs.
 */
public class VacuumBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 16384);
    public static final int SLOTS = 54;
    /** Watts of power for each metre of reach beyond 8: the configured one, rounded up to a power of two. */
    public static int falloff() {
        return Math.min(524288, Integer.highestOneBit(Math.max(1024, net.scwunge.rotarycraft.config.RotaryConfig.get(net.scwunge.rotarycraft.config.RotaryConfig.VACUUM_POWER_PER_METRE)) * 2 - 1));
    }

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int experience;
    private int tickCount;

    public VacuumBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.VACUUM_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "vacuum";
    }

    @Override
    protected boolean anySide() {
        return true;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    @Override
    public int menuRows() {
        return SLOTS / 9;
    }

    public int experience() {
        return experience;
    }

    public static int maxRange() {
        return Math.max(32, RotaryConfig.get(FarmConfig.VACUUM_RANGE));
    }

    public int range() {
        if (getPower() < REQUIREMENT.minPower()) {
            return 0;
        }
        return (int) Math.min(8 + getPower() / falloff(), maxRange());
    }

    @Override
    protected int[] status() {
        return new int[] {range(), experience};
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            return;
        }
        if (++tickCount < 2) {
            return;
        }
        tickCount = 0;
        ServerLevel server = server();
        suck(server);
        absorb(server);
        transfer(server);
    }

    private void suck(ServerLevel server) {
        AABB box = new AABB(worldPosition).inflate(range());
        double v = Math.max(1, getPower() / 1048576D);
        double x = worldPosition.getX() + 0.5, y = worldPosition.getY() + 0.5, z = worldPosition.getZ() + 0.5;
        for (Entity ent : server.getEntitiesOfClass(Entity.class, box, e -> e instanceof ItemEntity || e instanceof ExperienceOrb)) {
            if (ent.tickCount <= 5) {
                continue;
            }
            double dx = x - ent.getX(), dy = y - ent.getY(), dz = z - ent.getZ();
            double ddt = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (ddt < 1.0E-3) {
                continue;
            }
            if (ent.tickCount > 50 && ddt > 1.5 && ent.tickCount % 400 < 80) { // swirls round things in the way
                double t = server.getGameTime() / 25D;
                double r = 2.875;
                dx += r * Math.cos(t);
                dz += r * Math.sin(t);
            }
            double vmax = 0.125;
            double vx = Math.max(-vmax, Math.min(vmax, v * dx / ddt / ddt));
            double vy = Math.max(-vmax, Math.min(vmax, v * dy / ddt / ddt / 2));
            double vz = Math.max(-vmax, Math.min(vmax, v * dz / ddt / ddt));
            double extra = ent.getY() < worldPosition.getY() ? 0.125 : 0;
            ent.setDeltaMovement(ent.getDeltaMovement().add(vx, vy + extra, vz));
            ent.hurtMarked = true;
        }
    }

    private void absorb(ServerLevel server) {
        AABB close = new AABB(worldPosition).inflate(0.25);
        for (ItemEntity ent : server.getEntitiesOfClass(ItemEntity.class, close)) {
            if (ent.hasPickUpDelay()) {
                continue;
            }
            ItemStack stack = ent.getItem();
            ItemStack rest = ItemHandlerHelper.insertItem(items, stack, false);
            if (rest.getCount() == stack.getCount()) {
                continue;
            }
            if (rest.isEmpty()) {
                ent.discard();
            } else {
                ent.setItem(rest);
            }
            server.playSound(null, worldPosition, SoundEvents.ITEM_PICKUP, SoundSource.BLOCKS, 0.1F + 0.5F * server.random.nextFloat(), server.random.nextFloat());
        }
        for (ExperienceOrb orb : server.getEntitiesOfClass(ExperienceOrb.class, close)) {
            experience += orb.getValue();
            orb.discard();
            setChanged();
            server.playSound(null, worldPosition, SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.BLOCKS, 0.1F, 0.5F * ((server.random.nextFloat() - server.random.nextFloat()) * 0.7F + 1.8F));
        }
    }

    private void transfer(ServerLevel server) {
        boolean whole = getPower() / REQUIREMENT.minPower() >= 4;
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (server.getBlockEntity(worldPosition.relative(dir)) instanceof VacuumBlockEntity) {
                continue;
            }
            IItemHandler other = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(dir), dir.getOpposite());
            if (other == null) {
                continue;
            }
            for (int slot = 0; slot < other.getSlots(); slot++) {
                ItemStack here = other.getStackInSlot(slot);
                if (here.isEmpty()) {
                    continue;
                }
                ItemStack want = other.extractItem(slot, whole ? here.getCount() : 1, true);
                if (want.isEmpty()) {
                    continue;
                }
                ItemStack left = ItemHandlerHelper.insertItem(items, want, true);
                int moved = want.getCount() - left.getCount();
                if (moved > 0) {
                    ItemHandlerHelper.insertItem(items, other.extractItem(slot, moved, false), false);
                }
            }
        }
    }

    /** Spills the experience it holds as orbs. */
    public void spawnExperience() {
        if (experience > 0 && level instanceof ServerLevel server) {
            ExperienceOrb.award(server, worldPosition.getCenter(), experience);
            experience = 0;
        }
    }

    @Override
    public void dropContents() {
        super.dropContents();
        spawnExperience();
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("xp", experience);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        experience = tag.getInt("xp");
    }
}
