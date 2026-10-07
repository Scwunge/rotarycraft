package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Item Cannon (TileEntityItemCannon): a pair of these send items between them, wherever they are: it takes the first stack it holds, and every eight
 * ticks shoots one item (the whole stack with 512 kW) into the inventory of the Item Cannon at the coordinates and dimension set on its screen
 * (0 is the Overworld, -1 the Nether, 1 the End), if that one is loaded and has room. Power comes in from below. It holds nine stacks.
 */
public class ItemCannonBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "item_cannon";
    public static final int SLOTS = 9;
    public static final int STACK_POWER = 524_288;
    public static final int OPERATION_TIME = 8;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(128, 1, 32768);
    private static final int MAX = 30_000_000;
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(176, 170).grid(8, 66, 9, 1)
            .field(121, 14, 46, -MAX, MAX, "gui.rotarycraft.item_cannon.x", 68, 18).field(121, 30, 46, -MAX, MAX, "gui.rotarycraft.item_cannon.y", 68, 34)
            .field(121, 46, 46, -MAX, MAX, "gui.rotarycraft.item_cannon.z", 68, 51).field(16, 38, 26, -100, 100, "gui.rotarycraft.item_cannon.dim", 12, 26).build();

    private int targetX;
    private int targetY;
    private int targetZ;
    private int targetDim;
    private boolean hasTarget;

    public boolean hasTarget() {
        return hasTarget;
    }
    private int tickCount;

    public ItemCannonBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.ITEM_CANNON.type().get(), pos, state, SLOTS, NAME);
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
    public Direction inputSide() {
        return Direction.DOWN;
    }

    public BlockPos target() {
        return new BlockPos(targetX, targetY, targetZ);
    }

    public int targetDimension() {
        return targetDim;
    }

    public void setTarget(int dim, int x, int y, int z) {
        targetDim = dim;
        targetX = x;
        targetY = y;
        targetZ = z;
        hasTarget = true;
        setChanged();
    }

    @Override
    public boolean setField(Player player, int field, int value) {
        switch (field) {
            case 0 -> targetX = value;
            case 1 -> targetY = value;
            case 2 -> targetZ = value;
            case 3 -> targetDim = value;
            default -> {
                return false;
            }
        }
        hasTarget = true;
        setChanged();
        return true;
    }

    @Override
    public int extra(int index) {
        return switch (index) {
            case 0 -> targetX;
            case 1 -> targetY;
            case 2 -> targetZ;
            default -> targetDim;
        };
    }

    @Override
    protected int extraCount() {
        return 4;
    }

    /** The level of a dimension number: 0, -1 and 1 as the original's, or null for any other. */
    @Nullable
    public static ServerLevel levelOf(ServerLevel from, int dim) {
        return from.getServer().getLevel(switch (dim) {
            case 0 -> Level.OVERWORLD;
            case -1 -> Level.NETHER;
            case 1 -> Level.END;
            default -> null;
        });
    }

    /** The Item Cannon it is aimed at, if there is one in a loaded place. */
    @Nullable
    public ItemCannonBlockEntity targetCannon() {
        if (!hasTarget || !(level instanceof ServerLevel server)) {
            return null;
        }
        ServerLevel there = levelOf(server, targetDim);
        BlockPos pos = target();
        if (there == null || !there.isLoaded(pos)) {
            return null;
        }
        return there.getBlockEntity(pos) instanceof ItemCannonBlockEntity cannon ? cannon : null;
    }

    private int firstStack() {
        for (int i = 0; i < SLOTS; i++) {
            if (!items.getStackInSlot(i).isEmpty()) {
                return i;
            }
        }
        return -1;
    }

    /** How many items it may send now: the stack with enough power, else one. */
    public int sendCount(ItemStack stack) {
        return getPower() >= STACK_POWER ? stack.getCount() : 1;
    }

    /** One shot: moves what the target has room for, and takes it from the first stack. */
    public boolean fire(ServerLevel server, ItemCannonBlockEntity to) {
        int slot = firstStack();
        if (slot < 0 || to == this) {
            return false;
        }
        ItemStack stack = items.getStackInSlot(slot);
        ItemStack send = stack.copyWithCount(sendCount(stack));
        ItemStack left = send;
        for (int i = 0; i < SLOTS && !left.isEmpty(); i++) {
            left = to.items.insertItem(i, left, false);
        }
        int moved = send.getCount() - left.getCount();
        if (moved <= 0) {
            return false;
        }
        items.extractItem(slot, moved, false);
        server.playSound(null, worldPosition, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 1F, 1F);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, send), worldPosition.getX() + 0.5, worldPosition.getY() + 1.125, worldPosition.getZ() + 0.5, 8, 0.2, 0.2, 0.2, 0.15);
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("itemCannon") || ++tickCount < OPERATION_TIME) {
            return;
        }
        ItemCannonBlockEntity to = targetCannon();
        if (firstStack() < 0 || to == null) {
            return;
        }
        if (fire(server, to)) {
            tickCount = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("tx", targetX);
        tag.putInt("ty", targetY);
        tag.putInt("tz", targetZ);
        tag.putInt("tdim", targetDim);
        tag.putBoolean("has_target", hasTarget);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        targetX = tag.getInt("tx");
        targetY = tag.getInt("ty");
        targetZ = tag.getInt("tz");
        targetDim = tag.getInt("tdim");
        hasTarget = tag.getBoolean("has_target");
    }
}
