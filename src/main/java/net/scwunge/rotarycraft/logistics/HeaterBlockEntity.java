package net.scwunge.rotarycraft.logistics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.HeatEffects;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.LogisticsRegistry;

/**
 * Heater (TileEntityHeater): burns fuel from its eighteen slots to hold itself at the temperature set on its screen (up to 2000 C), taking the hottest fuel
 * that does not overshoot (a fuel is worth its burn time over 25 degrees), and passes its heat on to a heatable machine on top of it; things on top are set
 * alight from 240 C and the block above is heated (snow melts, water boils, flammables catch). It cools towards the surroundings. Power comes in from below.
 */
public class HeaterBlockEntity extends InventoryMachineBlockEntity implements Heatable {
    public static final String NAME = "heater";
    public static final int SLOTS = 18;
    public static final int MAX_TEMPERATURE = 2000;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(16, 1, 8192);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(176, 167).inventoryAt(8, 85).grid(8, 18, 9, 2)
            .field(136, 55, 32, 0, MAX_TEMPERATURE, "gui.rotarycraft.heater.temperature", 26, 59).build();

    private int temperature = 20;
    private int setTemperature;
    private int coolTicks;
    private int heatTicks;
    private boolean idle;

    public HeaterBlockEntity(BlockPos pos, BlockState state) {
        super(LogisticsRegistry.HEATER.type().get(), pos, state, SLOTS, NAME);
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

    @Override
    protected boolean acceptsItem(int slot, ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    @Override
    protected boolean mayExtract(int slot) {
        return false;
    }

    @Override
    public boolean setField(Player player, int field, int value) {
        if (field != 0) {
            return false;
        }
        setTemperature = Math.max(0, Math.min(MAX_TEMPERATURE, value));
        setChanged();
        return true;
    }

    @Override
    public int extra(int index) {
        return setTemperature;
    }

    @Override
    protected int extraCount() {
        return 1;
    }

    public int setTemperature() {
        return setTemperature;
    }

    public boolean isIdle() {
        return idle;
    }

    public int operationTime() {
        return PowerRequirement.operationTime(200, 10, omega);
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
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    public void setCurrentTemperature(int t) {
        temperature = t;
        setChanged();
    }

    // ---- fuel ----

    /** What a fuel is worth in degrees: its burn time over 25. */
    public static int fuelHeat(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) / 25;
    }

    /** The slot of the hottest fuel that is no hotter than {@code max}, or -1. */
    private int hottestSlot(int max) {
        int best = -1;
        int bestHeat = -1;
        for (int i = 0; i < SLOTS; i++) {
            ItemStack stack = items.getStackInSlot(i);
            if (!stack.isEmpty()) {
                int heat = fuelHeat(stack);
                if (heat > 0 && heat <= max && heat > bestHeat) {
                    bestHeat = heat;
                    best = i;
                }
            }
        }
        return best;
    }

    private void burn(int slot) {
        ItemStack stack = items.getStackInSlot(slot);
        ItemStack left = stack.getCraftingRemainingItem();
        items.extractItem(slot, 1, false);
        if (!left.isEmpty()) {
            for (int i = 0; i < SLOTS && !left.isEmpty(); i++) {
                left = items.insertItem(i, left, false);
            }
            if (!left.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(level, worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, left);
            }
        }
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        if (glowTier(temperature) != syncedTier) {
            syncedTier = glowTier(temperature);
            markClientDirty();
        }
        if (++coolTicks >= 20) {
            coolTicks = 0;
            int ambient = Ambient.temperature(server, worldPosition);
            if (temperature > ambient) {
                temperature -= Math.max((temperature - ambient) / 200, 1);
            } else if (temperature < ambient) {
                temperature += Math.max((ambient - temperature) / 40, 1);
            }
        }
        if (!powered || !MachineConfig.enabled("heater")) {
            return;
        }
        idle = setTemperature <= Ambient.temperature(server, worldPosition) || hottestSlot(setTemperature) < 0;
        if (++heatTicks >= operationTime()) {
            heatTicks = 0;
            int diff = setTemperature - temperature;
            if (diff > 0) {
                int slot = hottestSlot(diff);
                if (slot >= 0) {
                    int heat = fuelHeat(items.getStackInSlot(slot));
                    burn(slot);
                    temperature = Math.min(MAX_TEMPERATURE, temperature + (int) (heat * 1.5));
                }
            }
        }
        BlockPos above = worldPosition.above();
        BlockEntity be = server.getBlockEntity(above);
        if (be instanceof Heatable target && target.canBeFrictionHeated()) {
            int diff = temperature - target.getTemperature();
            if (diff > 0) {
                target.addTemperature(diff > 100 ? diff / 16 : diff > 16 ? diff / 8 : diff > 8 ? diff / 4 : diff);
            }
        }
        if (server.getGameTime() % 20 == 0) {
            HeatEffects.affect(server, above, temperature, owner);
        }
        if (temperature >= 240) {
            for (LivingEntity hot : server.getEntitiesOfClass(LivingEntity.class, new AABB(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                    worldPosition.getX() + 1, worldPosition.getY() + 2, worldPosition.getZ() + 1))) {
                hot.igniteForSeconds(temperature / 50F);
            }
        }
    }

    /** Which of the glow textures it shows: from cold up to 900 C and over, as the original's six. */
    public static int glowTier(int temperature) {
        return temperature >= 900 ? 5 : temperature >= 800 ? 4 : temperature >= 600 ? 3 : temperature >= 400 ? 2 : temperature >= 200 ? 1 : 0;
    }

    private int syncedTier;

    @Override
    protected void writeClient(CompoundTag tag) {
        super.writeClient(tag);
        tag.putInt("temperature", temperature);
    }

    @Override
    protected void readClient(CompoundTag tag) {
        super.readClient(tag);
        temperature = tag.getInt("temperature");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", temperature);
        tag.putInt("set_temperature", setTemperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : 20;
        setTemperature = tag.getInt("set_temperature");
    }
}
