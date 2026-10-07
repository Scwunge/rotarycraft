package net.scwunge.rotarycraft.blockentity;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.MachineGuard;
import net.scwunge.rotarycraft.machine.MachineInteractions;
import net.scwunge.rotarycraft.machine.SpringMachineBlockEntity;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Lamp (TileEntityLamp): runs off a wound coil, and while lit fills the empty air around it, up to twelve blocks (the range setting) along each axis and out along the
 * diagonals, with invisible light, a coil charge lasting 120 ticks times the coil's stiffness. A redstone signal puts it out, and stops the coil unwinding.
 * Its light is gone when it is broken.
 */
public class LampBlockEntity extends SpringMachineBlockEntity implements MachineInteractions {
    public static final int DEFAULT_RANGE = 12;
    public static final String NAME = "lamp";
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(80, 35).build();

    private final LongArrayList light = new LongArrayList();
    private boolean lit;

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.LAMP.type().get(), pos, state, 1, NAME);
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    protected int baseDischargeTime() {
        return 120;
    }

    public boolean isLit() {
        return lit;
    }

    public int lightBlocks() {
        return light.size();
    }

    private static BlockState lightState() {
        return Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, 15);
    }

    /** The spots it may light: air out along the six axes, and diagonally at even distances. */
    private List<BlockPos> spots(ServerLevel server) {
        List<BlockPos> spots = new ArrayList<>();
        BlockPos o = worldPosition;
        int range = MachineConfig.get(MachineConfig.LAMP_RANGE);
        for (int i = 1; i <= range; i++) {
            add(server, spots, o.offset(i, 0, 0));
            add(server, spots, o.offset(0, i, 0));
            add(server, spots, o.offset(0, 0, i));
            add(server, spots, o.offset(-i, 0, 0));
            add(server, spots, o.offset(0, -i, 0));
            add(server, spots, o.offset(0, 0, -i));
        }
        for (int r = 2; r <= range * 0.8; r += 2) {
            for (int sx = -1; sx <= 1; sx += 2) {
                for (int sz = -1; sz <= 1; sz += 2) {
                    add(server, spots, o.offset(sx * r, 0, sz * r));
                    for (int sy = -1; sy <= 1; sy += 2) {
                        add(server, spots, o.offset(sx * r, sy * r, sz * r));
                    }
                }
            }
        }
        return spots;
    }

    private void add(ServerLevel server, List<BlockPos> spots, BlockPos pos) {
        if (server.isLoaded(pos) && server.getBlockState(pos).isAir() && MachineGuard.mayChange(server, pos, owner)) {
            spots.add(pos);
        }
    }

    @Override
    protected void springTick(boolean hasCoil) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean redstone = level.hasNeighborSignal(worldPosition);
        boolean wasLit = lit;
        if (!redstone) {
            lit = hasCoil && MachineConfig.enabled("lamp");
            unwind();
        } else {
            lit = false;
        }
        if (lit != wasLit) {
            setChanged();
        }
        if (!lit) {
            goDark(server);
            return;
        }
        if (light.isEmpty()) {
            for (BlockPos p : spots(server)) {
                light.add(p.asLong());
            }
        }
        for (long packed : light) {
            BlockPos p = BlockPos.of(packed);
            if (server.isLoaded(p) && server.getBlockState(p).isAir()) {
                server.setBlock(p, lightState(), 2);
            }
        }
    }

    /** Takes the light away, but only blocks that are still its own light. */
    public void goDark(ServerLevel server) {
        if (light.isEmpty()) {
            return;
        }
        for (long packed : light) {
            BlockPos p = BlockPos.of(packed);
            if (server.isLoaded(p) && server.getBlockState(p).is(Blocks.LIGHT)) {
                server.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
            }
        }
        light.clear();
    }

    @Override
    public void onBroken(ServerLevel server) {
        goDark(server);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLongArray("light", light.toLongArray());
        tag.putBoolean("lit", lit);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        light.clear();
        for (long l : tag.getLongArray("light")) {
            light.add(l);
        }
        lit = tag.getBoolean("lit");
    }
}
