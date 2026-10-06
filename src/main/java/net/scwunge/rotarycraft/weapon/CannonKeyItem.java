package net.scwunge.rotarycraft.weapon;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.registry.WeaponRegistry;
import net.scwunge.rotarycraft.weapon.turret.TurretBlockEntity;

import java.util.List;

/**
 * Cannon Key, as the original: it takes the name of the first player to carry it. Used by a turret's owner it opens the turret's
 * whitelist; used by anyone else, a key made by the turret's owner puts them on the whitelist and is used up.
 */
public class CannonKeyItem extends Item {
    public CannonKeyItem(Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!level.isClientSide && entity instanceof Player player && !stack.has(WeaponRegistry.KEY_OWNER.get())) {
            stack.set(WeaponRegistry.KEY_OWNER.get(), player.getGameProfile().getName());
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        String owner = stack.get(WeaponRegistry.KEY_OWNER.get());
        if (owner != null) {
            tooltip.add(Component.translatable("tooltip.rotarycraft.cannon_key", owner));
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (!(ctx.getLevel().getBlockEntity(ctx.getClickedPos()) instanceof TurretBlockEntity turret) || ctx.getPlayer() == null) {
            return InteractionResult.PASS;
        }
        if (ctx.getLevel().isClientSide) {
            return InteractionResult.SUCCESS;
        }
        Player player = ctx.getPlayer();
        String name = player.getGameProfile().getName();
        if (turret.isOwner(player.getUUID())) {
            if (player instanceof ServerPlayer sp && sp.connection.hasChannel(WeaponNetwork.SafePlayers.TYPE)) {
                PacketDistributor.sendToPlayer(sp, new WeaponNetwork.SafePlayers(ctx.getClickedPos(), turret.safePlayers()));
            }
            return InteractionResult.CONSUME;
        }
        String keyOwner = ctx.getItemInHand().get(WeaponRegistry.KEY_OWNER.get());
        if (keyOwner == null || turret.owner() == null || !keyOwner.equals(turret.owner().name())) {
            player.displayClientMessage(Component.translatable("message.rotarycraft.cannon_key.wrong_owner", keyOwner == null ? "?" : keyOwner,
                    turret.owner() == null ? "?" : turret.owner().name()), true);
            return InteractionResult.FAIL;
        }
        if (!turret.addSafePlayer(name)) {
            player.displayClientMessage(Component.translatable("message.rotarycraft.cannon_key.already", name), true);
            return InteractionResult.FAIL;
        }
        player.displayClientMessage(Component.translatable("message.rotarycraft.cannon_key.added", name, keyOwner), true);
        if (!player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
        return InteractionResult.CONSUME;
    }
}
