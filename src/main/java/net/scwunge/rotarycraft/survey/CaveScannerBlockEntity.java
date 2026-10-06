package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import net.scwunge.rotarycraft.weapon.turret.OmniConsumerBlockEntity;

/**
 * Cave Scanner, as the original: with at least 131 kW (from any side) it makes a box of cave outline, drawn in the world around
 * a point it is aimed at: the machine itself at first, or a spot four blocks at a time away from it (right-click moves it the way
 * you look, sneak-right-click the opposite way). The box reaches {@link RotaryConfig#CAVE_SCANNER_RANGE} blocks each way. Finding
 * the cave outline is done by the client, which draws it; the machine only keeps the aim and whether it is on.
 */
public class CaveScannerBlockEntity extends OmniConsumerBlockEntity {
    public static final long MIN_POWER = 131072;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, MIN_POWER);
    public static final int STEP = 4;

    private BlockPos source;
    private boolean on;

    public CaveScannerBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.CAVE_SCANNER_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public boolean isOn() {
        return on;
    }

    /** The point the box is centred on. */
    public BlockPos source() {
        return source == null ? worldPosition : source;
    }

    /** How far the box reaches each way. */
    public int range() {
        return Math.max(4, RotaryConfig.get(RotaryConfig.CAVE_SCANNER_RANGE));
    }

    public void moveSource(int amount, Direction dir) {
        source = source().relative(dir, amount);
        setChanged();
        syncNow();
    }

    @Override
    protected void machineTick(boolean powered) {
        if (powered != on || level.getGameTime() % 40 == 0) {
            on = powered;
            syncNow();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("source", source().asLong());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("source")) {
            source = BlockPos.of(tag.getLong("source"));
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("on", on);
        tag.putLong("source", source().asLong());
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        on = tag.getBoolean("on");
        source = BlockPos.of(tag.getLong("source"));
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
