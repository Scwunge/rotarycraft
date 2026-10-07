package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import net.scwunge.rotarycraft.weapon.turret.OmniConsumerBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * Self Destruct (TileEntitySelfDestruct): a dead man's switch. Power from any side keeps it quiet; the moment that power stops, it starts shelling the
 * ground around it, an explosion every tick at a random place within six blocks, thirty-two of them, and then a last great one of twelve at itself.
 * Power coming back in time stops it. Off unless the server enables it; every blast is checked against claims as its owner, and respects mobGriefing.
 */
public class SelfDestructBlockEntity extends OmniConsumerBlockEntity implements Owned {
    public static final int SPREAD = 6;
    public static final int BLASTS = 32;
    public static final float BLAST = 3F;
    public static final float FINAL_BLAST = 12F;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1);

    private boolean wasPowered;
    private int blasts;
    private boolean counting;
    @Nullable
    private WorldGuard.Owner owner;

    public SelfDestructBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.SELF_DESTRUCT.type().get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    /** Whether it is in the middle of going off: the block shrugs off blasts until it is done. */
    public boolean isCounting() {
        return counting;
    }

    public int blastsSoFar() {
        return blasts;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (!MachineConfig.enabled("selfDestruct")) {
            wasPowered = powered;
            counting = false;
            blasts = 0;
            return;
        }
        if (powered) {
            wasPowered = true;
            if (counting) {
                counting = false;
                blasts = 0;
                setChanged();
            }
            return;
        }
        if (!counting) {
            if (!wasPowered) {
                return;
            }
            counting = true;
            setChanged();
        }
        blast(server);
    }

    private void blast(ServerLevel server) {
        blasts++;
        double rx = worldPosition.getX() + 0.5 + server.random.nextInt(2 * SPREAD + 1) - SPREAD;
        double ry = worldPosition.getY() + 0.5 + server.random.nextInt(2 * SPREAD + 1) - SPREAD;
        double rz = worldPosition.getZ() + 0.5 + server.random.nextInt(2 * SPREAD + 1) - SPREAD;
        BlockPos at = BlockPos.containing(rx, ry, rz);
        if (MachineGuard.mayChange(server, at, owner)) {
            server.explode(null, rx, ry, rz, BLAST, true, Level.ExplosionInteraction.MOB);
        }
        for (int i = 0; i < 32; i++) {
            server.sendParticles(ParticleTypes.LAVA, rx + server.random.nextInt(7) - 3, ry + server.random.nextInt(7) - 3, rz + server.random.nextInt(7) - 3, 1, 0, 0, 0, 0);
        }
        if (blasts > BLASTS) {
            counting = false;
            server.playSound(null, worldPosition, net.scwunge.rotarycraft.registry.MachineSoundRegistry.get("massivebang").get(), net.minecraft.sounds.SoundSource.BLOCKS, 4F, 1F);
            if (MachineGuard.mayChange(server, worldPosition, owner)) {
                server.explode(null, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, FINAL_BLAST, true, Level.ExplosionInteraction.MOB);
            }
            server.removeBlock(worldPosition, false);
        }
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("was_powered", wasPowered);
        tag.putBoolean("counting", counting);
        tag.putInt("blasts", blasts);
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        wasPowered = tag.getBoolean("was_powered");
        counting = tag.getBoolean("counting");
        blasts = tag.getInt("blasts");
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }
}
