package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.item.CoilItem;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Display, as the original: with a wound coil in its slot it shows a message on a board five blocks wide and three high standing
 * on top of it (as long as that space is clear), in the colour of a dye (or the argon blue it comes with, back with glowstone dust),
 * scrolling if it is longer than the board. The message is the pages of a signed book used on it.
 */
public class DisplayBlockEntity extends SurveyBlockEntity implements MenuProvider {
    public static final int BASE_DISCHARGE_TIME = 120;
    public static final int MAX_MESSAGE = 8192;
    public static final int WIDTH = 5;
    public static final int HEIGHT = 3;

    private final ItemStackHandler items = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof CoilItem;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }
    };
    private String message = "";
    @Nullable
    private DyeColor dye;
    private int ticks;
    private boolean on;
    private boolean clear;

    public DisplayBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.DISPLAY_BE.get(), pos, state);
    }

    @Override
    public ItemStackHandler items() {
        return items;
    }

    public String message() {
        return message;
    }

    public void setMessage(String text) {
        message = text.length() > MAX_MESSAGE ? text.substring(0, MAX_MESSAGE) : text;
        setChanged();
        syncNow();
    }

    /** The dye the board is coloured with, or null for the original's argon blue. */
    @Nullable
    public DyeColor dye() {
        return dye;
    }

    public void setDye(@Nullable DyeColor dye) {
        this.dye = dye;
        setChanged();
        syncNow();
    }

    /** Whether it is lit: it has a wound coil, and the board's space is clear. */
    public boolean isShowing() {
        return on && clear;
    }

    public boolean isLit() {
        return on;
    }

    public boolean hasClearBoard() {
        return clear;
    }

    public Direction facing() {
        return getBlockState().hasProperty(BlockStateProperties.HORIZONTAL_FACING) ? getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING) : Direction.NORTH;
    }

    /** The board's colour, 0xRRGGBB, and the colour of its border. */
    public int fill() {
        return dye == null ? 0x0080FF : dye.getTextureDiffuseColor() & 0xFFFFFF;
    }

    public int border() {
        return dye == null ? 0x00FFFF : dye.getTextureDiffuseColor() & 0xFFFFFF;
    }

    /** What is in the board's way, or null if nothing is. */
    public String obstruction() {
        Direction across = facing().getClockWise();
        for (int j = -2; j <= 2; j++) {
            for (int i = 1; i <= HEIGHT; i++) {
                BlockPos at = worldPosition.relative(across, j).above(i);
                if (!level.getBlockState(at).isAir()) {
                    return level.getBlockState(at) + " at " + at.subtract(worldPosition);
                }
            }
        }
        return null;
    }

    /** Five blocks across and three up, over the machine, must be empty. */
    private boolean hasSpace() {
        Direction across = facing().getClockWise();
        for (int j = -2; j <= 2; j++) {
            for (int i = 1; i <= HEIGHT; i++) {
                if (!level.getBlockState(worldPosition.relative(across, j).above(i)).isAir()) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public void serverTick() {
        ItemStack coil = items.getStackInSlot(0);
        boolean lit = coil.getItem() instanceof CoilItem && CoilItem.charge(coil) > 0;
        boolean space = lit && (level.getGameTime() % 10 == 0 ? hasSpace() : clear);
        if (lit != on || space != clear || level.getGameTime() % 100 == 0) {
            on = lit;
            clear = space;
            syncNow();
        }
        if (lit && ++ticks > BASE_DISCHARGE_TIME * ((CoilItem) coil.getItem()).stiffness()) {
            ItemStack unwound = coil.copy();
            CoilItem.setCharge(unwound, CoilItem.charge(coil) - 1);
            items.setStackInSlot(0, unwound);
            ticks = 0;
        }
    }

    private void syncNow() {
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoilMenu(SurveyRegistry.DISPLAY_MENU.get(), id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
        tag.putInt("ticks", ticks);
        tag.putString("message", message);
        if (dye != null) {
            tag.putInt("dye", dye.getId());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
        ticks = tag.getInt("ticks");
        message = tag.getString("message");
        dye = tag.contains("dye") ? DyeColor.byId(tag.getInt("dye")) : null;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("on", on);
        tag.putBoolean("clear", clear);
        tag.putString("message", message);
        if (dye != null) {
            tag.putInt("dye", dye.getId());
        }
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        on = tag.getBoolean("on");
        clear = tag.getBoolean("clear");
        message = tag.getString("message");
        dye = tag.contains("dye") ? DyeColor.byId(tag.getInt("dye")) : null;
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
