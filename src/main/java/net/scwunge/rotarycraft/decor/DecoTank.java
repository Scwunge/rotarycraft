package net.scwunge.rotarycraft.decor;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.function.Predicate;

/** The Decorative Tank's shared parts: what a tank item carries, and the four settings. */
public final class DecoTank {
    /** How much fluid makes a tank item "full" (the original's fill level of 25). */
    public static final int FILL = 25;

    public static final DeferredRegister<DataComponentType<?>> COMPONENTS = DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, RotaryCraft.MOD_ID);
    /** The fluid an item holds. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<SimpleFluidContent>> FLUID = COMPONENTS.register("deco_tank_fluid",
            () -> DataComponentType.<SimpleFluidContent>builder().persistent(SimpleFluidContent.CODEC).networkSynchronized(SimpleFluidContent.STREAM_CODEC).build());
    /** The settings of a tank item, as bits in the order of {@link Flag}. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> FLAGS = COMPONENTS.register("deco_tank_flags",
            () -> DataComponentType.<Integer>builder().persistent(com.mojang.serialization.Codec.INT).networkSynchronized(ByteBufCodecs.VAR_INT).build());

    private DecoTank() {}

    /** The four settings, each switched by a different item in the crafting grid. */
    public enum Flag {
        CLEAR("clear", "Clear Glass", s -> s.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("c:glass_panes")))),
        NOCOLOR("nocolor", "Ignore Fluid Color", s -> s.is(net.minecraft.tags.ItemTags.create(net.minecraft.resources.ResourceLocation.parse("c:dyes")))),
        LIGHTED("lighted", "Glowing", s -> s.is(Items.GLOWSTONE_DUST)),
        RESISTANT("resistant", "Resistant", s -> s.is(Items.OBSIDIAN));

        public static final Flag[] LIST = values();
        public final String id;
        public final String display;
        private final Predicate<ItemStack> toggle;
        public final BooleanProperty property;

        Flag(String id, String display, Predicate<ItemStack> toggle) {
            this.id = id;
            this.display = display;
            this.toggle = toggle;
            this.property = BooleanProperty.create(id);
        }

        public int bit() {
            return 1 << ordinal();
        }

        public boolean isToggle(ItemStack stack) {
            return toggle.test(stack);
        }
    }

    public static int flags(ItemStack stack) {
        return stack.getOrDefault(FLAGS.get(), 0);
    }

    public static boolean has(ItemStack stack, Flag flag) {
        return (flags(stack) & flag.bit()) != 0;
    }
}
