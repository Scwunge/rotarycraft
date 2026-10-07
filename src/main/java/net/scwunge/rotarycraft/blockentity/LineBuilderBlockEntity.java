package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.machine.MachineGuard;

/**
 * Line Builder (TileEntityLineBuilder): a ram that pushes a line of blocks along, one step at a time, and adds a block from its nine slots to the
 * end nearest it. The line is the run of solid blocks directly in front of it; it moves a block further out only if there is room at the far end
 * (air, water and the like). It will not push anything with a block entity, bedrock, or anything its owner may not break. Off unless the server enables it.
 */
public class LineBuilderBlockEntity extends InventoryMachineBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1024, 1, 131072);
    public static final int SLOTS = 9;
    public static final String NAME = "line_builder";
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).storage(1).build();
    private static final int INVALID = Integer.MIN_VALUE;

    private int tickCount;

    public LineBuilderBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.LINE_BUILDER.type().get(), pos, state, SLOTS, NAME);
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
        return stack.getItem() instanceof BlockItem;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    /** The ticks between pushes: 40 - 2 log2(speed), but never under three. */
    public int operationTime() {
        return Math.max(3, PowerRequirement.operationTime(40, 2, omega));
    }

    /** Whether there is a block to add (the original operating condition). */
    public boolean hasBlocks() {
        return nextBlockSlot() >= 0;
    }

    private int nextBlockSlot() {
        for (int i = 0; i < items.getSlots(); i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    private static boolean soft(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        return state.isAir() || state.canBeReplaced();
    }

    private static int maxLength() {
        return Math.max(64, MachineConfig.get(MachineConfig.LINE_BUILDER_LENGTH));
    }

    /**
     * The number of solid blocks in the line in front of it, or {@link #INVALID} if it cannot move: a block entity, bedrock or an unbreakable
     * block, or one its owner may not change, is in the way.
     */
    public int lineLength(ServerLevel server) {
        Direction dir = facing();
        int max = maxLength();
        int i = 1;
        BlockPos at = worldPosition.relative(dir, i);
        if (!pushable(server, at)) {
            return INVALID;
        }
        while (!soft(server, at) && i <= max) {
            i++;
            at = worldPosition.relative(dir, i);
            if (!pushable(server, at)) {
                return INVALID;
            }
        }
        return i - 1;
    }

    private boolean pushable(ServerLevel server, BlockPos at) {
        BlockState state = server.getBlockState(at);
        if (state.is(Blocks.BEDROCK) || state.getDestroySpeed(server, at) < 0 && !state.isAir() && !state.canBeReplaced()) {
            return false;
        }
        if (server.getBlockEntity(at) != null) {
            return false;
        }
        return MachineGuard.mayChange(server, at, owner);
    }

    public boolean canShift(ServerLevel server) {
        int length = lineLength(server);
        if (length == INVALID) {
            return false;
        }
        int r = length + 1;
        BlockPos end = worldPosition.relative(facing(), r);
        return soft(server, end) && r <= maxLength() && MachineGuard.mayChange(server, end, owner);
    }

    /** One push: every block of the line moves a step along, and a block from the slots goes in at the near end. */
    public boolean shiftBlocks(ServerLevel server) {
        if (!canShift(server)) {
            return false;
        }
        int slot = nextBlockSlot();
        if (slot < 0) {
            return false;
        }
        BlockItem item = (BlockItem) items.getStackInSlot(slot).getItem();
        Direction dir = facing();
        int length = lineLength(server);
        for (int i = length; i > 0; i--) {
            BlockPos from = worldPosition.relative(dir, i);
            BlockPos to = worldPosition.relative(dir, i + 1);
            server.setBlock(to, server.getBlockState(from), 3);
            server.removeBlock(from, false);
        }
        BlockPos first = worldPosition.relative(dir);
        server.setBlock(first, item.getBlock().defaultBlockState(), 3);
        items.extractItem(slot, 1, false);
        server.playSound(null, first, net.scwunge.rotarycraft.registry.MachineSoundRegistry.get("linebuild").get(), SoundSource.BLOCKS, 1F, 1F);
        return true;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("lineBuilder")) {
            return;
        }
        if (++tickCount >= operationTime()) {
            tickCount = 0;
            shiftBlocks(server);
        }
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
