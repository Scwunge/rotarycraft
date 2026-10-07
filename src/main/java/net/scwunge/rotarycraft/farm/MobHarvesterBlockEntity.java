package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.scwunge.rotarycraft.blockentity.MachineEnchantments;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.weapon.WorldGuard;

import java.util.List;

/**
 * The Mob Harvester, as the original: 4 kW (from any side) makes it hurt every creature (never villagers or players) in a column above it, 2
 * blocks tall and a block more for each 512 kW to 6, with a beam. The harm grows with the power (6 x log2 of 2.5 and a half more for each 4096 W,
 * and 2 per level of sharpness). Books give it sharpness, fire aspect, silk touch (a creature now and then drops its head) and looting,
 * and creatures die to it as its owner would kill them, so what they drop is theirs to take. Machines with no owner do nothing. Off by default.
 */
public class MobHarvesterBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 4096);

    private final MachineEnchantments enchantments = new MachineEnchantments(Enchantments.INFINITY, Enchantments.SHARPNESS, Enchantments.FIRE_ASPECT,
            Enchantments.SILK_TOUCH, Enchantments.LOOTING);
    /** Whether the beam is on: there is a creature in it. */
    private boolean laser;

    public MobHarvesterBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.MOB_HARVESTER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "mobHarvester";
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
    public MachineEnchantments enchantments() {
        return enchantments;
    }

    public boolean isLaserOn() {
        return laser;
    }

    public int damage() {
        double pdiff = 2 + 0.5 * getPower() / REQUIREMENT.minPower();
        double base = 6 * Math.log(pdiff) / Math.log(2) + 2 * enchantLevel(Enchantments.SHARPNESS);
        return (int) base;
    }

    public int height() {
        return (int) Math.min(6, 2 + Math.max(0, getPower() / 524288));
    }

    /** The column the machine hurts things in. */
    public AABB box() {
        return new AABB(worldPosition.getX(), worldPosition.getY() + 1, worldPosition.getZ(), worldPosition.getX() + 1, worldPosition.getY() + height() + 1,
                worldPosition.getZ() + 1);
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            setLaser(false);
            return;
        }
        ServerLevel server = server();
        boolean any = false;
        List<Mob> inBox = server.getEntitiesOfClass(Mob.class, box());
        FakePlayer actor = owner == null ? null : WorldGuard.actor(server, owner);
        for (Mob mob : inBox) {
            if (mob instanceof AbstractVillager) {
                continue;
            }
            any = true;
            int dmg = damage();
            if (actor != null && dmg > 0) {
                actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, enchantments.tool(server.registryAccess()));
                actor.setPos(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
                mob.hurt(server.damageSources().playerAttack(actor), dmg);
                actor.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                if (enchantments.has(Enchantments.SILK_TOUCH) && server.random.nextInt(20) == 0) {
                    dropHead(server, mob);
                }
                if (enchantments.has(Enchantments.FIRE_ASPECT)) {
                    mob.igniteForSeconds(enchantments.level(Enchantments.FIRE_ASPECT) * 2);
                }
            }
            mob.setDeltaMovement(mob.getDeltaMovement().x, 0, mob.getDeltaMovement().z);
        }
        setLaser(any);
    }

    private void setLaser(boolean on) {
        if (laser != on) {
            laser = on;
            syncNow();
        }
    }

    private static void dropHead(ServerLevel server, LivingEntity mob) {
        ItemStack head = null;
        var type = mob.getType();
        if (type == EntityType.ZOMBIE) {
            head = new ItemStack(Items.ZOMBIE_HEAD);
        } else if (type == EntityType.SKELETON) {
            head = new ItemStack(Items.SKELETON_SKULL);
        } else if (type == EntityType.WITHER_SKELETON) {
            head = new ItemStack(Items.WITHER_SKELETON_SKULL);
        } else if (type == EntityType.CREEPER) {
            head = new ItemStack(Items.CREEPER_HEAD);
        } else if (type == EntityType.PIGLIN) {
            head = new ItemStack(Items.PIGLIN_HEAD);
        }
        if (head != null) {
            server.addFreshEntity(new ItemEntity(server, mob.getX(), mob.getY() + 0.5, mob.getZ(), head));
        }
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("laser", laser);
        tag.putInt("height", height());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        laser = tag.getBoolean("laser");
    }
}
