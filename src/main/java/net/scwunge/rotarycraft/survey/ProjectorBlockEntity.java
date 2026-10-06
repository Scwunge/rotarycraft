package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Projector, as the original: with 512 W from behind it throws the slide in its first slot on the wall in front of it, up to 12 blocks
 * away, as a picture seven blocks wide and five tall, from its own height up (when the wall is solid for that whole area and the air in front of it is clear).
 * A redstone pulse moves on to the next slide. Slides are the 24 in the ring of slots.
 */
public class ProjectorBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 512);
    public static final int SLOTS = 24;
    public static final int MAX_RANGE = 12;
    public static final int HALF_WIDTH = 3;
    public static final int BELOW = 0;
    public static final int ABOVE = 4;

    private final ItemStackHandler items = new ItemStackHandler(SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof SlideItem;
        }
    };
    private int distance;
    private boolean showing;
    private int slide = -1;
    private boolean signal;

    public ProjectorBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.PROJECTOR_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public ItemStackHandler items() {
        return items;
    }

    /** The way it projects. */
    public Direction direction() {
        Direction facing = facing();
        return facing.getAxis().isHorizontal() ? facing : Direction.NORTH;
    }

    /** Whether it is lit and the wall in front can take the picture. */
    public boolean isShowing() {
        return showing;
    }

    /** How far away the wall is, in blocks (the number of blocks from the projector to the wall's face). */
    public int distance() {
        return distance;
    }

    /** The picture it is showing (0 to 23), or -1 if its first slot is empty. */
    public int slide() {
        return slide;
    }

    /** Moves the next slide to the front (a redstone pulse). */
    public void cycle() {
        ItemStack first = items.getStackInSlot(0);
        for (int i = 0; i < SLOTS - 1; i++) {
            items.setStackInSlot(i, items.getStackInSlot(i + 1));
        }
        items.setStackInSlot(SLOTS - 1, first);
        level.playSound(null, worldPosition, SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.BLOCKS, 1, 0.8f);
        setChanged();
    }

    /** Redstone: a rising edge moves on to the next slide. */
    public void neighbourSignal(boolean now) {
        if (now && !signal) {
            cycle();
        }
        signal = now;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (level.getGameTime() % 5 != 0) {
            return;
        }
        int d = 0;
        boolean can = false;
        if (powered) {
            d = wallDistance();
            can = d > 0 && wallIsReady(d);
        }
        ItemStack first = items.getStackInSlot(0);
        int s = first.getItem() instanceof SlideItem item ? item.index() : -1;
        if (can != showing || d != distance || s != slide || level.getGameTime() % 100 == 0) {
            showing = can;
            distance = d;
            slide = s;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    /** The distance to the first block in the way, or 0 if there is none within reach. */
    private int wallDistance() {
        Direction dir = direction();
        for (int i = 1; i <= MAX_RANGE + 1; i++) {
            if (!level.getBlockState(worldPosition.relative(dir, i)).isAir()) {
                return i;
            }
        }
        return 0;
    }

    /** The wall is solid across the whole picture, and the air in front of it is clear. */
    private boolean wallIsReady(int d) {
        Direction dir = direction();
        Direction across = dir.getClockWise();
        for (int dy = -BELOW; dy <= ABOVE; dy++) {
            for (int dx = -HALF_WIDTH; dx <= HALF_WIDTH; dx++) {
                BlockPos wall = worldPosition.relative(dir, d).relative(across, dx).above(dy);
                if (!level.getBlockState(wall).isSolidRender(level, wall)) {
                    return false;
                }
                if (d > 1 && !level.getBlockState(wall.relative(dir.getOpposite())).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ProjectorMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putBoolean("signal", signal);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        signal = tag.getBoolean("signal");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("showing", showing);
        tag.putInt("distance", distance);
        tag.putInt("slide", slide);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        showing = tag.getBoolean("showing");
        distance = tag.getInt("distance");
        slide = tag.getInt("slide");
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
