package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TieredItem;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;
import org.jetbrains.annotations.Nullable;

/**
 * Grindstone (TileEntityGrindstone): sharpens and repairs tools, swords and shears, a point of damage for each hundred mB of water. A tool can only be repaired
 * so far: once it has been in a grindstone it remembers a budget of twice its durability in repairs, and each point it takes uses one, so the machine takes a
 * tool back from automation only when it cannot repair it more. It needs 256 N*m and 16 kW, at either end, and works faster the faster it turns.
 */
public class GrindstoneBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "grindstone";
    public static final int CAPACITY = 1000;
    public static final int WATER_PER_POINT = 100;
    public static final String REPAIRS = "repairs";
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(256, 1, 16384);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(80, 35).tanks(1).build();

    private final FluidTank tank;
    private final IFluidHandler water;
    private int ticks;
    private int soundTicks;

    public GrindstoneBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.GRINDSTONE.type().get(), pos, state, 1, NAME);
        tank = addTank(CAPACITY, s -> s.getFluid() == Fluids.WATER);
        water = fillOnly(tank);
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
    protected boolean bothEnds() {
        return true;
    }

    public FluidTank tank() {
        return tank;
    }

    @Nullable
    @Override
    public IFluidHandler fluidHandler(@Nullable Direction side) {
        return water;
    }

    public static boolean isRepairable(ItemStack stack) {
        return stack.isDamageableItem() && (stack.getItem() instanceof TieredItem || stack.getItem() instanceof ShearsItem);
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return isRepairable(stack);
    }

    /** How many points of damage the tool in the slot may still be ground off: nothing until it has been in, then what is left of its budget. */
    public static int repairsLeft(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.copyTag().contains(REPAIRS) ? data.copyTag().getInt(REPAIRS) : stack.getMaxDamage() * 2;
    }

    /** The least damage the tool can be ground down to: none, until its repair budget is spent, and then whatever it has. */
    public static int minimumDamage(ItemStack stack) {
        return repairsLeft(stack) > 0 ? 0 : stack.getMaxDamage();
    }

    public boolean hasValidItem() {
        ItemStack stack = items.getStackInSlot(0);
        return !stack.isEmpty() && isRepairable(stack) && stack.getDamageValue() > minimumDamage(stack);
    }

    @Override
    protected boolean mayExtract(int slot) {
        ItemStack stack = items.getStackInSlot(slot);
        return stack.getDamageValue() <= minimumDamage(stack);
    }

    /** Operations a tick: one, and a few more once the time formula has run out of ticks. */
    public int operationsPerTick() {
        double raw = 80 - 6 * (Math.log(omega + 1D) / Math.log(2));
        return raw >= 1 ? 1 : 1 + (int) Math.min(8, Math.floor(1 - raw));
    }

    public int operationTime() {
        return PowerRequirement.operationTime(80, 6, omega);
    }

    private void repair() {
        ItemStack stack = items.getStackInSlot(0).copy();
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        if (!tag.contains(REPAIRS)) {
            tag.putInt(REPAIRS, stack.getMaxDamage() * 2);
        }
        stack.setDamageValue(stack.getDamageValue() - 1);
        tag.putInt(REPAIRS, tag.getInt(REPAIRS) - 1);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        items.setStackInSlot(0, stack);
        tank.drain(WATER_PER_POINT, IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server) || !powered || !MachineConfig.enabled("grindstone")) {
            return;
        }
        if (!items.getStackInSlot(0).isEmpty() && ++soundTicks > 49) {
            soundTicks = 0;
            server.playSound(null, worldPosition, MachineSoundRegistry.get("friction").get(), SoundSource.BLOCKS, 0.5F, 1F);
        }
        if (++ticks < operationTime()) {
            return;
        }
        ticks = 0;
        for (int i = operationsPerTick(); i > 0 && hasValidItem() && tank.getFluidAmount() >= WATER_PER_POINT; i--) {
            repair();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("ticks", ticks);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        ticks = tag.getInt("ticks");
    }
}
