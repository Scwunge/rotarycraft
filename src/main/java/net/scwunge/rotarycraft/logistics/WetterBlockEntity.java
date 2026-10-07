package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.WettingRecipe;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Wetter (TileEntityWetter): soaks the item in its one slot in the fluid in its thousand mB tank, as the wetting recipes say (sand in lubricant becomes soul
 * sand, cobblestone in jet fuel becomes netherrack). It needs 1 N*m at 1024 rad/s and 4 kW from below. The soaking takes the recipe's time less five ticks for each
 * 1024 rad/s over the minimum, and goes faster as the speed rises. The item cannot be taken out while it is soaking, nor for half a second after. Fluid comes in
 * at the sides.
 */
public class WetterBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "wetter";
    public static final int CAPACITY = 1000;
    public static final int MIN_SPEED = 1024;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(1, MIN_SPEED, 4096);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(80, 35).tanks(1).build();

    private final FluidTank tank;
    private final IFluidHandler sides;
    private int soak;
    private int cooldown;

    public WetterBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.WETTER.type().get(), pos, state, 1, NAME);
        tank = addTank(CAPACITY, this::validFluid);
        sides = fillOnly(tank);
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
    public Direction inputSide() {
        return Direction.DOWN;
    }

    public FluidTank tank() {
        return tank;
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return side == null || side.getAxis().isHorizontal() ? sides : null;
    }

    private List<RecipeHolder<WettingRecipe>> recipes() {
        return level == null ? List.of() : level.getRecipeManager().getAllRecipesFor(LogisticsRegistry.WETTING.get());
    }

    private boolean validFluid(FluidStack stack) {
        if (soak > 0) {
            return false;
        }
        ItemStack item = items.getStackInSlot(0);
        for (RecipeHolder<WettingRecipe> holder : recipes()) {
            if (holder.value().acceptsFluid(stack) && (item.isEmpty() || holder.value().ingredient().test(item))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        FluidStack fluid = tank.getFluid();
        for (RecipeHolder<WettingRecipe> holder : recipes()) {
            if (holder.value().ingredient().test(stack) && (fluid.isEmpty() || holder.value().acceptsFluid(fluid))) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected int slotLimit(int slot) {
        return 1;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return soak == 0 && cooldown == 0;
    }

    /** The recipe that applies to the item and fluid it holds, if there is enough fluid. */
    public Optional<RecipeHolder<WettingRecipe>> current() {
        ItemStack item = items.getStackInSlot(0);
        FluidStack fluid = tank.getFluid();
        if (item.isEmpty() || fluid.isEmpty()) {
            return Optional.empty();
        }
        for (RecipeHolder<WettingRecipe> holder : recipes()) {
            if (holder.value().ingredient().test(item) && holder.value().hasEnough(fluid)) {
                return Optional.of(holder);
            }
        }
        return Optional.empty();
    }

    /** The ticks a recipe takes at this speed. */
    public int duration(WettingRecipe recipe) {
        return Math.max(1, recipe.duration() - 5 * (omega / MIN_SPEED - 1));
    }

    public int soaked() {
        return soak;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel)) {
            return;
        }
        boolean worked = false;
        if (powered && MachineConfig.enabled("wetter")) {
            Optional<RecipeHolder<WettingRecipe>> recipe = current();
            if (recipe.isPresent()) {
                worked = true;
                WettingRecipe r = recipe.get().value();
                if (soak >= duration(r)) {
                    tank.drain(r.amount(), IFluidHandler.FluidAction.EXECUTE);
                    items.setStackInSlot(0, r.result().copy());
                    soak = 0;
                    markClientDirty();
                } else {
                    soak += 1 + 4 * (int) (Math.log(Math.max(1, omega / MIN_SPEED)) / Math.log(2));
                    cooldown = 10;
                }
            }
        }
        if (worked) {
            cooldown = 10;
        } else {
            if (cooldown > 0) {
                cooldown--;
            }
            soak = 0;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("soak", soak);
        tag.putInt("cooldown", cooldown);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        soak = tag.getInt("soak");
        cooldown = tag.getInt("cooldown");
    }
}
