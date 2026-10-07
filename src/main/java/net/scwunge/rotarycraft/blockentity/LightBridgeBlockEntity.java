package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.block.BridgeBlock;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Light Bridge (TileEntityLightBridge): lays a walkable beam of light across gaps, one block more each tick, as far as its power reaches (range is
 * power over 32 MW times the limit) or until something solid is in the way. It needs light level 13 or more on the block above it, the original's
 * stand-in for sunlight, and the beam is gone when it loses power or light or is broken. The beam is built as the machine's owner, so claims can
 * stop it.
 */
public class LightBridgeBlockEntity extends ConsumerBlockEntity implements Owned, MachineInteractions {
    /** Power at which the range is the limit, from the original's table. */
    public static final long FULL_POWER = 33_554_432L;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1);

    private int grown;
    @Nullable
    private WorldGuard.Owner owner;

    public LightBridgeBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.LIGHT_BRIDGE.type().get(), pos, state);
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

    public static int limit() {
        return Math.max(64, MachineConfig.get(MachineConfig.BRIDGE_RANGE));
    }

    /** The length the power allows. */
    public int range() {
        return (int) Math.min(limit(), (long) torque * omega * limit() / FULL_POWER);
    }

    /** How long the beam is now. */
    public int length() {
        return grown;
    }

    private static boolean soft(BlockState state) {
        return state.isAir() || state.canBeReplaced() || state.is(Blocks.LIGHT) || state.is(DecorRegistry.BEAM.get()) || state.is(DecorRegistry.BRIDGE.get());
    }

    private BlockState bridge() {
        return DecorRegistry.BRIDGE.get().defaultBlockState().setValue(BridgeBlock.AXIS, facing().getAxis());
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        int range = range();
        boolean lit = server.getMaxLocalRawBrightness(worldPosition.above()) >= 13;
        if (!MachineConfig.enabled("lightBridge") || range <= 0 || !lit || facing().getAxis() == Direction.Axis.Y) {
            if (grown > 0) {
                lightsOut(server);
            }
            return;
        }
        Direction dir = facing();
        BlockState bridge = bridge();
        int reach = Math.min(grown + 1, range);
        for (int i = 1; i <= reach; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            BlockState state = server.getBlockState(at);
            if (!soft(state)) {
                reach = i - 1;
                break;
            }
            if (state != bridge) {
                if (!MachineGuard.mayChange(server, at, owner)) {
                    reach = i - 1;
                    break;
                }
                server.setBlock(at, bridge, 3);
            }
        }
        // a shorter range or a block that has come into the way takes the end of the beam back
        for (int i = reach + 1; i <= grown; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            if (server.isLoaded(at) && server.getBlockState(at).is(DecorRegistry.BRIDGE.get())) {
                server.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        if (reach != grown) {
            grown = reach;
            setChanged();
        }
    }

    /** Takes the beam away: every bridge block in a line from it. */
    public void lightsOut(ServerLevel server) {
        Direction dir = facing();
        for (int i = 1; i <= limit(); i++) {
            BlockPos at = worldPosition.relative(dir, i);
            if (!server.isLoaded(at)) {
                break;
            }
            BlockState state = server.getBlockState(at);
            if (state.is(DecorRegistry.BRIDGE.get()) && state.getValue(BridgeBlock.AXIS) == dir.getAxis()) {
                server.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
            }
        }
        grown = 0;
        setChanged();
    }

    @Override
    public void onBroken(ServerLevel server) {
        lightsOut(server);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("grown", grown);
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        grown = tag.getInt("grown");
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }
}
