package net.scwunge.rotarycraft.solar;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.registry.SolarRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * A Solar Mirror: it turns to throw the sun at the top of the tower nearest it (drawn by its renderer), and counts towards the plant's light
 * as long as it is whole, is the highest mirror of its column and has open sky above it. A heavy fall onto it breaks it; a new mirror mends it.
 */
public class SolarMirrorBlockEntity extends BlockEntity implements SolarPlantMember {
    @Nullable
    private SolarPlant plant;
    private boolean broken;
    /** Where the glass is drawn pointing (client only: the renderer eases it round to where it should face). */
    public float renderYaw;
    public float renderTilt;
    public boolean renderAimed;

    public SolarMirrorBlockEntity(BlockPos pos, BlockState state) {
        super(SolarRegistry.SOLAR_MIRROR_BE.get(), pos, state);
    }

    @Nullable
    @Override
    public SolarPlant plant() {
        return plant;
    }

    @Override
    public void setPlant(@Nullable SolarPlant plant) {
        this.plant = plant;
    }

    @Override
    public void searchForPlant() {
        if (plant == null && level != null) {
            SolarPlant.build(level, worldPosition);
        }
    }

    public boolean isBroken() {
        return broken;
    }

    /** Whether it counts towards the plant's light. */
    public boolean isFunctional() {
        return !broken && plant != null && level != null && level.canSeeSky(worldPosition.above()) && plant.isHighestMirror(worldPosition);
    }

    public void breakMirror() {
        if (broken) {
            return;
        }
        broken = true;
        setChanged();
        if (level != null) {
            level.playSound(null, worldPosition, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1, 1);
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
        }
    }

    public void repair() {
        if (broken) {
            broken = false;
            setChanged();
            if (level != null) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 2);
            }
        }
    }

    public void serverTick() {
        searchForPlant();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("broken", broken);
        tag.putBoolean("working", isFunctional());
        tag.putBoolean("hasPlant", plant != null);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        broken = tag.getBoolean("broken");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putBoolean("broken", broken);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        broken = tag.getBoolean("broken");
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
