package net.scwunge.rotarycraft.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.machine.LayoutMachineBlock;

import java.util.function.Function;
import java.util.function.Supplier;

/** Registers a machine's block, block entity type and block item in one call, into the mod's shared registers. */
public final class Machines {
    private Machines() {
    }

    /** A machine: its block and block entity type. */
    public record Entry<B extends PowerBlockEntity, K extends Block>(DeferredBlock<K> block, DeferredHolder<BlockEntityType<?>, BlockEntityType<B>> type) {
    }

    /**
     * Registers {@code name} as a {@link LayoutMachineBlock}-like block built by {@code blockFactory} (given the block entity type) with the block
     * entity made by {@code factory}, and a block item.
     */
    public static <B extends PowerBlockEntity, K extends Block> Entry<B, K> register(String name, BlockEntityType.BlockEntitySupplier<B> factory,
                                                                                       Function<Supplier<BlockEntityType<B>>, K> blockFactory) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<B>>[] type = new DeferredHolder[1];
        DeferredBlock<K> block = RotaryBlocks.BLOCKS.register(name, () -> blockFactory.apply(() -> type[0].get()));
        type[0] = RotaryBlockEntities.TYPES.register(name, () -> BlockEntityType.Builder.of(factory, block.get()).build(null));
        RotaryItems.add(RotaryItems.ITEMS.registerSimpleBlockItem(block));
        return new Entry<>(block, type[0]);
    }

    /** A plain shaft-driven machine block, with the usual machine properties (not occluding, so renderer-drawn machines let light through). */
    public static <B extends PowerBlockEntity> Entry<B, LayoutMachineBlock> machine(String name, BlockEntityType.BlockEntitySupplier<B> factory) {
        return register(name, factory, type -> new LayoutMachineBlock(RotaryBlocks.machineProps().noOcclusion(), type::get, factory::create));
    }
}
