package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractFurnaceBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.recipe.FrictionHeatingRecipe;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryRecipes;

import java.util.Optional;

/**
 * Friction Heater: rubs shaft power into heat for the machine or furnace in front of it. Needs 32 N*m and 8 kW.
 * While powered and facing something it can heat, its temperature rises by 3 x log2(speed) x log2(torque) and falls by a
 * fifth of its excess over 30 C, so it settles at 30 + 12 x log2(speed) x log2(torque) C. Steel (600 C) needs
 * log2(speed) x log2(torque) of about 48 or more, for example 32 N*m at 1024 rad/s (about 630 C).
 * <ul>
 * <li>Heatable machines in front (the Blast Furnace, the Fermenter...) are brought up to that temperature.</li>
 * <li>A vanilla furnace in front is kept lit from 300 C and cooks faster the hotter the heater is, as in the original, and the heater
 * also does the special {@code rotarycraft:friction_heating} smelts (tungsten flakes at 1350 C, silicon dust, graphite...) in it.</li>
 * <li>At 2000 C the furnace in front may melt (explode and vanish, when explosions are allowed).</li>
 * </ul>
 */
public class FrictionHeaterBlockEntity extends ConsumerBlockEntity {
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(32, 1, 8192);
    public static final int MAX_TEMPERATURE = 2000;
    /** The original's medium-difficulty odds that a furnace at maximum temperature melts, per tick. */
    private static final int FURNACE_MELT_ODDS = 600;

    private int temperature = 20;
    private int smeltTime;

    public FrictionHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.FRICTION_HEATER.get(), pos, state);
    }

    public int temperature() {
        return temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    private Heatable target() {
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.relative(facing()));
        return be instanceof Heatable h && h.canBeFrictionHeated() ? h : null;
    }

    private AbstractFurnaceBlockEntity furnace() {
        if (level == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(worldPosition.relative(facing()));
        return be instanceof AbstractFurnaceBlockEntity f ? f : null;
    }

    @Override
    protected void machineTick(boolean powered) {
        Heatable target = target();
        AbstractFurnaceBlockEntity furnace = furnace();
        if (powered && (target != null || furnace != null)) {
            temperature += (int) (3 * log2(omega) * log2(torque));
        }
        int tAmb = powered ? 30 : 20;
        if (temperature > tAmb) {
            temperature -= (temperature - tAmb) / 5;
            if (temperature - tAmb <= 4) {
                temperature--;
            }
        } else if (temperature < tAmb) {
            temperature = tAmb;
        }
        temperature = Math.min(MAX_TEMPERATURE, temperature);
        if (powered && target != null) {
            int diff = Math.min(target.getMaxTemperature(), temperature) - target.getTemperature();
            if (diff > 0) {
                target.addTemperature(Math.max(1, (int) (diff * target.heatMultiplier())));
            }
        }
        if (powered && furnace != null) {
            driveFurnace(furnace);
        } else {
            smeltTime = 0;
        }
        setChanged();
    }

    /** Turns burn time into a furnace: 2 ticks of fire per degree over 300 C. */
    private int burnTime() {
        return temperature < 300 ? 0 : (temperature - 300) * 2;
    }

    /** The original's speed-up: x1 under 500 C, then 1 + sqrt(2^((T - 500) / 100)), and instant at the maximum. */
    public static int speedFactor(int temperature) {
        if (temperature < 500) {
            return 1;
        }
        if (temperature >= MAX_TEMPERATURE) {
            return 2000;
        }
        return 1 + (int) Math.sqrt(Math.pow(2, (temperature - 500) / 100F));
    }

    private Optional<FrictionHeatingRecipe> special(ItemStack in) {
        if (in.isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(RotaryRecipes.FRICTION_HEATING.get(), new SingleRecipeInput(in), level)
                .map(RecipeHolder::value).filter(r -> temperature >= r.temperature());
    }

    private void driveFurnace(AbstractFurnaceBlockEntity furnace) {
        int burn = burnTime();
        BlockState state = level.getBlockState(furnace.getBlockPos());
        boolean wasLit = furnace.litTime > 0;
        int newLit = Math.max(burn, furnace.litTime);
        furnace.litTime = newLit;
        furnace.litDuration = Math.max(newLit, furnace.litDuration);
        if (newLit > 0 && !wasLit && state.hasProperty(AbstractFurnaceBlock.LIT)) {
            level.setBlock(furnace.getBlockPos(), state.setValue(AbstractFurnaceBlock.LIT, true), 3);
        }
        ItemStack in = furnace.getItem(0);
        if (in.isEmpty()) {
            smeltTime = 0;
            return;
        }
        Optional<FrictionHeatingRecipe> special = special(in);
        ItemStack out = furnace.getItem(2);
        if (special.isPresent() && !(out.isEmpty() || (ItemStack.isSameItemSameComponents(out, special.get().result())
                && out.getCount() + special.get().result().getCount() <= out.getMaxStackSize()))) {
            special = Optional.empty();
        }
        if (special.isPresent()) {
            // our own cooking, as the original: faster the further over the recipe's temperature, up to the speed factor
            FrictionHeatingRecipe r = special.get();
            float ratio = temperature / (float) r.temperature();
            float acceleration = Math.min(1F, ratio * ratio - 1F);
            smeltTime += 1 + (int) (speedFactor(temperature) * acceleration);
            furnace.cookingTotalTime = r.duration();
            furnace.cookingProgress = Math.min(smeltTime, r.duration() - 5);
            if (smeltTime >= r.duration()) {
                furnace.removeItem(0, 1);
                furnace.setItem(2, out.isEmpty() ? r.result().copy() : out.copyWithCount(out.getCount() + r.result().getCount()));
                award(furnace, 1);
                smeltTime = 0;
            }
            furnace.setChanged();
            return;
        }
        smeltTime = 0;
        // ordinary smelting: the furnace does the work, we just push its progress along (it finishes itself when full)
        int extra = speedFactor(temperature) - 1;
        if (extra > 0 && furnace.cookingProgress > 0 && furnace.cookingTotalTime > 0) {
            furnace.cookingProgress = Math.min(furnace.cookingTotalTime - 1, furnace.cookingProgress + extra);
        }
        if (temperature >= MAX_TEMPERATURE && RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) && level.random.nextInt(FURNACE_MELT_ODDS) == 0) {
            meltFurnace(furnace.getBlockPos());
        }
    }

    private void award(AbstractFurnaceBlockEntity furnace, int xp) {
        if (level instanceof ServerLevel server) {
            ExperienceOrb.award(server, Vec3.atCenterOf(furnace.getBlockPos()).add(0, 0.1, 0), xp);
        }
    }

    private void meltFurnace(BlockPos pos) {
        Level lvl = level;
        lvl.removeBlock(pos, false);
        lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1F, Level.ExplosionInteraction.NONE);
        lvl.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
    }

    private static double log2(int v) {
        return v <= 0 ? 0 : Math.log(v) / Math.log(2);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("temperature", temperature);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : 20;
    }
}
