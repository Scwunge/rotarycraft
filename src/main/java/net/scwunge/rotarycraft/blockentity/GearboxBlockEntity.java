package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.scwunge.rotarycraft.block.GearboxBlock;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.item.GearboxState;
import net.scwunge.rotarycraft.power.Ambient;
import net.scwunge.rotarycraft.power.IShaftPowerOutput;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.power.Heatable;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryComponents;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;
import net.scwunge.rotarycraft.registry.RotaryParts;

/**
 * Trades speed for torque (reduction) or torque for speed (acceleration); power is conserved, as in the original, less
 * 1% of torque per point of wear. Gearboxes come in six materials:
 * <ul>
 * <li>wood: no lubricant, warms up while turning and wears when hot (above 90 C), catches fire above 120 C;</li>
 * <li>stone: 8 buckets of lubricant, uses more of it at speed, warms up above 8192 rad/s;</li>
 * <li>steel and tungsten: 24 buckets of lubricant (tungsten uses less at low speed);</li>
 * <li>diamond: 1 bucket of lubricant that it never uses up;</li>
 * <li>bedrock: unbreakable, never wears, needs no lubricant.</li>
 * </ul>
 * A lubricated gearbox running dry wears (1 in 40 ticks, up to 480 points). Right-click with a gear of its material to
 * repair it, with a bearing to fit a better bearing (changes how fast it uses lubricant). It breaks like a shaft of its
 * material when the load exceeds the material's limits.
 */
public class GearboxBlockEntity extends PowerBlockEntity implements Heatable {
    public static final int MAX_DAMAGE = 480;
    private static final double BEARING_REDUCTION = 0.25;
    private static final double BEARING_INCREASE = 1.0;
    /** The original's medium-difficulty lubricant usage factor. */
    private static final double LUBE_USAGE = 1.0;

    private boolean reduction = true;
    private int damage;
    private int temperature = Integer.MIN_VALUE;
    private ShaftMaterial bearing;
    private int ticksExisted;
    private int lubeTicks;
    private final FluidTank lubricant;

    public GearboxBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.GEARBOX.get(), pos, state);
        lubricant = new FluidTank(maxLubricant(material()), s -> s.is(RotaryFluids.LUBRICANT.get())) {
            @Override
            protected void onContentsChanged() {
                setChanged();
            }
        };
    }

    public ShaftMaterial material() {
        return getBlockState().getBlock() instanceof GearboxBlock gb ? gb.material() : ShaftMaterial.STEEL;
    }

    public int ratio() {
        return getBlockState().getBlock() instanceof GearboxBlock gb ? gb.ratio() : 2;
    }

    public boolean isReduction() {
        return reduction;
    }

    public void toggleMode() {
        reduction = !reduction;
        setChanged();
    }

    public int damage() {
        return damage;
    }

    public void setDamage(int d) {
        damage = Math.max(0, Math.min(MAX_DAMAGE, d));
        setChanged();
    }

    /** Percent of torque lost to wear, as the original shows it. */
    public static int damagePercent(int damage) {
        return (int) (100 * (1 - Math.pow(0.99, damage)));
    }

    public FluidTank lubricant() {
        return lubricant;
    }

    public ShaftMaterial bearing() {
        return bearing != null ? bearing : material();
    }

    public int temperature() {
        return temperature == Integer.MIN_VALUE ? Ambient.temperature(level, worldPosition) : temperature;
    }

    public void setTemperature(int t) {
        temperature = t;
        setChanged();
    }

    // material rules (GearboxTypes) -----------------------------------------------------------------------------------

    public static int maxLubricant(ShaftMaterial m) {
        return switch (m) {
            case DIAMOND -> 1000;
            case STEEL, TUNGSTEN -> 24000;
            case STONE -> 8000;
            default -> 0;
        };
    }

    public static boolean needsLubricant(ShaftMaterial m) {
        return m != ShaftMaterial.WOOD && !m.isUnbreakable();
    }

    private boolean consumesLubricant() {
        return needsLubricant(material()) && material() != ShaftMaterial.DIAMOND && bearing().ordinal() < ShaftMaterial.DIAMOND.ordinal();
    }

    private static boolean generatesHeat(ShaftMaterial m, int omega) {
        return m == ShaftMaterial.WOOD || (m == ShaftMaterial.STONE && omega >= 8192);
    }

    private static boolean takesTemperatureDamage(ShaftMaterial m) {
        return m == ShaftMaterial.WOOD || m == ShaftMaterial.STONE;
    }

    /** Whether a bearing of {@code b} can be fitted (no wood, at most two tiers above the gearbox). */
    public boolean acceptsBearing(ShaftMaterial b) {
        ShaftMaterial m = material();
        if (m == ShaftMaterial.BEDROCK || m == ShaftMaterial.WOOD) {
            return false;
        }
        return b != ShaftMaterial.WOOD && b.ordinal() <= m.ordinal() + 2;
    }

    public void setBearing(ShaftMaterial b) {
        bearing = b;
        setChanged();
    }

    private double lubricantConsumeRate(int omegaIn) {
        return switch (material()) {
            case STONE -> Math.max(1, 1 + omegaIn / 8192F);
            case TUNGSTEN -> Math.min(1, 0.5 + Math.max(0, 0.03125 * (log2(omegaIn) - 2)));
            default -> 1;
        };
    }

    /** A better bearing cuts lubricant use by a quarter per tier; a worse one adds 100% per tier (diamond and bedrock bearings never get here). */
    private double bearingLubricantFactor() {
        int offset = bearing().ordinal() - material().ordinal();
        double add = offset > 0 ? -BEARING_REDUCTION * offset : -offset * BEARING_INCREASE;
        return Math.max(0.1, 1 + add);
    }

    private static double log2(double v) {
        return v <= 0 ? 0 : Math.log(v) / Math.log(2);
    }

    // tick ------------------------------------------------------------------------------------------------------------

    @Override
    public void serverTick() {
        ticksExisted++;
        IShaftPowerOutput.Reading in = readInput();
        int ratio = ratio();
        int torqueOut;
        int omegaOut;
        if (reduction) {
            omegaOut = in.omega() / ratio;
            torqueOut = (int) Math.min(Integer.MAX_VALUE, (long) in.torque() * ratio);
        } else {
            omegaOut = (int) Math.min(Integer.MAX_VALUE, (long) in.omega() * ratio);
            torqueOut = in.torque() / ratio;
        }
        torqueOut = (int) (torqueOut * Math.pow(0.99, damage));
        // cold: thickened lubricant slows lubricated gearboxes; wood cracks
        int tempEff = temperature() + Math.max(0, level.getBrightness(LightLayer.BLOCK, worldPosition) - 10);
        long power = (long) torqueOut * omegaOut;
        if (power >= 131072L) {
            tempEff += (int) log2(power / 131072D);
        }
        ShaftMaterial m = material();
        if (tempEff <= (m == ShaftMaterial.WOOD ? -15 : -20)) {
            if (needsLubricant(m)) {
                omegaOut = (int) (omegaOut / Math.pow(1.4, -(tempEff + 20) / 40D));
            } else if (m == ShaftMaterial.WOOD && level.random.nextDouble() < Math.min(1, (-tempEff - 15) * 0.025)) {
                setDamage(damage + 1);
            }
        }
        if (torqueOut <= 0) {
            omegaOut = 0;
        }
        if (RotaryConfig.get(RotaryConfig.SHAFT_FAILURE) && m.fails(Math.max(torqueOut, in.torque()), Math.max(omegaOut, in.omega()))) {
            fail();
            return;
        }
        setPower(torqueOut, omegaOut);
        wearAndLubricate(in.omega());
        if (ticksExisted % 20 == 0) {
            updateTemperature();
        }
    }

    private void wearAndLubricate(int omegaIn) {
        if (!needsLubricant(material()) || omega <= 0 || bearing() == ShaftMaterial.BEDROCK) {
            return;
        }
        if (lubricant.isEmpty()) {
            if (damage < MAX_DAMAGE && ticksExisted >= 100 && level.random.nextInt(40) == 0) {
                setDamage(damage + 1);
            }
            if (level.random.nextInt(60) == 0) {
                level.playSound(null, worldPosition, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.3F, 0.6F);
            }
        } else if (consumesLubricant() && ++lubeTicks >= 80) {
            lubeTicks = 0;
            double factor = lubricantConsumeRate(omegaIn) * log2(omegaIn) / 4;
            if (bearing() != material()) {
                factor *= bearingLubricantFactor();
            }
            lubricant.drain(Math.max(1, (int) (LUBE_USAGE * factor)), IFluidHandler.FluidAction.EXECUTE);
        }
    }

    private void updateTemperature() {
        int tAmb = Ambient.temperature(level, worldPosition);
        int t = temperature();
        ShaftMaterial m = material();
        if (omega > 0 && generatesHeat(m, omega)) {
            t++;
        }
        if (t > 90 && level.random.nextBoolean() && takesTemperatureDamage(m)) {
            setDamage(damage + 1);
        }
        if (t > tAmb) {
            t = Math.max(tAmb, t - (omega == 0 ? 2 : 1));
        } else if (t < tAmb) {
            t = Math.min(tAmb, t + 3);
        }
        temperature = t;
        setChanged();
        if (t > 120 && m == ShaftMaterial.WOOD) {
            for (Direction d : Direction.values()) {
                BlockPos p = worldPosition.relative(d);
                if (level.getBlockState(p).isAir()) {
                    level.setBlockAndUpdate(p, BaseFireBlock.getState(level, p));
                    break;
                }
            }
        }
    }

    /** Breaks apart: a small explosion and the original's debris (one per ratio step). */
    private void fail() {
        Level lvl = level;
        BlockPos pos = worldPosition;
        ItemStack debris = switch (material()) {
            case WOOD -> new ItemStack(RotaryItems.SAWDUST.get());
            case STONE -> new ItemStack(Items.GRAVEL);
            case DIAMOND -> new ItemStack(Items.DIAMOND);
            case BEDROCK -> new ItemStack(RotaryParts.part("bedrock_dust").get());
            default -> new ItemStack(RotaryItems.SCRAP.get());
        };
        int n = ratio();
        lvl.removeBlock(pos, false);
        lvl.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 1F, true,
                RotaryConfig.get(RotaryConfig.EXPLOSIONS_BREAK_BLOCKS) ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE);
        for (int i = 0; i < n; i++) {
            Containers.dropItemStack(lvl, pos.getX() + 0.5, pos.getY() + 1.25, pos.getZ() + 0.5, debris.copy());
        }
    }

    /** Right-click repair with a gear of the gearbox's own material: the original's 1 + 20 x (0..16 - ratio). */
    public boolean repairWithGear() {
        if (damage <= 0) {
            return false;
        }
        setDamage(damage - (1 + 20 * level.random.nextInt(Math.max(1, 18 - ratio()))));
        return true;
    }

    /** Lubricant goes in from any side but the output. */
    public IFluidHandler lubricantHandler(Direction side) {
        return maxLubricant(material()) == 0 || side == facing() ? null : lubricant;
    }

    // item state ------------------------------------------------------------------------------------------------------

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        if (damage > 0 || !lubricant.isEmpty() || bearing != null) {
            components.set(RotaryComponents.GEARBOX_STATE.get(), new GearboxState(damage, lubricant.getFluidAmount(),
                    bearing == null ? "" : bearing.id()));
        }
    }

    @Override
    protected void applyImplicitComponents(DataComponentInput input) {
        super.applyImplicitComponents(input);
        GearboxState s = input.get(RotaryComponents.GEARBOX_STATE.get());
        if (s != null) {
            damage = s.damage();
            if (s.lubricant() > 0) {
                lubricant.setFluid(new FluidStack(RotaryFluids.LUBRICANT.get(), Math.min(s.lubricant(), lubricant.getCapacity())));
            }
            bearing = s.bearing().isEmpty() ? null : ShaftMaterial.valueOf(s.bearing().toUpperCase(java.util.Locale.ROOT));
        }
    }

    @Override
    public void removeComponentsFromTag(CompoundTag tag) {
        tag.remove("damage");
        tag.remove("lubricant");
        tag.remove("bearing");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putBoolean("reduction", reduction);
        tag.putInt("damage", damage);
        tag.putInt("temperature", temperature());
        tag.put("lubricant", lubricant.writeToNBT(registries, new CompoundTag()));
        if (bearing != null) {
            tag.putString("bearing", bearing.id());
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        reduction = !tag.contains("reduction") || tag.getBoolean("reduction");
        damage = tag.getInt("damage");
        temperature = tag.contains("temperature") ? tag.getInt("temperature") : Integer.MIN_VALUE;
        lubricant.readFromNBT(registries, tag.getCompound("lubricant"));
        bearing = tag.contains("bearing") ? ShaftMaterial.valueOf(tag.getString("bearing").toUpperCase(java.util.Locale.ROOT)) : null;
    }

    @Override
    public int getTemperature() {
        return temperature();
    }

    @Override
    public int getMaxTemperature() {
        return 120;
    }

    @Override
    public void addTemperature(int amount) {
        temperature = temperature() + amount;
        setChanged();
    }

    @Override
    public boolean canBeFrictionHeated() {
        return false;
    }

    @Override
    public boolean canBeCooledWithFins() {
        return true;
    }
}
