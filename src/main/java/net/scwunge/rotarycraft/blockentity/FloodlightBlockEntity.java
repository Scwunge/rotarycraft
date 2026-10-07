package net.scwunge.rotarycraft.blockentity;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Flood Light (TileEntityFloodlight): given 1 kW it fills the air in front of it with invisible light, as far as the first opaque block, up to
 * 64 blocks (the range setting may raise this). A fresnel lens (right-click with it, which the light keeps) spreads the light into a cone of at most
 * 24 blocks. Sneak with the screwdriver to make the light a visible beam. Refreshed on part of every sixteen ticks, like the original.
 */
public class FloodlightBlockEntity extends ConsumerBlockEntity implements Owned, MachineInteractions {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final int LENS_RANGE = 24;

    private boolean beamMode;
    private boolean fresnel;
    private final LongArrayList beam = new LongArrayList();
    private int lastRange = -1;
    private boolean wasLit;
    @Nullable
    private WorldGuard.Owner owner;

    public FloodlightBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.FLOODLIGHT.type().get(), pos, state);
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

    public boolean beamMode() {
        return beamMode;
    }

    public boolean fresnel() {
        return fresnel;
    }

    /** How many light blocks it keeps up. */
    public int beamLength() {
        return beam.size();
    }

    public static int maxRange() {
        return Math.max(64, MachineConfig.get(MachineConfig.FLOODLIGHT_RANGE));
    }

    public int maxRangeNow() {
        return fresnel ? LENS_RANGE : maxRange();
    }

    /** Blocks in front of it to the first opaque block, at most the most it can reach; nothing without the power. */
    public int range() {
        if (!hasEnoughPower()) {
            return 0;
        }
        int max = maxRangeNow();
        Direction dir = facing();
        for (int i = 1; i <= max; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            if (!level.isLoaded(at)) {
                return i;
            }
            BlockState state = level.getBlockState(at);
            if (!state.isAir() && state.isSolidRender(level, at)) {
                return i;
            }
        }
        return max;
    }

    private boolean isLight(BlockState state) {
        return state.is(Blocks.LIGHT) || state.is(DecorRegistry.BEAM.get());
    }

    private BlockState lightState() {
        return beamMode ? DecorRegistry.BEAM.get().defaultBlockState() : Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
    }

    private void tryAdd(ServerLevel server, BlockPos pos) {
        BlockState state = server.getBlockState(pos);
        if (server.isLoaded(pos) && (state.isAir() || isLight(state)) && MachineGuard.mayChange(server, pos, owner)) {
            beam.add(pos.asLong());
        }
    }

    /** Works out which blocks the light fills: the straight line to the range, or with a lens the widening square cone. */
    private void rebuild(ServerLevel server, int range) {
        beam.clear();
        if (range <= 0) {
            return;
        }
        Direction dir = facing();
        if (fresnel) {
            Direction d1 = dir.getAxis() == Direction.Axis.Y ? Direction.NORTH : Direction.UP;
            Direction d2 = dir.getAxis() == Direction.Axis.X ? Direction.SOUTH : Direction.EAST;
            for (int d = 1; d <= range; d++) {
                int w = (d - 1) / 3;
                if (d > 1) {
                    w++;
                }
                for (int a = -w; a <= w; a++) {
                    for (int b = -w; b <= w; b++) {
                        tryAdd(server, worldPosition.relative(dir, d).relative(d1, a).relative(d2, b));
                    }
                }
            }
        } else {
            for (int d = 1; d <= range; d++) {
                BlockPos pos = worldPosition.relative(dir, d);
                BlockState state = server.getBlockState(pos);
                if (!state.isAir() && !isLight(state)) {
                    break;
                }
                tryAdd(server, pos);
            }
        }
    }

    /** Puts out every light block of its own and forgets them. */
    public void lightsOut(ServerLevel server) {
        for (long packed : beam) {
            BlockPos pos = BlockPos.of(packed);
            if (server.isLoaded(pos) && isLight(server.getBlockState(pos))) {
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        beam.clear();
        lastRange = -1;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (fresnel) {
            beamMode = false;
        }
        boolean on = powered && MachineConfig.enabled("floodLight");
        if ((level.getGameTime() & 8) != 8) {
            return;
        }
        if (!on) {
            if (wasLit) {
                lightsOut(server);
                wasLit = false;
            }
            return;
        }
        wasLit = true;
        int range = range();
        if (range != lastRange) {
            lightsOut(server);
            rebuild(server, range);
            lastRange = range;
        }
        BlockState state = lightState();
        for (long packed : beam) {
            BlockPos pos = BlockPos.of(packed);
            if (server.isLoaded(pos) && server.getBlockState(pos).isAir()) {
                server.setBlock(pos, state, 2);
            }
        }
    }

    @Override
    public boolean onScrewdriver(UseOnContext context) {
        if (context.getPlayer() == null || !context.getPlayer().isShiftKeyDown() || fresnel) {
            return false;
        }
        beamMode = !beamMode;
        if (level instanceof ServerLevel server) {
            lightsOut(server);
        }
        setChanged();
        return true;
    }

    @Override
    public boolean onItemUse(ItemStack stack, Player player, InteractionHand hand) {
        if (fresnel || stack.getItem() != WeaponRegistry.PARTS.get("lens").get()) {
            return false;
        }
        fresnel = true;
        beamMode = false;
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        if (level instanceof ServerLevel server) {
            lightsOut(server);
        }
        setChanged();
        return true;
    }

    @Override
    public void onBroken(ServerLevel server) {
        lightsOut(server);
        if (fresnel) {
            Vec3 at = Vec3.atCenterOf(worldPosition);
            net.minecraft.world.Containers.dropItemStack(server, at.x, at.y, at.z, new ItemStack(WeaponRegistry.PARTS.get("lens").get()));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("beam_mode", beamMode);
        tag.putBoolean("lens", fresnel);
        tag.putLongArray("beam", beam.toLongArray());
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        beamMode = tag.getBoolean("beam_mode");
        fresnel = tag.getBoolean("lens");
        beam.clear();
        for (long l : tag.getLongArray("beam")) {
            beam.add(l);
        }
        lastRange = -1;
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }
}
