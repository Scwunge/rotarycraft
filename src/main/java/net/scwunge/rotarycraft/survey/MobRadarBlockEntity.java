package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mob Radar, as the original: fed from below with at least 8 kW it sees every creature within 8 blocks plus one block for each
 * 1024 W above that, to 256 blocks, at any height. Its screen draws them as faces on a map centred on it, north
 * up. Creatures are told apart by face, not by whether they are friendly.
 */
public class MobRadarBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final long MIN_POWER = 8192;
    public static final int FALLOFF = 1024;
    public static final int MAX_RANGE = 256;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    private static final int MAX_BLIPS = 200;
    private boolean on;

    public MobRadarBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.MOB_RADAR_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** It takes power from below only. */
    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, Direction.DOWN);
        setPower(in.torque(), in.omega());
        boolean now = getPower() >= MIN_POWER;
        if (now != on) {
            on = now;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** Whether the dish is turning (the client is told when this changes). */
    public boolean isOn() {
        return on;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("on", on);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        on = tag.getBoolean("on");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    protected void machineTick(boolean powered) {
    }

    /** How far it sees now. */
    public int range() {
        if (getPower() < MIN_POWER) {
            return 0;
        }
        return (int) Math.min(MAX_RANGE, 8 + (getPower() - MIN_POWER) / FALLOFF);
    }

    /** A creature on the map: offsets from the radar in blocks, and its face on the icon sheet. */
    public record Blip(int dx, int dz, int icon) {}

    public record Scan(int range, List<Blip> blips) {}

    /** Everything in range, nearest first. */
    public Scan scan() {
        int range = range();
        List<Blip> blips = new ArrayList<>();
        if (range > 0 && level != null) {
            AABB zone = new AABB(worldPosition.getX() - range, level.getMinBuildHeight(), worldPosition.getZ() - range,
                    worldPosition.getX() + 1 + range, level.getMaxBuildHeight(), worldPosition.getZ() + 1 + range);
            List<LivingEntity> found = level.getEntitiesOfClass(LivingEntity.class, zone);
            found.sort(java.util.Comparator.comparingDouble(e -> e.distanceToSqr(worldPosition.getCenter())));
            for (LivingEntity e : found) {
                if (blips.size() >= MAX_BLIPS) {
                    break;
                }
                int dx = (int) Math.floor(100 * (e.getX() - worldPosition.getX() - 0.5) / range);
                int dz = (int) Math.floor(100 * (e.getZ() - worldPosition.getZ() - 0.5) / range);
                blips.add(new Blip(Math.max(-100, Math.min(100, dx)), Math.max(-100, Math.min(100, dz)), icon(e.getType())));
            }
        }
        return new Scan(range, blips);
    }

    /** The face for a creature: the original's icon sheet is laid out by its (1.7) entity ids; -1 is a player. */
    public static int icon(EntityType<?> type) {
        if (type == EntityType.PLAYER) {
            return -1;
        }
        if (type == EntityType.CREEPER) return 50;
        if (type == EntityType.SKELETON || type == EntityType.STRAY || type == EntityType.BOGGED || type == EntityType.WITHER_SKELETON) return 51;
        if (type == EntityType.SPIDER) return 52;
        if (type == EntityType.GIANT) return 53;
        if (type == EntityType.ZOMBIE || type == EntityType.HUSK || type == EntityType.DROWNED || type == EntityType.ZOMBIE_VILLAGER) return 54;
        if (type == EntityType.SLIME) return 55;
        if (type == EntityType.GHAST) return 56;
        if (type == EntityType.ZOMBIFIED_PIGLIN || type == EntityType.PIGLIN || type == EntityType.PIGLIN_BRUTE) return 57;
        if (type == EntityType.ENDERMAN) return 58;
        if (type == EntityType.CAVE_SPIDER) return 59;
        if (type == EntityType.SILVERFISH || type == EntityType.ENDERMITE) return 60;
        if (type == EntityType.BLAZE) return 61;
        if (type == EntityType.MAGMA_CUBE) return 62;
        if (type == EntityType.ENDER_DRAGON) return 63;
        if (type == EntityType.WITHER) return 64;
        if (type == EntityType.BAT) return 65;
        if (type == EntityType.WITCH) return 66;
        if (type == EntityType.PIG || type == EntityType.HOGLIN || type == EntityType.ZOGLIN) return 90;
        if (type == EntityType.SHEEP) return 91;
        if (type == EntityType.COW) return 92;
        if (type == EntityType.CHICKEN) return 93;
        if (type == EntityType.SQUID || type == EntityType.GLOW_SQUID) return 94;
        if (type == EntityType.WOLF) return 95;
        if (type == EntityType.MOOSHROOM) return 96;
        if (type == EntityType.SNOW_GOLEM) return 97;
        if (type == EntityType.OCELOT || type == EntityType.CAT) return 98;
        if (type == EntityType.IRON_GOLEM) return 99;
        if (type == EntityType.HORSE || type == EntityType.DONKEY || type == EntityType.MULE || type == EntityType.SKELETON_HORSE
                || type == EntityType.ZOMBIE_HORSE || type == EntityType.LLAMA || type == EntityType.TRADER_LLAMA) return 100;
        if (type == EntityType.VILLAGER || type == EntityType.WANDERING_TRADER || type == EntityType.PILLAGER || type == EntityType.VINDICATOR
                || type == EntityType.EVOKER || type == EntityType.ILLUSIONER) return 120;
        return -1;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new RadarMenu(id, inventory, this);
    }
}
