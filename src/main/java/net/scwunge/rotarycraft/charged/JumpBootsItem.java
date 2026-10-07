package net.scwunge.rotarycraft.charged;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.List;

/**
 * The jump boots: while they have charge (a steel pair uses a kJ about every three minutes of wearing them) they give Jump Boost IV and Speed III and let you
 * step up a block and a half, except while you sneak. The bedrock pair never runs down and protects like bedrock boots.
 */
public class JumpBootsItem extends ArmorItem implements Rechargeable {
    public static final int JUMP_AMPLIFIER = 3;
    public static final int SPEED_AMPLIFIER = 2;
    public static final ResourceLocation STEP_ID = RotaryCraft.id("jump_boots_step");

    public JumpBootsItem(Holder<ArmorMaterial> material) {
        super(material, Type.BOOTS, new Properties().stacksTo(1));
    }

    /** Whether boots like these are doing anything right now. */
    public boolean isActive(ItemStack stack) {
        return Charge.get(stack) > 0;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (!(entity instanceof Player player) || player.getItemBySlot(EquipmentSlot.FEET) != stack || !isActive(stack)) {
            return;
        }
        effects(player);
        if (!level.isClientSide() && level.random.nextInt(3200) == 0) {
            Charge.use(stack, player, 1, "armor");
        }
    }

    /** The potion effects both pairs give while worn and working. */
    public static void effects(Player player) {
        player.addEffect(new MobEffectInstance(MobEffects.JUMP, 3, JUMP_AMPLIFIER, true, false, false));
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 3, SPEED_AMPLIFIER, true, false, false));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13F * Charge.get(stack) / Charge.FULL);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x3FA8FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.rotarycraft.charge", Charge.get(stack), Charge.FULL).withStyle(ChatFormatting.GRAY));
    }

    /** Steps up a block and a half while jump boots (either pair) are working, and back to normal when they are not or you sneak. */
    @EventBusSubscriber(modid = RotaryCraft.MOD_ID)
    public static final class StepEvents {
        private StepEvents() {}

        @SubscribeEvent
        public static void tick(PlayerTickEvent.Post event) {
            Player player = event.getEntity();
            ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
            boolean want = !player.isShiftKeyDown() && ((boots.getItem() instanceof JumpBootsItem jump && jump.isActive(boots)) || boots.getItem() instanceof BedrockJumpBootsItem);
            AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
            if (step == null) {
                return;
            }
            boolean has = step.getModifier(STEP_ID) != null;
            if (want && !has) {
                step.addTransientModifier(new AttributeModifier(STEP_ID, 0.85, AttributeModifier.Operation.ADD_VALUE));
            } else if (!want && has) {
                step.removeModifier(STEP_ID);
            }
        }
    }
}
