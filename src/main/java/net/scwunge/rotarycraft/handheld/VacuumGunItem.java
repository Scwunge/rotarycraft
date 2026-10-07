package net.scwunge.rotarycraft.handheld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.scwunge.rotarycraft.charged.Charge;
import net.scwunge.rotarycraft.charged.ChargedItem;
import net.scwunge.rotarycraft.machine.MachineGuard;

/**
 * The original's vacuum gun: each use draws the items (that you have room for) and experience orbs within eight blocks towards you, for a unit of charge. Sneaking
 * and pointing at an inventory within five blocks instead tips everything in it out, for two.
 */
public class VacuumGunItem extends ChargedItem {
    public static final int RANGE = 8;

    public VacuumGunItem(Properties properties) {
        super(properties);
    }

    /** Whether the inventory has room for some more of the stack. */
    public static boolean canTakeMore(Inventory inventory, ItemStack stack) {
        for (ItemStack there : inventory.items) {
            if (there.isEmpty() || ItemStack.isSameItemSameComponents(there, stack) && there.getCount() < there.getMaxStackSize()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide()) {
            return InteractionResultHolder.consume(stack);
        }
        if (player.isShiftKeyDown()) {
            if (spill(level, player) && Charge.use(stack, player, 2, "tool")) {
                return InteractionResultHolder.success(stack);
            }
            return InteractionResultHolder.pass(stack);
        }
        if (!Charge.use(stack, player, 1, "tool")) {
            return InteractionResultHolder.fail(stack);
        }
        AABB range = player.getBoundingBox().inflate(RANGE);
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, range)) {
            if (canTakeMore(player.getInventory(), item.getItem())) {
                pull(player, item);
            }
        }
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, range)) {
            pull(player, orb);
        }
        return InteractionResultHolder.success(stack);
    }

    private static void pull(Player player, Entity entity) {
        Vec3 d = player.position().subtract(entity.position());
        double dist = Math.max(0.5, d.length());
        Vec3 push = d.scale(1 / dist / dist / 2);
        entity.setDeltaMovement(entity.getDeltaMovement().add(push.x, push.y + (entity.getY() < player.getY() ? 0.1 : 0), push.z));
        entity.hurtMarked = true;
    }

    /** Empties the inventory looked at, if the player may break there; true if it was one. */
    private static boolean spill(Level level, Player player) {
        BlockHitResult block = level.clip(new ClipContext(player.getEyePosition(), player.getEyePosition().add(player.getLookAngle().scale(5)), ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, player));
        if (block.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        BlockPos pos = block.getBlockPos();
        IItemHandler inventory = level.getCapability(Capabilities.ItemHandler.BLOCK, pos, block.getDirection());
        if (inventory == null || !level.mayInteract(player, pos) || !(level instanceof net.minecraft.server.level.ServerLevel server) || !MachineGuard.mayChange(server, pos, null)) {
            return false;
        }
        boolean any = false;
        for (int i = 0; i < inventory.getSlots(); i++) {
            ItemStack out = inventory.extractItem(i, inventory.getStackInSlot(i).getCount(), false);
            if (!out.isEmpty()) {
                Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, out);
                any = true;
            }
        }
        return any;
    }
}
