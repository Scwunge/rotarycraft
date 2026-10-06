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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.scwunge.rotarycraft.menu.FuelEngineMenu;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryFluids;
import net.scwunge.rotarycraft.registry.RotaryItems;

/**
 * Gas Engine: 128 N*m at 512 rad/s (65.5 kW) on ethanol, 10 mB every 12 ticks (about 17 mB a second; a bucket lasts
 * 60 seconds). Ethanol crystals in the slot add a bucket each. Needs air.
 */
public class GasEngineBlockEntity extends FuelEngineBlockEntity {
    public static final int TORQUE = 128;
    public static final int SPEED = 512;
    public static final TagKey<Fluid> ETHANOL = TagKey.create(Registries.FLUID, ResourceLocation.fromNamespaceAndPath("c", "ethanol"));

    public GasEngineBlockEntity(BlockPos pos, BlockState state) {
        super(RotaryBlockEntities.GAS_ENGINE.get(), pos, state);
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
        return ETHANOL;
    }

    @Override
    protected Fluid fuelFluid() {
        return RotaryFluids.ETHANOL.get();
    }

    @Override
    protected Item fuelItem() {
        return RotaryItems.ETHANOL_CRYSTALS.get();
    }

    @Override
    protected int fuelUnitTicks() {
        return 12;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.rotarycraft.gas_engine");
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FuelEngineMenu(id, inventory, this, data);
    }
}
