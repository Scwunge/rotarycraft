package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;

/**
 * Block Cannon (TileEntityBlockCannon): loaded with blocks, it throws one every half second, as a falling block, at the compass bearing, elevation and speed set on
 * its screen (speed in blocks a second), or in target mode at a block you name (it works out the speed and angle). Each block needs torque in proportion to how
 * heavy it is and how fast it is thrown: the next power of two above speed times density, divided by four, in N*m. It takes 65 kW from any side.
 * It is off unless the server enables it, and it will not fire a block that would land where its owner may not build.
 */
public class BlockCannonBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "block_cannon";
    public static final int SLOTS = 11;
    public static final int OPERATION_TIME = 10;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 65536);
    private static final int COORD = 30_000_000;
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(212, 236).inventoryAt(27, 155).grid(9, 133, SLOTS, 1)
            .field(154, 112, 28, 0, 1000, "gui.rotarycraft.block_cannon.velocity", 112, 116).shownWhen(6, 0)
            .field(31, 92, 28, 0, 90, "gui.rotarycraft.block_cannon.launch_angle", 12, 80).shownWhen(6, 0)
            .field(154, 92, 28, 0, 359, "gui.rotarycraft.block_cannon.compass_angle", 118, 80).shownWhen(6, 0)
            .field(38, 14, 52, -COORD, COORD, "gui.rotarycraft.block_cannon.x", 22, 18).shownWhen(6, 1)
            .field(38, 34, 52, -COORD, COORD, "gui.rotarycraft.block_cannon.y", 22, 38).shownWhen(6, 1)
            .field(38, 54, 52, -COORD, COORD, "gui.rotarycraft.block_cannon.z", 22, 58).shownWhen(6, 1)
            .button(0, 78, 30, 56, 20, 6).build();

    private int velocity;
    private int theta;
    private int phi;
    private boolean targetMode;
    private BlockPos target = BlockPos.ZERO;
    private int tickCount;
    private int solvedTheta;
    private int solvedVelocity;
    private long solvedFor = Long.MIN_VALUE;

    public BlockCannonBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.BLOCK_CANNON.type().get(), pos, state, SLOTS, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    protected boolean omniSided() {
        return true;
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    // ---- settings ----

    public int velocity() {
        return velocity;
    }

    public int theta() {
        return theta;
    }

    public int phi() {
        return phi;
    }

    public boolean targetMode() {
        return targetMode;
    }

    public BlockPos target() {
        return target;
    }

    public void aimAt(BlockPos pos) {
        target = pos;
        targetMode = true;
        solvedFor = Long.MIN_VALUE;
        setChanged();
    }

    public void aim(int velocity, int theta, int phi) {
        this.velocity = velocity;
        this.theta = theta;
        this.phi = phi;
        targetMode = false;
        setChanged();
    }

    @Override
    public boolean setField(Player player, int field, int value) {
        switch (field) {
            case 0 -> velocity = value;
            case 1 -> theta = value;
            case 2 -> phi = Math.floorMod(value, 360);
            case 3 -> target = new BlockPos(value, target.getY(), target.getZ());
            case 4 -> target = new BlockPos(target.getX(), value, target.getZ());
            case 5 -> target = new BlockPos(target.getX(), target.getY(), value);
            default -> {
                return false;
            }
        }
        solvedFor = Long.MIN_VALUE;
        setChanged();
        return true;
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id != 0) {
            return false;
        }
        targetMode = !targetMode;
        solvedFor = Long.MIN_VALUE;
        setChanged();
        return true;
    }

    @Override
    public int extra(int index) {
        return switch (index) {
            case 0 -> velocity;
            case 1 -> theta;
            case 2 -> phi;
            case 3 -> target.getX();
            case 4 -> target.getY();
            case 5 -> target.getZ();
            default -> targetMode ? 1 : 0;
        };
    }

    @Override
    protected int extraCount() {
        return 7;
    }

    // ---- what it throws ----

    /** Roughly how dense a block is, in kg/m3, as the original's table of densities. */
    public static int density(BlockState state) {
        if (state.is(Blocks.GOLD_BLOCK)) {
            return 19300;
        }
        if (state.is(Blocks.IRON_BLOCK)) {
            return 7870;
        }
        if (state.is(Blocks.COPPER_BLOCK)) {
            return 8960;
        }
        if (state.is(Blocks.NETHERITE_BLOCK)) {
            return 21000;
        }
        if (state.is(Blocks.DIAMOND_BLOCK)) {
            return 3510;
        }
        if (state.is(Blocks.EMERALD_BLOCK) || state.is(Blocks.LAPIS_BLOCK)) {
            return 2700;
        }
        if (state.is(Blocks.OBSIDIAN) || state.is(Blocks.CRYING_OBSIDIAN)) {
            return 2600;
        }
        if (state.is(BlockTags.WOOL) || state.is(BlockTags.LEAVES) || state.is(BlockTags.WOOL_CARPETS) || state.is(BlockTags.SAPLINGS) || state.is(BlockTags.FLOWERS)) {
            return 150;
        }
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.PLANKS) || state.is(BlockTags.WOODEN_SLABS) || state.is(BlockTags.WOODEN_STAIRS)) {
            return 700;
        }
        if (state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(Blocks.GRAVEL) || state.is(Blocks.CLAY)) {
            return 1600;
        }
        if (state.is(Blocks.ICE) || state.is(Blocks.PACKED_ICE) || state.is(Blocks.SNOW_BLOCK)) {
            return 920;
        }
        if (state.is(Blocks.GLOWSTONE) || state.is(Blocks.SPONGE) || state.is(Blocks.HAY_BLOCK)) {
            return 300;
        }
        return 2500;
    }

    /** The torque a block needs at this speed: the next power of two above speed times density, divided by four. */
    public static int requiredTorque(BlockState state, int velocity) {
        long product = (long) velocity * density(state);
        if (product <= 1) {
            return 1;
        }
        long pow = Long.highestOneBit(product - 1) << 1;
        return (int) Math.min(Integer.MAX_VALUE, pow / 4);
    }

    private int effectiveVelocity() {
        return targetMode ? solvedVelocity : Math.min(velocity, 1000);
    }

    private int effectiveTheta() {
        return targetMode ? solvedTheta : Math.min(theta, 90);
    }

    private Vec3 muzzle() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5, worldPosition.getZ() + 0.5);
    }

    private static Vec3 launchVelocity(double blocksPerSecond, double theta, double phi) {
        double speed = blocksPerSecond / 20D;
        double t = Math.toRadians(theta);
        double p = Math.toRadians(phi);
        return new Vec3(speed * Math.cos(t) * Math.cos(p), speed * Math.sin(t), speed * Math.cos(t) * Math.sin(p));
    }

    /** Flies a shot tick by tick as a falling block flies (gravity of 0.04, then drag of 0.98), stopping at the first block in its way; the block it ends in, or null if it never lands. */
    public BlockPos landing(ServerLevel server, int blocksPerSecond, int elevation, int compass) {
        Vec3 pos = muzzle();
        Vec3 vel = launchVelocity(blocksPerSecond, elevation, compass);
        for (int t = 0; t < 1200; t++) {
            vel = new Vec3(vel.x, vel.y - 0.04, vel.z);
            Vec3 next = pos.add(vel);
            HitResult hit = server.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, net.minecraft.world.phys.shapes.CollisionContext.empty()));
            if (hit.getType() != HitResult.Type.MISS) {
                return BlockPos.containing(pos);
            }
            if (!server.isLoaded(BlockPos.containing(next))) {
                return null;
            }
            pos = next;
            vel = vel.scale(0.98);
        }
        return null;
    }

    /** Target mode: the slowest speed and an elevation that bring the shot down on the target, found by trying them; kept until the target or the torque changes. */
    private boolean solve() {
        long key = target.asLong() * 31 + torque;
        if (key == solvedFor) {
            return solvedVelocity > 0;
        }
        solvedFor = key;
        solvedVelocity = 0;
        Vec3 from = muzzle();
        double dx = target.getX() + 0.5 - from.x;
        double dz = target.getZ() + 0.5 - from.z;
        double ty = target.getY() + 0.5;
        phi = Math.floorMod((int) Math.round(Math.toDegrees(Math.atan2(dz, dx))), 360);
        double wanted = Math.sqrt(dx * dx + dz * dz);
        double bestError = Double.MAX_VALUE;
        for (int v = 10; v <= 1000; v++) {
            for (int angle = 1; angle <= 90; angle++) {
                double vy = launchVelocity(v, angle, 0).y;
                double speed = Math.cos(Math.toRadians(angle)) * v / 20D;
                double y = 0;
                double x = 0;
                for (int t = 1; t < 1200; t++) {
                    vy -= 0.04;
                    double nextY = y + vy;
                    x += speed;
                    speed *= 0.98;
                    if (nextY <= ty - from.y && vy < 0) {
                        double error = Math.abs(x - wanted);
                        if (error < bestError) {
                            bestError = error;
                            solvedVelocity = v;
                            solvedTheta = angle;
                        }
                        break;
                    }
                    y = nextY;
                    vy *= 0.98;
                }
            }
            if (bestError < 1.5) {
                break;
            }
        }
        if (bestError >= 1.5) {
            solvedVelocity = 0;
        }
        setChanged();
        return solvedVelocity > 0;
    }

    private int nextSlot(int velocity) {
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (stack.getItem() instanceof BlockItem block && torque >= requiredTorque(block.getBlock().defaultBlockState(), velocity)) {
                return i;
            }
        }
        return -1;
    }

    /** Fires one block: false if it has none it can throw, nowhere to put it, or the shot would land where it may not build. */
    public boolean fire(ServerLevel server) {
        if (targetMode && !solve()) {
            return false;
        }
        int v = effectiveVelocity();
        int slot = nextSlot(v);
        BlockPos start = worldPosition.above();
        if (slot < 0 || v <= 0 || !server.getBlockState(start).canBeReplaced() || !server.getBlockState(start).getFluidState().isEmpty()) {
            return false;
        }
        BlockPos lands = landing(server, v, effectiveTheta(), phi);
        if (lands == null || !MachineGuard.mayChange(server, lands, owner) || !MachineGuard.mayChange(server, start, owner)) {
            return false;
        }
        Block block = ((BlockItem) items.getStackInSlot(slot).getItem()).getBlock();
        items.extractItem(slot, 1, false);
        BlockState state = block.defaultBlockState();
        FallingBlockEntity shot = FallingBlockEntity.fall(server, start, state);
        shot.time = -10_000;
        shot.setDeltaMovement(launchVelocity(v, effectiveTheta(), phi));
        shot.hurtMarked = true;
        server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1F, 1F);
        server.sendParticles(ParticleTypes.EXPLOSION, worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, 1, 0, 0, 0, 0);
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("blockCannon") || ++tickCount < OPERATION_TIME) {
            return;
        }
        tickCount = 0;
        fire(server);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("velocity", velocity);
        tag.putInt("theta", theta);
        tag.putInt("phi", phi);
        tag.putBoolean("target_mode", targetMode);
        tag.putLong("target", target.asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        velocity = tag.getInt("velocity");
        theta = tag.getInt("theta");
        phi = tag.getInt("phi");
        targetMode = tag.getBoolean("target_mode");
        target = BlockPos.of(tag.getLong("target"));
    }
}
