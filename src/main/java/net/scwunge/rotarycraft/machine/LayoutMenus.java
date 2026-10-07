package net.scwunge.rotarycraft.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.scwunge.rotarycraft.registry.RotaryMenus;

import java.util.LinkedHashMap;
import java.util.Map;

/** Registers one menu type per machine screen and builds its menus; the screens of all of them are {@code LayoutScreen}. */
public final class LayoutMenus {
    private static final Map<String, GuiLayout> LAYOUTS = new LinkedHashMap<>();
    private static final Map<String, DeferredHolder<MenuType<?>, MenuType<LayoutMenu>>> TYPES = new LinkedHashMap<>();

    private LayoutMenus() {
    }

    /** Registers the menu of the machine called {@code layout.name()}. */
    public static DeferredHolder<MenuType<?>, MenuType<LayoutMenu>> register(GuiLayout layout) {
        LAYOUTS.put(layout.name(), layout);
        DeferredHolder<MenuType<?>, MenuType<LayoutMenu>>[] holder = new DeferredHolder[1];
        holder[0] = RotaryMenus.MENUS.register(layout.name(), () -> IMenuTypeExtension.create((id, inventory, buf) -> fromNetwork(holder[0].get(), layout, id, inventory, buf)));
        TYPES.put(layout.name(), holder[0]);
        return holder[0];
    }

    public static GuiLayout layout(String name) {
        return LAYOUTS.get(name);
    }

    public static Map<String, DeferredHolder<MenuType<?>, MenuType<LayoutMenu>>> types() {
        return TYPES;
    }

    private static LayoutMenu fromNetwork(MenuType<LayoutMenu> type, GuiLayout layout, int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        ItemStackHandler items = inventory.player.level().getBlockEntity(pos) instanceof MachineHost host ? host.items() : new ItemStackHandler(layout.slots().size());
        return new LayoutMenu(type, layout, id, inventory, items, pos, new SimpleContainerData(layout.dataCount()));
    }

    /** The server's menu for the machine {@code host}. */
    public static LayoutMenu create(String key, int id, Inventory inventory, InventoryMachineBlockEntity host) {
        return new LayoutMenu(TYPES.get(key).get(), LAYOUTS.get(key), id, inventory, host.items(), host.getBlockPos(), host.menuData());
    }
}
