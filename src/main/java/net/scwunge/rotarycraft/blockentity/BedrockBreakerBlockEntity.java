package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Container;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.api.BedrockDigEvent;
import net.scwunge.rotarycraft.block.BedrockSliceBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.RotaryParts;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import net.scwunge.rotarycraft.weapon.Owned;
import net.scwunge.rotarycraft.weapon.WorldGuard;
import org.jetbrains.annotations.Nullable;

/**
 * Bedrock Breaker, as the original: a long grinding drill that reaches out in the direction it faces to the first solid block. Anything
 * soft or ordinary in its way it simply grinds out of existence (nothing drops); bedrock it grinds away a sixteenth at a time, and the
 * last sixteenth comes out as bedrock dust (3 on easy, 2 on normal, 1 on hard). The dust goes into a neighbouring inventory, or a one-slot
 * store of its own, or is thrown out. Needs 4.2 MW and a torque of 16384. It acts as its owner, so claims stop it, and it will not touch the
 * bottom layer of the world unless the config says so. It refuses blocks that hold items and other machines.
 */
public class BedrockBreakerBlockEntity extends ConsumerBlockEntity implements Owned {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(16384, 1, 4_194_304);
    private static final int SYNC_INTERVAL = 10;

    private final ItemStackHandler store = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private int step = 1;
    private int tickCount;
    private int syncedStep = -1;
    private int syncedOmega = -1;
    @Nullable
    private WorldGuard.Owner owner;

    public BedrockBreakerBlockEntity(BlockPos pos, BlockState state) {
        super(WorldMachineRegistry.BEDROCK_BREAKER_BE.get(), pos, state);
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

    public int step() {
        return step;
    }

    public ItemStackHandler store() {
        return store;
    }

    /** The grinding head's block. */
    public BlockPos head() {
        return worldPosition.relative(facing(), step);
    }

    /** How far through its block the head's bedrock has been ground (0 to 15 sixteenths, or 0 when it is not on any). */
    public float grindFraction() {
        if (level != null) {
            BlockState state = level.getBlockState(head());
            if (state.is(WorldMachineRegistry.BEDROCK_SLICE.get())) {
                return state.getValue(BedrockSliceBlock.PROGRESS) / 16f;
            }
        }
        return 0;
    }

    /** What automation may do: take the dust out (nothing in). */
    public IItemHandler automationItems() {
        return new IItemHandler() {
            @Override
            public int getSlots() {
                return 1;
            }

            @Override
            public ItemStack getStackInSlot(int slot) {
                return store.getStackInSlot(slot);
            }

            @Override
            public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                return stack;
            }

            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return store.extractItem(slot, amount, simulate);
            }

            @Override
            public int getSlotLimit(int slot) {
                return store.getSlotLimit(slot);
            }

            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                return false;
            }
        };
    }

    /** Bedrock dust a ground-out block gives (the original's difficulty scaling). */
    public static int dustPerBlock(Difficulty difficulty) {
        return switch (difficulty) {
            case EASY -> 3;
            case HARD -> 1;
            default -> 2;
        };
    }

    public static boolean isBedrock(BlockState state) {
        return state.is(Blocks.BEDROCK) || state.is(WorldMachineRegistry.BEDROCK_SLICE.get());
    }

    // ---- the work ----

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !RotaryConfig.diggerEnabled("bedrockBreaker")) {
            return;
        }
        tickCount++;
        if (!powered || (worldPosition.getY() <= server.getMinBuildHeight() && !RotaryConfig.get(RotaryConfig.BEDROCK_VOID_HOLE))) {
            return;
        }
        if (tickCount >= PowerRequirement.operationTime(600, 30, omega)) {
            process(server);
            tickCount = 0;
        }
        BlockPos head = head();
        BlockState state = server.getBlockState(head);
        if (!state.isAir() && server.getGameTime() % 3 == 0) {
            double f = step + grindFraction() - 0.5;
            Direction dir = facing();
            double px = worldPosition.getX() + 0.5 + dir.getStepX() * f;
            double py = worldPosition.getY() + 0.5 + dir.getStepY() * f;
            double pz = worldPosition.getZ() + 0.5 + dir.getStepZ() * f;
            server.sendParticles(ParticleTypes.CRIT, px, py, pz, 4, dir.getStepX() == 0 ? 0.5 : 0.05, dir.getStepY() == 0 ? 0.5 : 0.05,
                    dir.getStepZ() == 0 ? 0.5 : 0.05, 0.1);
        }
        if ((step != syncedStep || omega != syncedOmega) && server.getGameTime() % SYNC_INTERVAL == 0) {
            syncedStep = step;
            syncedOmega = omega;
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    private boolean hasSpace(ServerLevel server) {
        ItemStack held = store.getStackInSlot(0);
        if (held.isEmpty()) {
            return true;
        }
        return held.is(RotaryParts.part("bedrock_dust").get()) && held.getCount() + dustPerBlock(server.getDifficulty()) <= held.getMaxStackSize();
    }

    private boolean canBreakAt(ServerLevel server, BlockPos pos) {
        if (pos.getY() < server.getMinBuildHeight() || pos.getY() >= server.getMaxBuildHeight()) {
            return false;
        }
        if (pos.getY() == server.getMinBuildHeight() && !RotaryConfig.get(RotaryConfig.BEDROCK_VOID_HOLE)) {
            return false;
        }
        return WorldGuard.mayChange(server, pos, owner);
    }

    private void process(ServerLevel server) {
        if (!hasSpace(server)) {
            return;
        }
        BlockPos head = head();
        if (server.hasChunkAt(head) && canBreakAt(server, head)) {
            grind(server, head);
        }
    }

    private void grind(ServerLevel server, BlockPos pos) {
        BlockState state = server.getBlockState(pos);
        if (isBedrock(state)) {
            if (state.is(Blocks.BEDROCK)) {
                server.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5f, 0.8f + server.random.nextFloat() * 0.4f);
                server.setBlock(pos, WorldMachineRegistry.BEDROCK_SLICE.get().defaultBlockState()
                        .setValue(BedrockSliceBlock.FACING, facing().getOpposite()), 3);
            } else if (state.getValue(BedrockSliceBlock.PROGRESS) < BedrockSliceBlock.STAGES - 1) {
                server.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 0.5f, 0.8f + server.random.nextFloat() * 0.4f);
                server.setBlock(pos, state.setValue(BedrockSliceBlock.PROGRESS, state.getValue(BedrockSliceBlock.PROGRESS) + 1), 3);
                step--;
                incrementStep(server);
            } else {
                server.playSound(null, pos, SoundEvents.BLAZE_HURT, SoundSource.BLOCKS, 0.5f, 0.8f + server.random.nextFloat() * 0.4f);
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                deliver(server, new ItemStack(RotaryParts.part("bedrock_dust").get(), dustPerBlock(server.getDifficulty())));
                NeoForge.EVENT_BUS.post(new BedrockDigEvent(this, pos));
            }
        } else {
            if (!state.isAir() && state.getDestroySpeed(server, pos) >= 0) {
                var be = server.getBlockEntity(pos);
                if ((be instanceof Container container && !container.isEmpty()) || be instanceof PowerBlockEntity) {
                    return;
                }
                server.levelEvent(2001, pos, net.minecraft.world.level.block.Block.getId(state));
                server.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }
            incrementStep(server);
        }
        setChanged();
    }

    /** Reaches the drill on to the first solid block, one block more each time. */
    private void incrementStep(ServerLevel server) {
        int max = step + 1;
        for (int i = 1; i < max; i++) {
            if (!soft(server, worldPosition.relative(facing(), i))) {
                step = i;
                return;
            }
        }
        step = max;
    }

    private static boolean soft(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) {
            return false;
        }
        BlockState state = level.getBlockState(pos);
        return state.isAir() || !state.getFluidState().isEmpty() || state.canBeReplaced() || state.getDestroySpeed(level, pos) == 0
                && !state.hasBlockEntity();
    }

    /** Into a neighbouring inventory, else the machine's own store, else out of its top (or bottom, facing down). */
    private void deliver(ServerLevel server, ItemStack stack) {
        ItemStack left = stack;
        for (Direction side : Direction.values()) {
            IItemHandler handler = server.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(side), side.getOpposite());
            if (handler != null) {
                left = ItemHandlerHelper.insertItem(handler, left, false);
                if (left.isEmpty()) {
                    return;
                }
            }
        }
        ItemStack held = store.getStackInSlot(0);
        if (held.isEmpty() || ItemStack.isSameItemSameComponents(held, left)) {
            int moved = Math.min(left.getMaxStackSize() - held.getCount(), left.getCount());
            if (moved > 0) {
                store.setStackInSlot(0, left.copyWithCount(held.getCount() + moved));
                left = left.copyWithCount(left.getCount() - moved);
            }
        }
        if (!left.isEmpty()) {
            eject(server, left);
        }
    }

    /** Throws a stack out of the machine's top (its bottom when it faces down). */
    public void eject(ServerLevel server, ItemStack stack) {
        double y = facing() == Direction.DOWN ? worldPosition.getY() - 0.25 : worldPosition.getY() + 1.25;
        ItemEntity entity = new ItemEntity(server, worldPosition.getX() + 0.5, y, worldPosition.getZ() + 0.5, stack);
        entity.setDeltaMovement((server.random.nextDouble() - 0.5) * 0.05, facing() == Direction.DOWN ? 0 : 0.1 + server.random.nextDouble() * 0.2,
                (server.random.nextDouble() - 0.5) * 0.05);
        server.addFreshEntity(entity);
    }

    // ---- saving ----

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("store", store.serializeNBT(registries));
        tag.putInt("step", step);
        if (owner != null) {
            tag.putUUID("owner", owner.id());
            tag.putString("ownerName", owner.name());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        store.deserializeNBT(registries, tag.getCompound("store"));
        step = Math.max(1, tag.getInt("step"));
        owner = tag.hasUUID("owner") ? new WorldGuard.Owner(tag.getUUID("owner"), tag.getString("ownerName")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("step", step);
        tag.putInt("omega", omega);
        tag.putInt("torque", torque);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        step = Math.max(1, tag.getInt("step"));
        omega = tag.getInt("omega");
        torque = tag.getInt("torque");
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
