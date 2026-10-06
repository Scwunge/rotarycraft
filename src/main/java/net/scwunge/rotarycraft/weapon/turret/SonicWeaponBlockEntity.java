package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.InfestedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.SonicMenu;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Sonic Weapon, as the original: needs 262 kW (from any side) and a volume set in decibels on its screen, held to what its torque
 * can make. Everything alive within 16 blocks feels the sound, weaker with the square of the distance (players in creative or
 * wearing a helmet are spared): blindness, then confusion and slowness (animals wander, mobs turn on each other), then drowned
 * lungs, and at the loudest the sound kills outright. It also shakes silverfish out of infested stone.
 */
public class SonicWeaponBlockEntity extends OmniConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 262144);
    public static final int RANGE = 16;
    /** The original's thresholds, in its intensity units. */
    public static final long EYE_DAMAGE = 1_807_500;
    public static final long BRAIN_DAMAGE = 3_906_200;
    public static final long LUNG_DAMAGE = 2_971_000;
    public static final long LETHAL = 100_000_000;
    public static final long SILVERFISH_KILL = 400_000;
    private static final long INTENSITY_PER_TORQUE = 1_000_000L * LETHAL / 262_144;

    private int decibels;
    private int ticks;
    @Nullable
    private WorldGuard.Owner owner;

    public SonicWeaponBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.SONIC_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public int decibels() {
        return decibels;
    }

    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public void setDecibels(int dB) {
        decibels = Math.max(0, Math.min(dB, 999));
        setChanged();
        syncNow();
    }

    /** The loudest it can be at this torque, in the original's units. */
    public double maxVolume() {
        return (double) INTENSITY_PER_TORQUE * torque;
    }

    /** The volume it makes: what is set, held to the maximum, in the units the thresholds use. */
    public double volume() {
        return Math.min(Math.pow(10, decibels / 10D), maxVolume()) / 1_000_000D;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered || !RotaryConfig.weaponEnabled("sonicWeapon")) {
            return;
        }
        applyEffects((ServerLevel) level);
        if (++ticks >= 10) {
            ticks = 0;
            level.playSound(null, worldPosition, WeaponRegistry.SONIC_SOUND.get(), SoundSource.BLOCKS, 1, 1);
        }
    }

    private void applyEffects(ServerLevel server) {
        double volume = volume();
        for (LivingEntity e : server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition).inflate(RANGE))) {
            if (e instanceof Player p && !isVulnerable(p)) {
                continue;
            }
            double dx = e.getX() - worldPosition.getX() - 0.5;
            double dy = e.getY() - worldPosition.getY() - 0.5;
            double dz = e.getZ() - worldPosition.getZ() - 0.5;
            double intensity = volume / Math.max(1e-6, dx * dx + dy * dy + dz * dz);
            if (intensity >= EYE_DAMAGE) {
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0));
            }
            if (intensity >= BRAIN_DAMAGE) {
                confuse(server, e);
            }
            if (intensity >= LUNG_DAMAGE && server.random.nextInt(40) == 0) {
                e.hurt(server.damageSources().drown(), 1);
            }
            if (intensity >= LETHAL) {
                e.hurt(server.damageSources().genericKill(), Float.MAX_VALUE);
            }
        }
        if (volume >= SILVERFISH_KILL) {
            shakeSilverfish(server, volume);
        }
    }

    /** Players in creative or wearing a helmet (the original's ear protection) are spared. */
    public static boolean isVulnerable(Player player) {
        return !player.isCreative() && !player.isSpectator() && player.getItemBySlot(EquipmentSlot.HEAD).isEmpty();
    }

    private void confuse(ServerLevel server, LivingEntity e) {
        e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 10));
        e.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 20, 3));
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20, 1));
        if (e instanceof Animal animal && animal.getNavigation().isDone()) {
            animal.getNavigation().moveTo(animal.getX() - 8 + server.random.nextInt(17), animal.getY(), animal.getZ() - 8 + server.random.nextInt(17), 0.2);
        }
        if (e instanceof Mob mob && e instanceof Enemy) {
            Mob other = server.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(10), m -> m != mob && m instanceof Enemy).stream().findFirst().orElse(null);
            if (other != null) {
                mob.setTarget(other);
                mob.setLastHurtByMob(other);
            }
        }
    }

    /** Infested blocks within reach of the sound turn back into plain stone, and the silverfish in them die. */
    private void shakeSilverfish(ServerLevel server, double volume) {
        if (!RotaryConfig.get(RotaryConfig.WEAPON_BLOCK_DAMAGE)) {
            return;
        }
        int range = (int) Math.min(20, 6D * volume / SILVERFISH_KILL);
        for (int i = 0; i < range; i++) {
            BlockPos at = worldPosition.offset(server.random.nextInt(2 * range + 1) - range, server.random.nextInt(2 * range + 1) - range,
                    server.random.nextInt(2 * range + 1) - range);
            if (server.getBlockState(at).getBlock() instanceof InfestedBlock infested && WorldGuard.setBlock(server, at, infested.getHostBlock().defaultBlockState(), owner)) {
                ExperienceOrb.award(server, at.getCenter(), 5);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new SonicMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("decibels", decibels);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        decibels = tag.getInt("decibels");
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("decibels", decibels);
        writePower(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        decibels = tag.getInt("decibels");
        readPower(tag);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
