package net.scwunge.rotarycraft.menu;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;
import net.scwunge.rotarycraft.blockentity.TerraformerBlockEntity;
import net.scwunge.rotarycraft.power.BiomeTransforms;
import net.scwunge.rotarycraft.registry.WorldMachineRegistry;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The Terraformer's container: its 54 slots (nine across, six down) and the player's inventory, laid out on the original's screen. Buttons
 * 0 and up pick the target biome from the list of what the biome the machine stands in can become; {@link #RADIUS_UP} and
 * {@link #RADIUS_DOWN} change the area it works.
 */
public class TerraformerMenu extends AbstractContainerMenu {
    public static final int RADIUS_UP = 100;
    public static final int RADIUS_DOWN = 101;
    public static final int RADIUS_STEP = 4;

    @Nullable
    private final TerraformerBlockEntity terraformer;
    private final ContainerData data;
    private final BlockPos pos;

    public TerraformerMenu(int id, Inventory inventory, TerraformerBlockEntity terraformer) {
        this(id, inventory, terraformer, terraformer.items(), terraformer.data(), terraformer.getBlockPos());
    }

    public TerraformerMenu(int id, Inventory inventory, BlockPos pos) {
        this(id, inventory, inventory.player.level().getBlockEntity(pos) instanceof TerraformerBlockEntity t ? t : null,
                inventory.player.level().getBlockEntity(pos) instanceof TerraformerBlockEntity t ? t.items() : new ItemStackHandler(TerraformerBlockEntity.SLOTS),
                new SimpleContainerData(TerraformerBlockEntity.DATA_COUNT), pos);
    }

    private TerraformerMenu(int id, Inventory inventory, @Nullable TerraformerBlockEntity terraformer, ItemStackHandler items, ContainerData data, BlockPos pos) {
        super(WorldMachineRegistry.TERRAFORMER_MENU.get(), id);
        this.terraformer = terraformer;
        this.data = data;
        this.pos = pos;
        for (int i = 0; i < 9; i++) {
            for (int j = 0; j < 6; j++) {
                addSlot(new SlotItemHandler(items, j * 9 + i, 72 + i * 18, 18 + j * 18));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 72 + col * 18, 140 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 72 + col * 18, 198));
        }
        addDataSlots(data);
    }

    public static TerraformerMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        return new TerraformerMenu(id, inventory, buf.readBlockPos());
    }

    public int omega() {
        return data.get(0);
    }

    public int torque() {
        return data.get(1);
    }

    public long power() {
        return (long) torque() * omega();
    }

    /** The registry id of the target biome, or -1. */
    public int targetId() {
        return data.get(2);
    }

    /** The registry id of the biome the machine stands in. */
    public int centralId() {
        return data.get(3);
    }

    public int radius() {
        return data.get(4);
    }

    public int water() {
        return data.get(5);
    }

    public int remaining() {
        return data.get(6);
    }

    public boolean signal() {
        return data.get(7) != 0;
    }

    /** What the biome the machine stands in can be turned into, in the order the buttons number them. */
    public static List<ResourceKey<Biome>> targets(net.minecraft.world.level.Level level, int centralId) {
        var biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        var central = biomes.getHolder(centralId).flatMap(h -> h.unwrapKey()).orElse(null);
        return central == null ? List.of() : BiomeTransforms.targetsFrom(central);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (terraformer == null || terraformer.isRemoved()) {
            return false;
        }
        if (id == RADIUS_UP || id == RADIUS_DOWN) {
            terraformer.setRadius(terraformer.radius() + (id == RADIUS_UP ? RADIUS_STEP : -RADIUS_STEP));
            return true;
        }
        List<ResourceKey<Biome>> list = BiomeTransforms.targetsFrom(terraformer.centralBiome());
        if (id >= 0 && id < list.size()) {
            ResourceKey<Biome> chosen = list.get(id);
            terraformer.setTarget(chosen.equals(terraformer.target()) ? null : chosen);
            return true;
        }
        return false;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int own = TerraformerBlockEntity.SLOTS;
        if (index < own ? !moveItemStackTo(stack, own, slots.size(), true) : !moveItemStackTo(stack, 0, own, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public boolean stillValid(Player player) {
        return terraformer == null || !terraformer.isRemoved() && player.distanceToSqr(pos.getCenter()) <= 64;
    }
}
