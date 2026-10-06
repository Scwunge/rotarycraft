package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.scwunge.rotarycraft.blockentity.ConsumerBlockEntity;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.SurveyRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

/**
 * The CCTV Screen, as the original: powered from below (4 N*m, 1 rad/s and 1 kW), with three dyes in its slots naming the camera it
 * answers to. Sneak-right-click calls up the CCTV or Spy Cam within 64 blocks that has the same three dyes in the same order.
 */
public class ScreenBlockEntity extends ConsumerBlockEntity implements MenuProvider {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(4, 1, 1024);
    public static final int RANGE = 64;

    private final ItemStackHandler items = new ItemStackHandler(3) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return stack.getItem() instanceof DyeItem;
        }
    };

    public ScreenBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.SCREEN_BE.get(), pos, state);
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    public ItemStackHandler items() {
        return items;
    }

    /** It takes power from below only. */
    @Override
    public void serverTick() {
        IShaftPowerOutput.Reading in = IShaftPowerOutput.readInput(level, worldPosition, Direction.DOWN);
        setPower(in.torque(), in.omega());
    }

    @Override
    protected void machineTick(boolean powered) {
    }

    /** The three dye ids this screen is set to, or null if a slot is empty. */
    @Nullable
    public int[] colors() {
        int[] out = new int[3];
        for (int i = 0; i < 3; i++) {
            if (!(items.getStackInSlot(i).getItem() instanceof DyeItem dye)) {
                return null;
            }
            out[i] = dye.getDyeColor().getId();
        }
        return out;
    }

    /** The camera within range whose dyes match, if any. */
    @Nullable
    public RemoteMachineBlockEntity findCamera() {
        int[] want = colors();
        if (want == null) {
            return null;
        }
        for (int cx = (worldPosition.getX() - RANGE) >> 4; cx <= (worldPosition.getX() + RANGE) >> 4; cx++) {
            for (int cz = (worldPosition.getZ() - RANGE) >> 4; cz <= (worldPosition.getZ() + RANGE) >> 4; cz++) {
                if (!level.hasChunk(cx, cz)) {
                    continue;
                }
                LevelChunk chunk = level.getChunk(cx, cz);
                for (BlockEntity be : new ArrayList<>(chunk.getBlockEntities().values())) {
                    if (be instanceof RemoteMachineBlockEntity camera && be != this && camera.isNamed() && java.util.Arrays.equals(camera.colors(), want)
                            && Math.abs(be.getBlockPos().getX() - worldPosition.getX()) <= RANGE && Math.abs(be.getBlockPos().getY() - worldPosition.getY()) <= RANGE
                            && Math.abs(be.getBlockPos().getZ() - worldPosition.getZ()) <= RANGE) {
                        return camera;
                    }
                }
            }
        }
        return null;
    }

    /** Sneak-right-click: calls up the matching camera for the player. */
    public boolean activate(Player player) {
        if (!hasEnoughPower()) {
            return false;
        }
        RemoteMachineBlockEntity camera = findCamera();
        if (camera == null) {
            return false;
        }
        camera.activate(player);
        return true;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new ScreenMenu(id, inventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("items", items.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.deserializeNBT(registries, tag.getCompound("items"));
    }
}
