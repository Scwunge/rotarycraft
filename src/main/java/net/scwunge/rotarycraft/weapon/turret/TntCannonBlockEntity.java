package net.scwunge.rotarycraft.weapon.turret;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.menu.OneSlotMenu;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.CannonMenu;
import net.scwunge.rotarycraft.weapon.CannonTnt;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * TNT Cannon, as the original: needs 65.5 kW, holds 11 TNT and, every half second, lobs one at the angle, compass bearing and
 * speed set on its screen. Its top speed is sqrt(power / 67.5) blocks a second and its top elevation depends on torque. In target
 * mode it works out the speed and angle that put the shot on a block you name, and sets the fuse to go off on arrival.
 */
public class TntCannonBlockEntity extends OmniConsumerBlockEntity implements MenuProvider, OneSlotMenu.Host {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 65536);
    public static final int SLOTS = 11;
    public static final double TORQUE_CAP = 32768;
    private static final int OPERATION_TIME = 10;
    private static final int MIN_FUSE = 5;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.is(Items.TNT);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    /** Hoppers and pipes may load it but not empty it. */
    private final IItemHandler automation = new IItemHandler() {
        @Override
        public int getSlots() {
            return items.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return items.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return items.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return items.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return items.isItemValid(slot, stack);
        }
    };

    private int velocity;
    private int phi;
    private int theta;
    private int fuse;
    private boolean targetMode;
    private BlockPos target = BlockPos.ZERO;
    private int tickcount;
    @Nullable
    private WorldGuard.Owner owner;
    /** The solved shot for target mode: elevation, speed and flight time, with what it was solved for. */
    private int solvedTheta, solvedVelocity, solvedTicks;
    private long solvedFor = Long.MIN_VALUE;

    public TntCannonBlockEntity(BlockPos pos, BlockState state) {
        super(WeaponRegistry.TNT_CANNON_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    public IItemHandler automationItems() {
        return automation;
    }

    public void setOwner(Player player) {
        owner = new WorldGuard.Owner(player.getUUID(), player.getGameProfile().getName());
        setChanged();
    }

    public int velocity() {
        return velocity;
    }

    public int phi() {
        return phi;
    }

    public int theta() {
        return theta;
    }

    public int fuse() {
        return fuse;
    }

    public boolean targetMode() {
        return targetMode;
    }

    public BlockPos target() {
        return target;
    }

    /** The fastest it can throw at the power it has now. */
    public int maxVelocity() {
        return (int) Math.sqrt(getPower() / 67.5D);
    }

    /** The highest elevation its torque allows. */
    public int maxTheta() {
        if (torque > TORQUE_CAP) {
            return 90;
        }
        int angle = 2 * (int) Math.ceil(Math.toDegrees(Math.asin(torque / TORQUE_CAP)));
        return Math.min(angle, 90);
    }

    /** Takes new settings from its screen; they are held to what the cannon can do when it fires. */
    public void configure(boolean targetMode, int phi, int theta, int velocity, int fuse, BlockPos target) {
        this.targetMode = targetMode;
        this.phi = Math.floorMod(phi, 360);
        this.theta = Math.max(0, Math.min(theta, 90));
        this.velocity = Math.max(0, Math.min(velocity, 100_000));
        this.fuse = Math.max(0, Math.min(fuse, 1200));
        this.target = target;
        solvedFor = Long.MIN_VALUE;
        setChanged();
        syncNow();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered || !RotaryConfig.weaponEnabled("tntCannon") || ++tickcount < OPERATION_TIME) {
            return;
        }
        tickcount = 0;
        int slot = loaded();
        if (slot < 0) {
            return;
        }
        if (targetMode && !solve()) {
            return;
        }
        fire(slot);
    }

    private int loaded() {
        for (int i = 0; i < items.getSlots(); i++) {
            if (items.getStackInSlot(i).is(Items.TNT)) {
                return i;
            }
        }
        return -1;
    }

    private Vec3 muzzle() {
        return new Vec3(worldPosition.getX() + 0.5, worldPosition.getY() + 1.5 - 0.0625, worldPosition.getZ() + 0.5);
    }

    private static Vec3 launchVelocity(double blocksPerSecond, double theta, double phi) {
        double speed = blocksPerSecond / 20D;
        double t = Math.toRadians(theta);
        double p = Math.toRadians(phi);
        return new Vec3(speed * Math.cos(t) * Math.cos(p), speed * Math.sin(t), speed * Math.cos(t) * Math.sin(p));
    }

    private void fire(int slot) {
        items.extractItem(slot, 1, false);
        level.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 0.7f + level.random.nextFloat(), 0.5f);
        Vec3 m = muzzle();
        int fuseTime = targetMode ? Math.max(MIN_FUSE, solvedTicks) : Math.max(MIN_FUSE, fuse);
        CannonTnt tnt = new CannonTnt(level, m.x, m.y, m.z, fuseTime, owner);
        int speed = targetMode ? solvedVelocity : Math.min(velocity, maxVelocity());
        int elevation = targetMode ? solvedTheta : Math.min(theta, maxTheta());
        tnt.setDeltaMovement(launchVelocity(speed, elevation, phi));
        level.addFreshEntity(tnt);
    }

    /**
     * Target mode: finds the slowest speed and an elevation within its limits that bring a shot to the target block, by flying
     * the shot with the TNT's own gravity and drag; keeps the answer until the target or its power changes.
     */
    private boolean solve() {
        long key = target.asLong() * 31 + maxVelocity() * 1000L + maxTheta();
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
        for (int v = 10; v <= maxVelocity(); v++) {
            for (int angle = 1; angle <= Math.min(90, maxTheta()); angle++) {
                int ticks = flightTicks(v, angle, ty - from.y);
                if (ticks < 0) {
                    continue;
                }
                double error = Math.abs(landing(v, angle, ticks) - wanted);
                if (error < bestError) {
                    bestError = error;
                    solvedVelocity = v;
                    solvedTheta = angle;
                    solvedTicks = ticks;
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

    /** Ticks until a shot at this speed and angle comes down through height {@code dy} above the muzzle, or -1 if it never does. */
    private static int flightTicks(int v, int angle, double dy) {
        Vec3 vel = launchVelocity(v, angle, 0);
        double y = 0;
        double vy = vel.y;
        for (int t = 1; t < 600; t++) {
            vy -= 0.04;
            double next = y + vy;
            vy *= 0.98;
            if (next <= dy && vy < 0) {
                return t;
            }
            y = next;
        }
        return -1;
    }

    /** Horizontal distance covered in {@code ticks}. */
    private static double landing(int v, int angle, int ticks) {
        double speed = Math.cos(Math.toRadians(angle)) * v / 20D;
        double x = 0;
        for (int t = 0; t < ticks; t++) {
            x += speed;
            speed *= 0.98;
        }
        return x;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CannonMenu(id, inventory, this);
    }

    private void writeSettings(CompoundTag tag) {
        tag.putInt("velocity", velocity);
        tag.putInt("phi", phi);
        tag.putInt("theta", theta);
        tag.putInt("fuse", fuse);
        tag.putBoolean("targetMode", targetMode);
        tag.putLong("target", target.asLong());
        writePower(tag);
    }

    private void readSettings(CompoundTag tag) {
        velocity = tag.getInt("velocity");
        phi = tag.getInt("phi");
        theta = tag.getInt("theta");
        fuse = tag.getInt("fuse");
        targetMode = tag.getBoolean("targetMode");
        target = BlockPos.of(tag.getLong("target"));
        readPower(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        writeSettings(tag);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("items")) {
            items.deserializeNBT(registries, tag.getCompound("items"));
        }
        readSettings(tag);
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeSettings(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readSettings(tag);
    }

    @Override
    public void onDataPacket(net.minecraft.network.Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider registries) {
        handleUpdateTag(pkt.getTag(), registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
