package net.scwunge.rotarycraft.charged;

import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.registry.GadgetRegistry;
import net.scwunge.rotarycraft.registry.RotaryComponents;

/** A jetpack's tank, 30 000 mB of one fuel (ethanol, or jet fuel; the server can allow jet fuel only), filled from any container or pipe and not drained again. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public class JetpackTank extends FluidHandlerItemStack {
    public JetpackTank(ItemStack container) {
        super(RotaryComponents.ITEM_FLUID, container, Jetpack.CAPACITY);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return Jetpack.accepts(stack) && (getFluid().isEmpty() || getFluid().getFluid() == stack.getFluid());
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return isFluidValid(0, fluid);
    }

    @Override
    public boolean canDrainFluidType(FluidStack fluid) {
        return false;
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new JetpackTank(stack),
                GadgetRegistry.JETPACK.get(), GadgetRegistry.STEEL_JETPACK.get(), GadgetRegistry.BEDROCK_JETPACK.get());
    }
}
