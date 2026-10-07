package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.HangingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Pile Driver (TileEntityPileDriver): a hammer that drives a pile down into the ground below it. Each stroke smashes the layer under the pile, a
 * five by five square without its corners; stone becomes cobblestone, most things break, obsidian takes five hits, and the shock breaks netherrack,
 * glowstone, glass and wool a few layers deeper. When the layer is clear it lays a length of pile there and goes on down. A stroke also kills what is in
 * the hole (not players), throws everything within 24 blocks into the air, makes players within 15 blocks sick, and breaks glass, glowstone, plants,
 * webs, ice and hanging things within five blocks. Power comes in at either end, it needs 80 kN*m, and 16 kW for every metre of pile plus one.
 * The more power over that, the faster it strikes. Every block it breaks is checked against claims as its owner; it is off unless the server enables it.
 */
public class PileDriverBlockEntity extends ConsumerBlockEntity implements Owned {
    public static final int MIN_TORQUE = 80_000;
    public static final int BASE_POWER = 16_384;
    public static final int BASE_SPEED = 300;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(MIN_TORQUE, 1, BASE_POWER);

    private int step;
    private int tickCount;
    private final Map<BlockPos, Integer> hits = new HashMap<>();
    @Nullable
    private WorldGuard.Owner owner;

    public PileDriverBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.PILE_DRIVER.type().get(), pos, state);
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

    /** How many layers of pile it has laid. */
    public int depth() {
        return step;
    }

    /** The power it needs for the next stroke: 16 kW for the length of pile it holds and the one it is driving. */
    public long powerNeeded() {
        return (long) BASE_POWER * (step + 1);
    }

    @Override
    public boolean hasEnoughPower() {
        return torque >= MIN_TORQUE && (long) torque * omega >= powerNeeded();
    }

    /** The ticks between strokes: 300, less the more the power is over what it needs, but never under one. */
    public int strokeTime() {
        long power = (long) torque * omega;
        long needed = powerNeeded();
        return power > needed ? (int) Math.max(BASE_SPEED / Math.max(1, power / needed), 1) : BASE_SPEED;
    }

    /** It takes power at either end. */
    @Override
    protected IShaftPowerOutput.Reading readInput() {
        if (level == null) {
            return IShaftPowerOutput.Reading.NONE;
        }
        IShaftPowerOutput.Reading a = IShaftPowerOutput.readInput(level, worldPosition, facing());
        IShaftPowerOutput.Reading b = IShaftPowerOutput.readInput(level, worldPosition, facing().getOpposite());
        return (long) a.torque() * a.omega() >= (long) b.torque() * b.omega() ? a : b;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("pileDriver")) {
            return;
        }
        if (++tickCount < strokeTime()) {
            return;
        }
        tickCount = 0;
        stroke(server);
    }

    // ---- what blocks do ----

    /** How many hits a block takes: positive for hard blocks; negative for those the shock of a stroke breaks that many layers down; zero for one hit. */
    public static int hitsFor(BlockState state) {
        if (state.is(Blocks.OBSIDIAN)) {
            return 5;
        }
        if (state.is(Blocks.NETHERRACK)) {
            return -2;
        }
        if (state.is(Blocks.GLASS)) {
            return -4;
        }
        if (state.is(Blocks.GLOWSTONE)) {
            return -3;
        }
        if (state.is(BlockTags.WOOL)) {
            return -1;
        }
        return 0;
    }

    /** What a smashed block turns into: stone to cobblestone, bricks to cracked bricks, bedrock stays; null for nothing (it breaks and drops). */
    @Nullable
    public static BlockState productOf(BlockState state) {
        if (state.is(Blocks.BEDROCK)) {
            return state;
        }
        if (state.is(Blocks.STONE)) {
            return Blocks.COBBLESTONE.defaultBlockState();
        }
        if (state.is(Blocks.STONE_BRICKS)) {
            return Blocks.CRACKED_STONE_BRICKS.defaultBlockState();
        }
        if (!state.getFluidState().isEmpty()) {
            return state;
        }
        return null;
    }

    private boolean breakable(ServerLevel server, BlockPos pos, BlockState state) {
        if (state.isAir() || state.is(Blocks.BEDROCK)) {
            return false;
        }
        if (state.getDestroySpeed(server, pos) < 0 && state.getFluidState().isEmpty()) {
            return false;
        }
        return MachineGuard.mayChange(server, pos, owner);
    }

    private static boolean corner(int i, int j) {
        return i * j == 4 || i * j == -4;
    }

    private void hit(ServerLevel server, BlockPos pos, BlockState state) {
        Integer count = hits.get(pos);
        boolean breaks;
        if (count != null) {
            if (count <= 1) {
                hits.remove(pos);
                breaks = true;
            } else {
                hits.put(pos, count - 1);
                breaks = false;
            }
        } else {
            int needed = hitsFor(state);
            if (needed <= 0) {
                breaks = true;
            } else {
                hits.put(pos, needed - 1);
                breaks = needed - 1 <= 0;
                if (breaks) {
                    hits.remove(pos);
                }
            }
        }
        if (!breaks) {
            return;
        }
        BlockState product = productOf(state);
        if (product == null) {
            BlockEntity be = server.getBlockEntity(pos);
            if (be != null) {
                return;
            }
            Block.dropResources(state, server, pos, null);
            server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        } else if (product != state) {
            server.setBlock(pos, product, 3);
        }
    }

    /** One stroke at the layer under the pile: smashes it, and advances when it is clear. */
    private void stroke(ServerLevel server) {
        int y = worldPosition.getY() - step - 1;
        if (step >= MachineConfig.get(MachineConfig.PILE_DRIVER_DEPTH) || y <= server.getMinBuildHeight() + 1) {
            return;
        }
        BlockPos center = new BlockPos(worldPosition.getX(), y, worldPosition.getZ());
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                if (corner(i, j)) {
                    continue;
                }
                BlockPos at = center.offset(i, 0, j);
                BlockState state = server.getBlockState(at);
                if (!breakable(server, at, state)) {
                    continue;
                }
                if (state.is(Blocks.SPAWNER)) {
                    saveSpawner(server, at);
                    continue;
                }
                for (int h = 1; h <= 4; h++) {
                    BlockPos deeper = at.below(h);
                    BlockState below = server.getBlockState(deeper);
                    int needed = hitsFor(below);
                    if (needed < 0 && -needed >= h && breakable(server, deeper, below)) {
                        hit(server, deeper, below);
                    }
                }
                hit(server, at, state);
            }
        }
        server.playSound(null, worldPosition, MachineSoundRegistry.get("piledriver").get(), SoundSource.BLOCKS, 1F, 1F);
        boolean cleared = true;
        for (int i = -2; i <= 2 && cleared; i++) {
            for (int j = -2; j <= 2; j++) {
                BlockState state = server.getBlockState(center.offset(i, 0, j));
                if (!corner(i, j) && !state.isAir() && state.getFluidState().isEmpty()) {
                    cleared = false;
                    break;
                }
            }
        }
        hurtAndThrow(server, center);
        breakFragile(server, center);
        if (cleared) {
            splash(server, center);
            if (MachineGuard.mayChange(server, center, owner)) {
                server.setBlock(center, DecorRegistry.PILE_PIPE.get().defaultBlockState(), 3);
                step++;
                setChanged();
            }
        }
    }

    private void saveSpawner(ServerLevel server, BlockPos at) {
        BlockEntity be = server.getBlockEntity(at);
        if (be == null) {
            return;
        }
        ItemStack stack = new ItemStack(Items.SPAWNER);
        BlockItem.setBlockEntityData(stack, be.getType(), be.saveWithoutMetadata(server.registryAccess()));
        server.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        server.addFreshEntity(new ItemEntity(server, at.getX() + 0.5, at.getY() + 0.5, at.getZ() + 0.5, stack));
    }

    /** Kills what is in the hole (not players), throws up everything near, and sickens the players near. */
    private void hurtAndThrow(ServerLevel server, BlockPos center) {
        AABB hole = new AABB(center).inflate(0.5, 2, 0.5);
        for (LivingEntity living : server.getEntitiesOfClass(LivingEntity.class, hole)) {
            if (!(living instanceof Player) && MachineGuard.mayChange(server, living.blockPosition(), owner)) {
                living.hurt(server.damageSources().inWall(), Float.MAX_VALUE);
            }
        }
        AABB zone = new AABB(center).inflate(24);
        for (Entity entity : server.getEntitiesOfClass(Entity.class, zone)) {
            if (entity.onGround()) {
                double dist = Math.sqrt(entity.position().distanceToSqr(Vec3.atCenterOf(center)));
                entity.setDeltaMovement(entity.getDeltaMovement().add(0, 0.5 / Math.sqrt(Math.max(dist, 1)), 0));
                entity.hurtMarked = true;
            }
        }
        for (Player player : server.getEntitiesOfClass(Player.class, new AABB(center).inflate(15))) {
            if (!player.isCreative()) {
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 150, 10));
            }
        }
    }

    /** Glass, glowstone, plants, webs and ice within five blocks of the stroke, and anything hanging on a wall there. */
    private void breakFragile(ServerLevel server, BlockPos center) {
        int range = 5;
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-range, -range, -range), center.offset(range, range, range))) {
            BlockState state = server.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }
            boolean glass = state.is(Blocks.GLASS) || state.is(Blocks.GLASS_PANE) || state.is(Blocks.GLOWSTONE);
            boolean plant = state.is(Blocks.CACTUS) || state.is(Blocks.SUGAR_CANE) || state.is(Blocks.VINE) || state.is(Blocks.LILY_PAD)
                    || state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(BlockTags.SAPLINGS) || state.is(Blocks.FLOWER_POT)
                    || state.getBlock() instanceof net.minecraft.world.level.block.AbstractSkullBlock;
            boolean web = state.is(Blocks.COBWEB);
            boolean ice = state.is(Blocks.ICE);
            boolean falls = state.getBlock() instanceof FallingBlock;
            if (!(glass || plant || web || ice || falls)) {
                continue;
            }
            BlockPos at = pos.immutable();
            if (falls) {
                server.scheduleTick(at, state.getBlock(), 2);
                continue;
            }
            if (!MachineGuard.mayChange(server, at, owner)) {
                continue;
            }
            if (ice) {
                Block.dropResources(state, server, at, null);
                server.setBlock(at, Blocks.WATER.defaultBlockState(), 3);
                continue;
            }
            if (web) {
                server.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
                Block.popResource(server, at, new ItemStack(Items.STRING));
                continue;
            }
            Block.dropResources(state, server, at, server.getBlockEntity(at));
            server.setBlock(at, Blocks.AIR.defaultBlockState(), 3);
        }
        for (HangingEntity hanging : server.getEntitiesOfClass(HangingEntity.class, new AABB(center).inflate(range))) {
            if (MachineGuard.mayChange(server, hanging.blockPosition(), owner)) {
                hanging.kill();
            }
        }
    }

    /** A splash or a hiss when the pile meets water or lava. */
    private void splash(ServerLevel server, BlockPos center) {
        for (int i = -2; i <= 2; i++) {
            for (int j = -2; j <= 2; j++) {
                FluidState fluid = server.getFluidState(center.offset(i, 0, j));
                if (!fluid.isEmpty() && !corner(i, j)) {
                    boolean lava = fluid.is(net.minecraft.tags.FluidTags.LAVA);
                    server.sendParticles(lava ? ParticleTypes.LAVA : ParticleTypes.SPLASH, center.getX() + 0.5, center.getY() + 1, center.getZ() + 0.5, 20, 0.5, 0.3, 0.5, 0.2);
                    server.playSound(null, center, lava ? SoundEvents.LAVA_EXTINGUISH : SoundEvents.PLAYER_SPLASH, SoundSource.BLOCKS, 1F, 1F);
                    return;
                }
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("step", step);
        tag.putInt("ticks", tickCount);
        if (owner != null) {
            tag.putUUID("owner_id", owner.id());
            tag.putString("owner_name", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        step = tag.getInt("step");
        tickCount = tag.getInt("ticks");
        owner = tag.hasUUID("owner_id") ? new WorldGuard.Owner(tag.getUUID("owner_id"), tag.getString("owner_name")) : null;
    }
}
