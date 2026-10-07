package net.scwunge.rotarycraft.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.config.RotaryConfig;
import net.scwunge.rotarycraft.registry.GadgetRegistry;

/** The Handbook: a guide to the machines and items, shown on a screen of its own. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID)
public class HandbookItem extends Item {
    private static final String GIVEN = "rotarycraft_handbook_given";

    public HandbookItem(Properties properties) {
        super(properties.stacksTo(1));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) {
            net.scwunge.rotarycraft.client.HandbookScreen.open();
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }

    /** With spawnWithHandbook on, a player gets one the first time they join. */
    @SubscribeEvent
    public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide() || !RotaryConfig.get(RotaryConfig.SPAWN_WITH_HANDBOOK)) {
            return;
        }
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(Player.PERSISTED_NBT_TAG);
        if (!persisted.getBoolean(GIVEN)) {
            persisted.putBoolean(GIVEN, true);
            data.put(Player.PERSISTED_NBT_TAG, persisted);
            player.getInventory().add(new ItemStack(GadgetRegistry.HANDBOOK.get()));
        }
    }
}
