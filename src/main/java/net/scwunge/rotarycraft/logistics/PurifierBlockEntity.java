package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;
import net.scwunge.rotarycraft.registry.RotaryItems;

/**
 * Purifier (TileEntityPurifier): turns the steel ingots of other mods (any steel but RotaryCraft's own) into RotaryCraft steel, which it makes from up to five of them
 * with a gunpowder and a block of sand, at 600 C or more. It heats itself from what is beside it: fire adds 200 C to the air it sits in, lava 600, water halves it
 * and ice quarters it (and melts into water); over 1000 C it burns up. A run takes 800 ticks less 40 for each doubling of the speed. The gunpowder is used a
 * twenty-fifth of the time and the sand a fifth. It needs 64 N*m and 16 kW from any side.
 */
public class PurifierBlockEntity extends InventoryMachineBlockEntity implements Heatable {
    public static final String NAME = "purifier";
    public static final int SMELT_TEMPERATURE = 600;
    public static final int MAX_TEMPERATURE = 1000;
    public static final int GUNPOWDER = 0;
    public static final int OUTPUT = 6;
    public static final int SAND = 7;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(64, 1, 16384);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).slot(35, 16).slot(8, 52).slot(26, 52).slot(44, 52).slot(62, 52).slot(80, 52).slot(134, 34).slot(53, 16)
            .barDown(0, 1, 11, 34, 4, 167, 82, 17).build();

    private int temperature = 20;
    private int cook;

    public PurifierBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.PURIFIER.type().get(), pos, state, 8, NAME);
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
    protected boolean omniSided() {
        return true;
    }

    private static final java.util.function.Predicate<ItemStack> TAGGED_STEEL = stack ->
            stack.is(ItemTags.create(ResourceLocation.parse("c:ingots/steel"))) && !stack.is(RotaryItems.HSLA_STEEL_INGOT.get());
    /** What counts as steel from another mod (swappable so tests can stand in for a mod that adds one). */
    public static java.util.function.Predicate<ItemStack> foreignSteel = TAGGED_STEEL;

    /** Steel from another mod: in the steel tag, and not RotaryCraft's own. */
    public static boolean isForeignSteel(ItemStack stack) {
        return foreignSteel.test(stack);
    }

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        if (slot == GUNPOWDER) {
            return stack.is(Items.GUNPOWDER);
        }
        if (slot == SAND) {
            return stack.is(ItemTags.SAND);
        }
        return slot != OUTPUT && isForeignSteel(stack);
    }

    @Override
    protected boolean mayExtract(int slot) {
        return slot == OUTPUT;
    }

    public int operationTime() {
        return PowerRequirement.operationTime(800, 40, omega);
    }

    public boolean canSmelt() {
        if (temperature < SMELT_TEMPERATURE || !items.getStackInSlot(GUNPOWDER).is(Items.GUNPOWDER) || !items.getStackInSlot(SAND).is(ItemTags.SAND)) {
            return false;
        }
        for (int i = 1; i < 6; i++) {
            if (isForeignSteel(items.getStackInSlot(i))) {
                return true;
            }
        }
        return false;
    }

    // ---- Heatable ----

    @Override
    public int getTemperature() {
        return temperature;
    }

    @Override
    public int getMaxTemperature() {
        return MAX_TEMPERATURE;
    }

    @Override
    public void addTemperature(int amount) {
        temperature += amount;
    }

    public void setCurrentTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public int extra(int index) {
        return index == 0 ? cook : operationTime();
    }

    @Override
    protected int extraCount() {
        return 2;
    }

    private void updateTemperature(ServerLevel server) {
        int ambient = Ambient.temperature(server, worldPosition);
        boolean water = false;
        boolean fire = false;
        boolean lava = false;
        for (Direction dir : Direction.values()) {
            BlockPos at = worldPosition.relative(dir);
            BlockState state = server.getBlockState(at);
            if (state.getFluidState().is(FluidTags.WATER)) {
                water = true;
            }
            if (state.getFluidState().is(FluidTags.LAVA)) {
                lava = true;
            }
            if (state.is(BlockTags.FIRE)) {
                fire = true;
            }
            if (state.is(BlockTags.ICE)) {
                if (ambient > 0) {
                    ambient /= 4;
                }
                server.setBlock(at, Blocks.WATER.defaultBlockState(), 3);
            }
        }
        if (water) {
            ambient /= 2;
        }
        if (fire) {
            ambient += 200;
        }
        if (lava) {
            ambient += 600;
        }
        if (temperature > ambient) {
            temperature--;
        }
        if (temperature > ambient * 2) {
            temperature--;
        }
        if (temperature < ambient) {
            temperature++;
        }
        if (temperature * 2 < ambient) {
            temperature++;
        }
        if (temperature > MAX_TEMPERATURE) {
            temperature = MAX_TEMPERATURE;
            server.setBlock(worldPosition, Blocks.AIR.defaultBlockState(), 3);
        }
    }

    private void smelt() {
        int count = 0;
        for (int i = 1; i < 6; i++) {
            if (isForeignSteel(items.getStackInSlot(i))) {
                items.extractItem(i, 1, false);
                count++;
            }
        }
        if (count <= 0) {
            return;
        }
        ItemStack steel = new ItemStack(RotaryItems.HSLA_STEEL_INGOT.get(), count);
        ItemStack left = putOutput(OUTPUT, steel);
        if (!left.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
        }
        if (level.random.nextInt(25) == 0) {
            items.extractItem(GUNPOWDER, 1, false);
        }
        if (level.random.nextInt(5) == 0) {
            items.extractItem(SAND, 1, false);
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        updateTemperature(server);
        if (!powered || !MachineConfig.enabled("purifier") || !canSmelt()) {
            cook = 0;
            return;
        }
        if (++cook >= operationTime()) {
            cook = 0;
            smelt();
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", temperature);
        tag.putInt("cook", cook);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : 20;
        cook = tag.getInt("cook");
    }
}
