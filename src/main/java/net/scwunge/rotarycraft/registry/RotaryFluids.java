package net.scwunge.rotarycraft.registry;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.core.registries.Registries;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.ArrayList;
import java.util.List;

/**
 * The mod's fluids, with the original's colour, density (kg/m3), viscosity and temperature (K). Each gets a source and
 * flowing fluid, a placeable block and a bucket. They are drawn with the water textures tinted their colour.
 */
public class RotaryFluids {
    public static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, RotaryCraft.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, RotaryCraft.MOD_ID);

    public static final List<Entry> ALL = new ArrayList<>();

    public static final Entry ETHANOL = register("ethanol", 0x5CC5B2, 789, 950, 340);
    public static final Entry JET_FUEL = register("jet_fuel", 0xFB5C90, 810, 800, 300);
    public static final Entry LUBRICANT = register("lubricant", 0xE4E18E, 750, 1200, 300);
    public static final Entry LIQUID_NITROGEN = register("liquid_nitrogen", 0xB37ECC, 808, 158, 77);

    public static final class Entry {
        public final String name;
        public final int color;
        public final DeferredHolder<FluidType, FluidType> type;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Source> source;
        public final DeferredHolder<Fluid, BaseFlowingFluid.Flowing> flowing;
        public final DeferredBlock<LiquidBlock> block;
        public final DeferredItem<BucketItem> bucket;

        private Entry(String name, int color, int density, int viscosity, int temperature) {
            this.name = name;
            this.color = color;
            type = TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid_type.rotarycraft." + name).density(density).viscosity(viscosity).temperature(temperature)
                    .canExtinguish(false).canConvertToSource(false).supportsBoating(true)));
            BaseFlowingFluid.Properties[] props = new BaseFlowingFluid.Properties[1];
            source = FLUIDS.register(name, () -> new BaseFlowingFluid.Source(props[0]));
            flowing = FLUIDS.register("flowing_" + name, () -> new BaseFlowingFluid.Flowing(props[0]));
            block = RotaryBlocks.BLOCKS.register(name, () -> new LiquidBlock((FlowingFluid) source.get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)));
            bucket = RotaryItems.ITEMS.register(name + "_bucket", () -> new BucketItem(source.get(),
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));
            props[0] = new BaseFlowingFluid.Properties(type, source, flowing).block(block).bucket(bucket)
                    .slopeFindDistance(viscosity > 1000 ? 2 : 4).levelDecreasePerBlock(viscosity > 1000 ? 2 : 1);
        }

        public Fluid get() {
            return source.get();
        }
    }

    private static Entry register(String name, int color, int density, int viscosity, int temperature) {
        Entry e = new Entry(name, color, density, viscosity, temperature);
        ALL.add(e);
        return e;
    }
}
