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
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import net.scwunge.rotarycraft.weapon.turret.OmniConsumerBlockEntity;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Ground-Penetrating Radar, as the original: with at least 32 kW (from any side) it scans, once a second, a slice of the ground
 * below it: a plane 96 blocks deep and up to 81 wide (2 * log2 of the power above the minimum, either side) running east-west
 * or north-south, and shows it on its screen as a map of coloured blocks. The plane can be moved along the way it looks, and
 * turned with a screwdriver.
 */
public class GprBlockEntity extends OmniConsumerBlockEntity implements MenuProvider {
    public static final long MIN_POWER = 32768;
    public static final int MAX_HEIGHT = 96;
    public static final int MAX_WIDTH = 81;
    public static final int MAX_RANGE = MAX_WIDTH / 2;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    static final int UNKNOWN_COLOR = 0x000000;
    static final int AIR_COLOR = 0x151520;

    /** True when the plane runs east-west (and the screen looks northward). */
    private boolean xdir;
    private int offsetX, offsetY, offsetZ;
    private int countdown;
    /** The slice: palette indexes, row by depth then column across (first row is the layer under the machine). */
    private final byte[] cells = new byte[MAX_HEIGHT * MAX_WIDTH];
    private final List<Integer> palette = new ArrayList<>(List.of(UNKNOWN_COLOR, AIR_COLOR));
    private int scanned;
    private int scannedRange = -1;
    private long version;

    public GprBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.GPR_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** The slice's reach either side of the middle: 2 * log2(power above the minimum). */
    public int range() {
        long extra = getPower() - MIN_POWER;
        if (extra < 1) {
            return 0;
        }
        return Math.min(MAX_RANGE, 2 * (63 - Long.numberOfLeadingZeros(extra)));
    }

    public boolean xdir() {
        return xdir;
    }

    public void setDirection(boolean xdir) {
        this.xdir = xdir;
        setChanged();
        syncNow();
    }

    public void flipDirection() {
        setDirection(!xdir);
    }

    /** The way the screen looks, which is across the plane. */
    public Direction guiDirection() {
        return xdir ? Direction.SOUTH : Direction.EAST;
    }

    /** Moves the plane along the way the screen looks; zero puts it back under the machine. */
    public void shift(int amount) {
        if (amount == 0) {
            offsetX = offsetY = offsetZ = 0;
        } else {
            Direction dir = guiDirection();
            offsetX += dir.getStepX() * amount;
            offsetY += dir.getStepY() * amount;
            offsetZ += dir.getStepZ() * amount;
        }
        setChanged();
        countdown = 0;
    }

    /** The middle of the plane's top edge: where the machine is, moved by the shifts. */
    public BlockPos centre() {
        return worldPosition.offset(offsetX, offsetY, offsetZ);
    }

    public long version() {
        return version;
    }

    public int scannedRange() {
        return scannedRange;
    }

    public List<Integer> palette() {
        return palette;
    }

    /** The column bytes for -range..range, deepest last, as the screen draws them. */
    public byte[] columns(int range) {
        byte[] out = new byte[(2 * range + 1) * MAX_HEIGHT];
        for (int col = 0; col <= 2 * range; col++) {
            for (int row = 0; row < MAX_HEIGHT; row++) {
                out[col * MAX_HEIGHT + row] = cells[row * MAX_WIDTH + (MAX_RANGE - range + col)];
            }
        }
        return out;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!powered) {
            return;
        }
        if (countdown <= 0) {
            scan();
            countdown = 20;
        }
        countdown--;
    }

    private int paletteIndex(Map<Integer, Integer> index, int color) {
        Integer known = index.get(color);
        if (known != null) {
            return known;
        }
        if (palette.size() >= 255) {
            return 0;
        }
        palette.add(color);
        index.put(color, palette.size() - 1);
        return palette.size() - 1;
    }

    private void scan() {
        Level world = level;
        int r = range();
        BlockPos centre = centre();
        Map<Integer, Integer> index = new HashMap<>();
        for (int i = 0; i < palette.size(); i++) {
            index.put(palette.get(i), i);
        }
        boolean alongX = xdir;
        for (int j = -r; j <= r; j++) {
            for (int dd = 1; dd <= MAX_HEIGHT; dd++) {
                BlockPos at = new BlockPos(centre.getX() + (alongX ? j : 0), worldPosition.getY() + offsetY - dd, centre.getZ() + (alongX ? 0 : j));
                int color;
                if (at.getY() < world.getMinBuildHeight() || at.getY() >= world.getMaxBuildHeight() || !world.hasChunkAt(at)) {
                    color = UNKNOWN_COLOR;
                } else {
                    BlockState state = world.getBlockState(at);
                    color = state.isAir() ? AIR_COLOR : state.getMapColor(world, at).col;
                    if (color == 0) {
                        color = AIR_COLOR;
                    }
                }
                cells[(dd - 1) * MAX_WIDTH + MAX_RANGE + j] = (byte) paletteIndex(index, color);
            }
        }
        scanned++;
        scannedRange = r;
        version++;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new GprMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("xdir", xdir);
        tag.putInt("offX", offsetX);
        tag.putInt("offY", offsetY);
        tag.putInt("offZ", offsetZ);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        xdir = tag.getBoolean("xdir");
        offsetX = tag.getInt("offX");
        offsetY = tag.getInt("offY");
        offsetZ = tag.getInt("offZ");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("xdir", xdir);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        xdir = tag.getBoolean("xdir");
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
