package net.scwunge.rotarycraft.charged;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.pipe.PipeType;
import net.scwunge.rotarycraft.registry.GadgetRegistry;
import net.scwunge.rotarycraft.registry.RotaryComponents;

import java.util.List;

/**
 * The portable fuel tank (ItemFuelTank): 16 000 mB of one fuel (ethanol, jet fuel, or any other fuel in the pipe fuels tag). Used on a machine with a tank it
 * fills the machine from the tank, or, if empty, takes the machine's fuel into itself; used in the air it tops up a jetpack you carry.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public class FuelTankItem extends Item {
    public static final int CAPACITY = 16000;

    public FuelTankItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    public static boolean accepts(FluidStack fluid) {
        return fluid.is(PipeType.FUELS);
    }

    public static FluidStack contents(ItemStack stack) {
        return Jetpack.fluid(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        ItemStack stack = context.getItemInHand();
        IFluidHandlerItem tank = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (tank == null) {
            return InteractionResult.PASS;
        }
        boolean fill = !contents(stack).isEmpty();
        FluidStack probe = fill ? contents(stack) : null;
        IFluidHandler block = null;
        for (Direction side : sides(context.getClickedFace())) {
            IFluidHandler candidate = level.getCapability(Capabilities.FluidHandler.BLOCK, pos, side);
            if (candidate == null) {
                continue;
            }
            // the side the tank is used on first, then any other that will do what is wanted (a reservoir only gives from below)
            if (fill ? candidate.fill(probe.copyWithAmount(CAPACITY), IFluidHandler.FluidAction.SIMULATE) > 0
                    : !candidate.drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE).isEmpty() && tank.fill(candidate.drain(CAPACITY, IFluidHandler.FluidAction.SIMULATE), IFluidHandler.FluidAction.SIMULATE) > 0) {
                block = candidate;
                break;
            }
        }
        if (block == null) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        FluidStack moved = fill ? FluidUtil.tryFluidTransfer(block, tank, CAPACITY, true) : FluidUtil.tryFluidTransfer(tank, block, CAPACITY, true);
        return moved.isEmpty() ? InteractionResult.PASS : InteractionResult.SUCCESS;
    }

    private static java.util.List<Direction> sides(Direction first) {
        java.util.List<Direction> sides = new java.util.ArrayList<>();
        sides.add(first);
        for (Direction d : Direction.values()) {
            if (d != first) {
                sides.add(d);
            }
        }
        sides.add(null);
        return sides;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        IFluidHandlerItem tank = stack.getCapability(Capabilities.FluidHandler.ITEM);
        if (tank == null || contents(stack).isEmpty()) {
            return InteractionResultHolder.pass(stack);
        }
        for (ItemStack carried : player.getInventory().items) {
            if (carried.getItem() instanceof Jetpack) {
                if (!level.isClientSide()) {
                    FluidUtil.tryFluidTransfer(carried.getCapability(Capabilities.FluidHandler.ITEM), tank, CAPACITY, true);
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
        }
        ItemStack worn = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.CHEST);
        if (worn.getItem() instanceof Jetpack) {
            if (!level.isClientSide()) {
                FluidUtil.tryFluidTransfer(worn.getCapability(Capabilities.FluidHandler.ITEM), tank, CAPACITY, true);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return !contents(stack).isEmpty();
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * contents(stack).getAmount() / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFB5C90;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        FluidStack fluid = contents(stack);
        tooltip.add(fluid.isEmpty() ? Component.translatable("item.rotarycraft.fuel_tank.empty").withStyle(ChatFormatting.GRAY)
                : Component.translatable("item.rotarycraft.fuel_tank.contents", fluid.getAmount(), fluid.getHoverName()).withStyle(ChatFormatting.GRAY));
    }

    /** The tank, 16 000 mB of one fuel. */
    public static class Tank extends FluidHandlerItemStack {
        public Tank(ItemStack container) {
            super(RotaryComponents.ITEM_FLUID, container, CAPACITY);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return accepts(stack) && (getFluid().isEmpty() || getFluid().getFluid() == stack.getFluid());
        }

        @Override
        public boolean canFillFluidType(FluidStack fluid) {
            return isFluidValid(0, fluid);
        }
    }

    @SubscribeEvent
    public static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, context) -> new Tank(stack), GadgetRegistry.FUEL_TANK.get());
    }
}
