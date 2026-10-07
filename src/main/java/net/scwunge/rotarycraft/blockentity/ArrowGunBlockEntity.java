package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.List;

/**
 * Arrow Gun (TileEntityMachineGun): a machine gun for arrows. Loaded with up to 27 stacks, it looks along the way it faces, as far as ten blocks plus two for
 * every doubling of the torque or the first solid block, and shoots an arrow at a speed of log2 of the torque blocks a tick whenever there is something living
 * there (other than the player who placed it, who is left alone), as often as every four ticks, faster the faster the shaft turns. It needs 1 kW.
 */
public class ArrowGunBlockEntity extends InventoryMachineBlockEntity implements MachineInteractions {
    public static final String NAME = "arrow_gun";
    public static final int SLOTS = 27;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).storage(3).build();

    private int tickCount;

    public ArrowGunBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.ARROW_GUN.type().get(), pos, state, SLOTS, NAME);
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
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return stack.is(Items.ARROW);
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    private static double log2(int n) {
        return Math.log(n + 1D) / Math.log(2);
    }

    public int range() {
        return 10 + 2 * (int) log2(torque);
    }

    public double speed() {
        return log2(torque);
    }

    public int operationTime() {
        return Math.max(16 - (int) log2(omega), 4);
    }

    private int arrowSlot() {
        for (int i = 0; i < SLOTS; i++) {
            if (items.getStackInSlot(i).is(Items.ARROW)) {
                return i;
            }
        }
        return -1;
    }

    /** Blocks free in front of the muzzle, up to the range. */
    private int freeDistance(ServerLevel server) {
        Direction dir = facing();
        int max = range();
        for (int i = 1; i <= max; i++) {
            BlockPos at = worldPosition.relative(dir, i);
            if (!server.isLoaded(at) || server.getBlockState(at).isSolidRender(server, at)) {
                return i - 1;
            }
        }
        return max;
    }

    /** The space it watches: the block in front and the free blocks beyond it. */
    public AABB watched(ServerLevel server) {
        Direction dir = facing();
        AABB box = new AABB(worldPosition).deflate(0.1).move(dir.getStepX(), 0, dir.getStepZ());
        int r = Math.min(freeDistance(server), range());
        return switch (dir) {
            case EAST -> box.setMaxX(box.maxX + r);
            case WEST -> box.setMinX(box.minX - r);
            case SOUTH -> box.setMaxZ(box.maxZ + r);
            default -> box.setMinZ(box.minZ - r);
        };
    }

    private boolean isOwner(LivingEntity e) {
        return e instanceof Player player && owner != null && owner.id().equals(player.getUUID());
    }

    /** Whether there is anything alive to shoot at (not the owner). */
    public boolean hasTarget(ServerLevel server) {
        List<LivingEntity> in = server.getEntitiesOfClass(LivingEntity.class, watched(server), LivingEntity::isAlive);
        return in.stream().anyMatch(e -> !isOwner(e));
    }

    /** Fires one arrow, using one up. */
    public boolean fire(ServerLevel server) {
        int slot = arrowSlot();
        if (slot < 0) {
            return false;
        }
        Direction dir = facing();
        Vec3 from = new Vec3(worldPosition.getX() + 0.5 + dir.getStepX(), worldPosition.getY() + 0.8, worldPosition.getZ() + 0.5 + dir.getStepZ());
        Arrow arrow = new Arrow(server, from.x, from.y, from.z, new ItemStack(Items.ARROW), null);
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        arrow.setDeltaMovement(dir.getStepX() * speed(), 0, dir.getStepZ() * speed());
        arrow.hurtMarked = true;
        server.addFreshEntity(arrow);
        items.extractItem(slot, 1, false);
        server.playSound(null, worldPosition, SoundEvents.ARROW_SHOOT, SoundSource.BLOCKS, 1F, 1F);
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("arrowGun") || ++tickCount < operationTime()) {
            return;
        }
        tickCount = 0;
        if (arrowSlot() >= 0 && hasTarget(server)) {
            fire(server);
        }
    }

    /** What a comparator reads: how full the slots are, as a chest's. */
    @Override
    public int comparatorSignal() {
        int filled = 0;
        float fraction = 0;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                fraction += (float) stack.getCount() / stack.getMaxStackSize();
                filled++;
            }
        }
        return (int) Math.floor(fraction / SLOTS * 14) + (filled > 0 ? 1 : 0);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticks", tickCount);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        tickCount = tag.getInt("ticks");
    }
}
