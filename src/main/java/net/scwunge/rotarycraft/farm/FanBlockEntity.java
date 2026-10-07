package net.scwunge.rotarycraft.farm;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.blockentity.CoolingFinBlockEntity;
import net.scwunge.rotarycraft.config.FarmConfig;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.FarmRegistry;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.weapon.WorldGuard;

import java.util.List;

/**
 * The Fan, as the original: a beam of moving air along the way it faces, reaching 8 blocks and one more for each 1024 W (2048 W with a diffuser
 * fitted) above its minimum, to a limit set in the config (never under 32), and stopped by solid blocks. It pushes what is in the beam (the
 * faster the shaft the harder), blows away webs, leaves, tall grass, snow and fire, and harvests ripe crops in a 3 wide strip of the beam;
 * fire beside the fan is carried along the beam, and cooling fins in the beam lose heat. A diffuser (right-click) widens the beam.
 * It breaks blocks as its owner, so claims and mobGriefing stop it, and the fire spreading also needs doFireTick.
 */
public class FanBlockEntity extends FarmBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final long MAX_POWER = 2097152;
    public static final int FALLOFF = 1024;
    public static final int FALLOFF_WIDE = 2048;
    public static final double AXIS_SPEED_CAP = 1;
    public static final double BASE_SPEED = 0.000125;
    public static final int WEB_SPEED = 256, LEAF_SPEED = 4096, GRASS_SPEED = 1024, FIRE_SPEED = 64, HARVEST_SPEED = 512;

    private boolean wideBlow;

    public FanBlockEntity(BlockPos pos, BlockState state) {
        super(FarmRegistry.FAN_BE.get(), pos, state);
    }

    @Override
    protected String switchName() {
        return "fan";
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public boolean isWide() {
        return wideBlow;
    }

    public boolean canUpgrade(ItemStack stack) {
        return !wideBlow && stack.is(RotaryParts.part("diffuser").get());
    }

    public void upgrade() {
        wideBlow = true;
        setChanged();
        if (level != null && !level.isClientSide) {
            syncNow();
        }
    }

    @Override
    public boolean wouldTake(ItemStack stack) {
        return canUpgrade(stack);
    }

    @Override
    public boolean interact(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (canUpgrade(stack)) {
            upgrade();
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            return true;
        }
        return false;
    }

    @Override
    public void dropContents() {
        if (wideBlow) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    new ItemStack(RotaryParts.part("diffuser").get()));
        }
    }

    public static int maxRange() {
        return Math.max(32, RotaryConfig.get(FarmConfig.FAN_RANGE));
    }

    /** How far the beam reaches with the power it has. */
    public int range() {
        long power = getPower();
        if (power < REQUIREMENT.minPower()) {
            return 0;
        }
        long extra = Math.min(power - REQUIREMENT.minPower(), MAX_POWER);
        return (int) Math.min(8 + extra / (wideBlow ? FALLOFF_WIDE : FALLOFF), maxRange());
    }

    /** Whether the block stops the beam: solid things, but not crops, sprinklers or the air. */
    private boolean isStoppedBy(BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (state.isAir() || isCrop(state)) {
            return false;
        }
        if (state.is(FarmRegistry.SPRINKLER.get()) || state.is(FarmRegistry.LAWN_SPRINKLER.get())) {
            return false;
        }
        return state.isSolidRender(level, pos) || !state.getCollisionShape(level, pos).isEmpty() && state.isSolid();
    }

    private static boolean isCrop(BlockState state) {
        return state.getBlock() instanceof CropBlock || state.getBlock() instanceof NetherWartBlock || state.is(BlockTags.CROPS);
    }

    /** The blocks the beam travels through, cut short at the first thing that stops it. */
    public int clippedRange() {
        int range = range();
        Direction dir = facing();
        for (int i = 1; i <= range; i++) {
            if (isStoppedBy(worldPosition.relative(dir, i))) {
                return i;
            }
        }
        return range;
    }

    /** The box the beam fills, out to {@code range} blocks. */
    public AABB beamBox(int range) {
        Direction dir = facing();
        AABB box = new AABB(worldPosition).expandTowards(dir.getStepX() * (double) range, dir.getStepY() * (double) range, dir.getStepZ() * (double) range);
        return wideBlow ? box.inflate(dir.getAxis() == Direction.Axis.X ? 0 : 1, dir.getAxis() == Direction.Axis.Y ? 0 : 1, dir.getAxis() == Direction.Axis.Z ? 0 : 1) : box;
    }

    @Override
    protected void machineTick(boolean powered) {
        long time = level.getGameTime();
        if (!powered) {
            return;
        }
        int range = clippedRange();
        if (range <= 0) {
            return;
        }
        blowEntities(range);
        clearBlocks(range);
        if (time % 20 == 0) {
            spreadFire(range);
        }
    }

    private static double mass(Entity entity) {
        double volume = entity.getBbWidth() * entity.getBbWidth() * entity.getBbHeight();
        double mass = Math.max(0.05, volume * 100);
        if (entity instanceof LivingEntity living) {
            mass += living.getArmorValue() * 0.5;
        }
        return mass;
    }

    private void blowEntities(int range) {
        Direction dir = facing();
        long power = Math.min(getPower(), MAX_POWER);
        List<Entity> caught = level.getEntitiesOfClass(Entity.class, beamBox(range), e -> !e.isSpectator() && !e.isShiftKeyDown());
        for (Entity e : caught) {
            double mass = mass(e);
            Vec3 motion = e.getDeltaMovement();
            double[] m = {motion.x, motion.y, motion.z};
            double[] origin = {e.getX() - worldPosition.getX() - 0.5, e.getY() - worldPosition.getY(), e.getZ() - worldPosition.getZ() - 0.5};
            int[] step = {dir.getStepX(), dir.getStepY(), dir.getStepZ()};
            for (int axis = 0; axis < 3; axis++) {
                if (step[axis] == 0 || Math.abs(m[axis]) >= AXIS_SPEED_CAP) {
                    continue;
                }
                double d = origin[axis];
                if (d == 0) {
                    d = 1;
                }
                double multiplier = 1 / (d - maxRange());
                if (d - maxRange() > 12) {
                    multiplier = 0;
                }
                if (multiplier > 1 || multiplier < 0) {
                    multiplier = 1;
                }
                double base = multiplier * power * BASE_SPEED * (wideBlow ? 0.125 : 1);
                double speed = Math.max(Math.abs(Math.abs(m[axis]) + base / (mass * Math.abs(d))), AXIS_SPEED_CAP);
                m[axis] = step[axis] * speed;
            }
            e.setDeltaMovement(m[0], m[1], m[2]);
            e.hurtMarked = true;
            e.fallDistance = 0;
        }
    }

    private int harvestingRand() {
        return Math.max(50, 600 - 25 * (31 - Integer.numberOfLeadingZeros(Math.max(1, omega))));
    }

    private void clearBlocks(int range) {
        Direction dir = facing();
        ServerLevel server = server();
        for (int i = 1; i <= range; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            rip(server, at);
            enhanceFin(server, at);
            if (dir.getAxis() == Direction.Axis.Y) {
                for (int a = -1; a <= 1; a++) {
                    for (int b = -1; b <= 1; b++) {
                        rip(server, new BlockPos(worldPosition.getX() + a, worldPosition.getY() + i * dir.getStepY(), worldPosition.getZ() + b));
                    }
                }
            } else {
                Direction left = dir.getCounterClockWise();
                for (int a = -1; a <= 1; a++) {
                    for (int up = 0; up <= 2; up++) {
                        rip(server, at.relative(left, a).above(up));
                    }
                }
            }
        }
    }

    private void enhanceFin(ServerLevel server, BlockPos pos) {
        if (server.getBlockEntity(pos) instanceof CoolingFinBlockEntity fin) {
            server.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1, 0.3, 0.3, 0.3, 0.01);
            if (server.getGameTime() % 20 == 0 && fin.getTemperature() > Ambient.temperature(server, pos)) {
                fin.addTemperature(-(int) Math.min(10, 1 + getPower() / 32768));
            }
        }
    }

    private static boolean isGrass(BlockState state) {
        return state.is(Blocks.SHORT_GRASS) || state.is(Blocks.TALL_GRASS) || state.is(Blocks.FERN) || state.is(Blocks.LARGE_FERN);
    }

    /** The original's per-block effect: tear loose webs, leaves, grass, snow and fire, and harvest ripe crops, each by chance. */
    public void rip(ServerLevel server, BlockPos pos) {
        BlockState state = server.getBlockState(pos);
        if (state.isAir()) {
            return;
        }
        boolean crop = isCrop(state);
        boolean fire = state.is(BlockTags.FIRE);
        boolean tearable = state.is(Blocks.COBWEB) || state.is(BlockTags.LEAVES) || isGrass(state) || state.is(Blocks.SNOW) || fire;
        if (!tearable && !crop) {
            return;
        }
        int chance = harvestingRand();
        if (isGrass(state)) {
            chance /= 3;
        }
        chance = Math.max(1, chance);
        if (server.random.nextInt(chance) > 0) {
            return;
        }
        if (state.is(Blocks.COBWEB) && omega < WEB_SPEED || state.is(BlockTags.LEAVES) && omega < LEAF_SPEED || isGrass(state) && omega < GRASS_SPEED
                || (fire || state.is(Blocks.SNOW)) && omega < FIRE_SPEED || crop && omega < HARVEST_SPEED) {
            return;
        }
        if (crop) {
            harvest(server, pos, state);
            return;
        }
        WorldGuard.breakBlock(server, pos, owner, !fire);
    }

    private void harvest(ServerLevel server, BlockPos pos, BlockState state) {
        boolean ripe = state.getBlock() instanceof CropBlock crop ? crop.isMaxAge(state)
                : state.getBlock() instanceof NetherWartBlock && state.getValue(NetherWartBlock.AGE) >= 3;
        if (!ripe || !WorldGuard.mayChange(server, pos, owner)) {
            return;
        }
        List<ItemStack> drops = Block.getDrops(state, server, pos, server.getBlockEntity(pos));
        ItemStack seed = state.getBlock().getCloneItemStack(server, pos, state);
        boolean removedSeed = false;
        for (ItemStack drop : drops) {
            if (!removedSeed && drop.is(seed.getItem())) {
                drop.shrink(1);
                removedSeed = true;
            }
            if (!drop.isEmpty()) {
                server.addFreshEntity(new ItemEntity(server, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, drop));
            }
        }
        BlockState replanted = state.getBlock() instanceof CropBlock crop ? crop.getStateForAge(0) : state.setValue(NetherWartBlock.AGE, 0);
        server.setBlockAndUpdate(pos, replanted);
    }

    /** Fire or lava beside the fan is carried down the beam, to burn what is soft there. */
    private void spreadFire(int range) {
        ServerLevel server = server();
        if (!server.getGameRules().getBoolean(GameRules.RULE_DOFIRETICK)) {
            return;
        }
        boolean burning = false;
        for (Direction side : Direction.values()) {
            BlockState state = server.getBlockState(worldPosition.relative(side));
            if (state.is(BlockTags.FIRE) || state.is(Blocks.LAVA)) {
                burning = true;
                break;
            }
        }
        if (!burning) {
            return;
        }
        Direction dir = facing();
        for (int i = 1; i <= range; i++) {
            BlockPos base = worldPosition.relative(dir, i);
            for (BlockPos at : new BlockPos[] {base, base.above(), base.above(2)}) {
                if (server.random.nextInt(3) == 0 && server.getBlockState(at).isAir() && Blocks.FIRE.defaultBlockState().canSurvive(server, at)) {
                    WorldGuard.setBlock(server, at, Blocks.FIRE.defaultBlockState(), owner);
                }
            }
        }
    }

    @Override
    protected void writeData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("wide", wideBlow);
    }

    @Override
    protected void readData(CompoundTag tag, HolderLookup.Provider registries) {
        wideBlow = tag.getBoolean("wide");
    }

    @Override
    protected void writeClientData(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("wide", wideBlow);
    }

    @Override
    protected void readClientData(CompoundTag tag, HolderLookup.Provider registries) {
        wideBlow = tag.getBoolean("wide");
    }
}
