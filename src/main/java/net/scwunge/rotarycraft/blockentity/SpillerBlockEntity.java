package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.machine.AreaFillerBlockEntity;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Spiller (TileEntityFlooder): a 4000 mB tank that, with power, turns each bucket of fluid in it into a source block in the space beneath it (see
 * {@link AreaFillerBlockEntity}), thick fluids needing more power and working more slowly. Fluid comes in at the top and sides. Off unless the server enables it.
 */
public class SpillerBlockEntity extends AreaFillerBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, 1, 1024);
    public static final String NAME = "spiller";
    public static final int CAPACITY = 4000;
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).tanks(1).build();

    private final FluidTank tank = addTank(CAPACITY, s -> placeable(s.getFluid()));
    private final IFluidHandler intake = fillOnly(tank);

    public SpillerBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.SPILLER.type().get(), pos, state, 0, NAME, "spiller");
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    /** Whether the fluid has a block that can be placed in the world. */
    public static boolean placeable(Fluid fluid) {
        return fluid != Fluids.EMPTY && !fluid.defaultFluidState().createLegacyBlock().isAir() && fluid.isSource(fluid.defaultFluidState());
    }

    public FluidTank tank() {
        return tank;
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == Direction.DOWN ? null : intake;
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return null;
    }

    private int viscosity() {
        return tank.isEmpty() ? 1000 : tank.getFluid().getFluidType().getViscosity();
    }

    @Override
    public int operationTime() {
        int base = super.operationTime();
        return tank.isEmpty() ? base : Math.max(base / 4, Math.min(base * 4, base * viscosity() / 1000));
    }

    @Override
    protected boolean allowFluidOverwrite() {
        return false;
    }

    @Override
    protected boolean isFluidBlock(BlockState state) {
        return !state.getFluidState().isEmpty();
    }

    @Override
    protected boolean hasRemainingBlocks() {
        return tank.getFluidAmount() >= 1000;
    }

    @Nullable
    @Override
    protected BlockState nextBlock() {
        if (tank.isEmpty()) {
            return null;
        }
        BlockState state = tank.getFluid().getFluid().defaultFluidState().createLegacyBlock();
        return state.isAir() ? null : state;
    }

    @Override
    protected void onBlockPlaced() {
        tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
    }

    /** getRequiredPower: 512 W per thousand units of viscosity, at least 128 W. */
    @Override
    protected long requiredPower() {
        return Math.max(128, 512L * viscosity() / 1000);
    }
}
