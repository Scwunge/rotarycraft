package net.scwunge.rotarycraft.blockentity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryMenus;

/**
 * Microturbine: 16 N*m at 131072 rad/s (2.1 MW) on jet fuel ({@code c:jet_fuel}), 10 mB every 48 ticks at speed. At that
 * speed it takes about a minute and a half to spin up, as in the original. Needs air. Fuel comes in through pipes.
 */
public class MicroturbineBlockEntity extends FuelEngineBlockEntity {
    public static final int TORQUE = 16;
    public static final int SPEED = 131072;
    public static final TagKey<Fluid> JET_FUEL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "jet_fuel"));

    public MicroturbineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.MICROTURBINE.get(), pos, state);
    }

    @Override
    protected int ratedTorque() {
        return TORQUE;
    }

    @Override
    protected int targetSpeed() {
        return SPEED;
    }

    @Override
    protected TagKey<Fluid> fuelTag() {
        return JET_FUEL;
    }

    @Override
    protected Fluid fuelFluid() {
        return RotaryFluids.JET_FUEL.get();
    }

    @Override
    protected Item fuelItem() {
        return Items.AIR;
    }

    @Override
    protected int slotCount() {
        return 0;
    }

    @Override
    protected int fuelUnitTicks() {
        return 48;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.microturbine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelEngineMenu(RotaryMenus.TURBINE.get(), id, inventory, this, data);
    }

    @Override
    protected boolean isTurbine() {
        return true;
    }
}
