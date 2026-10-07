package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ambient.Bat;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Blaze;
import net.minecraft.world.entity.monster.Ghast;
import net.minecraft.world.entity.monster.Slime;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;

import java.util.List;

/**
 * The Bait Box, as the original: 32 kW (from any side), and baits in its 27 slots. Each kind of creature within its range (8 blocks, and one
 * more for each 4096 W above the minimum, to 24 unless the config says more) is drawn to the box if the box holds the thing it likes, or driven
 * off, dropping what it holds, if the box holds the thing it hates (see {@link MobBait}). Walkers are sent walking towards or away from it;
 * flyers, slimes and swimmers are pushed. Eggs beside fire hatch into chickens. It does not move creatures that are sitting.
 */
public class BaitBoxBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 32768);
    public static final int SLOTS = 27;
    public static final int FALLOFF = 4096;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    public BaitBoxBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.BAIT_BOX_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "baitBox";
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

    public static int maxRange() {
        return Math.max(24, RotaryConfig.get(FarmConfig.BAIT_RANGE));
    }

    public int range() {
        if (getPower() < REQUIREMENT.minPower()) {
            return 0;
        }
        return (int) Math.min(8 + (getPower() - REQUIREMENT.minPower()) / FALLOFF, maxRange());
    }

    @Override
    protected int[] status() {
        return new int[] {range()};
    }

    public boolean canAttract(LivingEntity entity) {
        return range() >= Math.sqrt(entity.distanceToSqr(worldPosition.getCenter())) && MobBait.attracts(entity, items) && !entity.isRemoved();
    }

    public boolean canRepel(LivingEntity entity) {
        return range() >= Math.sqrt(entity.distanceToSqr(worldPosition.getCenter())) && MobBait.repels(entity, items) && !entity.isRemoved();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            return;
        }
        ServerLevel server = server();
        int range = range();
        if ((server.getGameTime() & 3) == 0) {
            List<Mob> inBox = server.getEntitiesOfClass(Mob.class, new AABB(worldPosition).inflate(range));
            int limit = Math.max(24, RotaryConfig.get(FarmConfig.BAIT_MOBS));
            for (Mob mob : inBox.subList(0, Math.min(limit, inBox.size()))) {
                double d = Math.sqrt(mob.distanceToSqr(worldPosition.getCenter()));
                if (d > range) {
                    continue;
                }
                if (MobBait.repels(mob, items)) {
                    apply(server, mob, false);
                } else if (MobBait.attracts(mob, items)) {
                    apply(server, mob, true);
                }
            }
        }
        if (server.random.nextInt(20) == 0) {
            incubateEgg(server);
        }
    }

    /** An egg beside fire hatches a chicken; beside lava as well as fire, it is cooked and given back. */
    private void incubateEgg(ServerLevel server) {
        for (int slot = 0; slot < SLOTS; slot++) {
            if (items.getStackInSlot(slot).is(Items.EGG)) {
                boolean fire = false, lava = false;
                for (net.minecraft.core.Direction d : net.minecraft.core.Direction.values()) {
                    BlockState state = server.getBlockState(worldPosition.relative(d));
                    fire |= state.is(net.minecraft.tags.BlockTags.FIRE);
                    lava |= state.is(net.minecraft.world.level.block.Blocks.LAVA);
                }
                if (fire) {
                    if (!lava) {
                        var chicken = net.minecraft.world.entity.EntityType.CHICKEN.create(server);
                        if (chicken != null) {
                            chicken.moveTo(worldPosition.getX() + server.random.nextDouble(), worldPosition.getY() + server.random.nextDouble() + 0.5,
                                    worldPosition.getZ() + server.random.nextDouble(), server.random.nextFloat() * 360, 0);
                            server.addFreshEntity(chicken);
                        }
                    } else {
                        net.neoforged.neoforge.items.ItemHandlerHelper.insertItem(items, new ItemStack(Items.COOKED_CHICKEN), false);
                    }
                    items.extractItem(slot, 1, false);
                }
                return;
            }
        }
    }

    private void apply(ServerLevel server, Mob mob, boolean attract) {
        if (mob instanceof TamableAnimal pet && pet.isOrderedToSit()) {
            return;
        }
        long time = server.getGameTime();
        if (time - mob.getPersistentData().getLong("baitbox") < 20) {
            return;
        }
        mob.getPersistentData().putLong("baitbox", time);
        Vec3 centre = worldPosition.getCenter();
        Vec3 away = mob.position().subtract(centre);
        if (!attract) {
            dropHeldItem(server, mob);
        }
        if (mob instanceof Blaze || mob instanceof Slime || mob instanceof Ghast || mob instanceof Squid) {
            if (!(mob instanceof Slime) || !mob.onGround()) {
                double f = attract ? -0.02 : 0.02;
                double vy = !(mob instanceof Squid) || mob.isInWater() ? f * away.y : 0;
                double boost = mob instanceof Blaze ? 4 : 1;
                mob.setDeltaMovement(f * away.x * boost, vy, f * away.z * boost);
                mob.hurtMarked = true;
            }
        } else if (mob instanceof Bat) {
            double f = attract ? -0.1 : 0.1;
            mob.setDeltaMovement(f * away.x, f * away.y, f * away.z);
            mob.hurtMarked = true;
        } else if (attract) {
            mob.getNavigation().moveTo(centre.x, worldPosition.getY(), centre.z, 1.0);
        } else {
            Vec3 goal = mob.position().add(away.normalize().scale(8));
            mob.getNavigation().moveTo(goal.x, goal.y, goal.z, 1.2);
        }
    }

    private void dropHeldItem(ServerLevel server, Mob mob) {
        ItemStack held = mob.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }
        mob.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
        ItemEntity drop = new ItemEntity(server, mob.getX(), mob.getEyeY(), mob.getZ(), held);
        drop.setDeltaMovement(-0.2 + 0.4 * server.random.nextFloat(), 0.4 * server.random.nextFloat(), -0.2 + 0.4 * server.random.nextFloat());
        drop.setPickUpDelay(200);
        server.addFreshEntity(drop);
    }
}
