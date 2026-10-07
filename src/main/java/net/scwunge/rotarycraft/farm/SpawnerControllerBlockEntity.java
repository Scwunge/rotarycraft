package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;

import java.util.Optional;

/**
 * The Spawner Controller, as the original: set on top of a monster spawner, with 128 kW (from any side), it takes the spawner over. It sets a
 * delay between spawn cycles (up to 999 ticks, 800 to begin with, shortened by 40 ticks for each doubling of the shaft's speed, as far as
 * the delay the player asked for allows), lifts the spawner's need for a player nearby and its limit on creatures, and can be switched off from
 * its screen or by a redstone signal under it. What it spawns does not despawn. It will not spawn monsters on peaceful, nor more than
 * 99 of a kind within 16 by 24 blocks. Off by default in the farm config.
 */
public class SpawnerControllerBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 131072);
    public static final int BASE_DELAY = 800;
    public static final int MAX_DELAY = 999;
    public static final int TOGGLE = 0;
    public static final int SET_DELAY = 1000;
    public static final int LIMIT = 99;
    public static final String CONTROLLED_TAG = "ControllerSpawned";

    private boolean disabled;
    private int setDelay = BASE_DELAY;
    private int timer;
    /** The spawner's own player range, put back when the controller goes. */
    private int savedRange = -1;

    public SpawnerControllerBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.SPAWNER_CONTROLLER_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "spawnerController";
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
    public FarmUi ui() {
        return FarmUi.panel("spawner_controller", 75, -1);
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id == TOGGLE) {
            disabled = !disabled;
        } else if (id >= SET_DELAY && id <= SET_DELAY + MAX_DELAY) {
            setDelay = id - SET_DELAY;
            timer = Math.min(timer, Math.max(0, setDelay - 1));
        } else {
            return false;
        }
        setChanged();
        return true;
    }

    public boolean isDisabled() {
        return disabled;
    }

    public int setDelay() {
        return setDelay;
    }

    /** The time between spawn cycles the shaft's speed allows. */
    public int operationTime() {
        return Math.max(0, BASE_DELAY - 40 * (int) (Math.log(Math.max(1, omega)) / Math.log(2)));
    }

    /** The delay it works to: the one asked for, but no shorter than the shaft allows. */
    public int delay() {
        return Math.max(setDelay, operationTime());
    }

    public SpawnerBlockEntity spawner() {
        return level != null && level.getBlockEntity(worldPosition.below()) instanceof SpawnerBlockEntity s ? s : null;
    }

    public boolean isValidLocation() {
        return spawner() != null;
    }

    @Override
    protected int[] status() {
        return new int[] {delay(), disabled ? 1 : 0, isValidLocation() ? 1 : 0, setDelay};
    }

    @Override
    public void dropContents() {
        release();
    }

    private void release() {
        SpawnerBlockEntity spawner = spawner();
        if (spawner != null && savedRange >= 0) {
            setPlayerRange(spawner, savedRange);
        }
        savedRange = -1;
    }

    private void setPlayerRange(SpawnerBlockEntity spawner, int range) {
        CompoundTag tag = spawner.saveWithoutMetadata(level.registryAccess());
        tag.putShort("RequiredPlayerRange", (short) range);
        tag.putShort("MaxNearbyEntities", (short) Short.MAX_VALUE);
        spawner.loadWithComponents(tag, level.registryAccess());
        spawner.setChanged();
    }

    @Override
    protected void machineTick(boolean powered) {
        SpawnerBlockEntity spawner = spawner();
        if (level.getGameTime() % 20 == 0 && validClient != (spawner != null)) {
            validClient = spawner != null;
            syncNow();
        }
        if (spawner == null) {
            timer = 0;
            savedRange = -1;
            return;
        }
        if (!powered || setDelay <= 0) {
            return;
        }
        ServerLevel server = server();
        if (savedRange < 0) {
            savedRange = spawner.saveWithoutMetadata(server.registryAccess()).getShort("RequiredPlayerRange");
            setPlayerRange(spawner, 0);
        } else if (server.getGameTime() % 100 == 0) {
            setPlayerRange(spawner, 0);
        }
        if (disabled || server.hasNeighborSignal(worldPosition.below())) {
            timer = 0;
            return;
        }
        if (++timer >= delay()) {
            timer = 0;
            spawnCycle(server, spawner);
        }
    }

    private void spawnCycle(ServerLevel server, SpawnerBlockEntity spawner) {
        CompoundTag data = spawner.saveWithoutMetadata(server.registryAccess());
        CompoundTag entityTag = data.getCompound("SpawnData").getCompound("entity");
        Optional<EntityType<?>> type = EntityType.by(entityTag);
        if (type.isEmpty()) {
            return;
        }
        BlockPos at = worldPosition.below();
        if (server.getDifficulty() == Difficulty.PEACEFUL && type.get().getCategory() == MobCategory.MONSTER) {
            return;
        }
        int near = server.getEntitiesOfClass(Entity.class, new AABB(at).inflate(16, 24, 16), e -> e.getType() == type.get()).size();
        if (near >= LIMIT) {
            return;
        }
        int count = data.contains("SpawnCount") ? data.getShort("SpawnCount") : 4;
        int range = data.contains("SpawnRange") ? data.getShort("SpawnRange") : 4;
        for (int i = 0; i < Math.max(1, count); i++) {
            double x = at.getX() + 0.5 + (server.random.nextDouble() - server.random.nextDouble()) * range;
            double y = at.getY() + server.random.nextInt(3) - 1;
            double z = at.getZ() + 0.5 + (server.random.nextDouble() - server.random.nextDouble()) * range;
            if (!server.noCollision(type.get().getSpawnAABB(x, y, z))) {
                continue;
            }
            Entity entity = EntityType.loadEntityRecursive(entityTag, server, e -> {
                e.moveTo(x, y, z, server.random.nextFloat() * 360, 0);
                return e;
            });
            if (entity == null) {
                continue;
            }
            if (entity instanceof Mob mob) {
                if (!mob.checkSpawnObstruction(server)) {
                    continue;
                }
                if (entityTag.size() == 1 && entityTag.contains("id")) {
                    net.neoforged.neoforge.event.EventHooks.finalizeMobSpawn(mob, server, server.getCurrentDifficultyAt(entity.blockPosition()), MobSpawnType.SPAWNER, null);
                }
                mob.setPersistenceRequired();
            }
            entity.getPersistentData().putBoolean(CONTROLLED_TAG, true);
            server.addFreshEntityWithPassengers(entity);
            server.levelEvent(2004, at, 0);
            if (entity instanceof Mob mob) {
                mob.spawnAnim();
            }
        }
    }

    private boolean validClient;

    public boolean isValidClient() {
        return validClient;
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("valid", isValidLocation());
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        validClient = tag.getBoolean("valid");
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putInt("setDelay", setDelay);
        tag.putBoolean("disabled", disabled);
        tag.putInt("savedRange", savedRange);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        setDelay = tag.contains("setDelay") ? tag.getInt("setDelay") : BASE_DELAY;
        disabled = tag.getBoolean("disabled");
        savedRange = tag.contains("savedRange") ? tag.getInt("savedRange") : -1;
    }
}
