package net.scwunge.rotarycraft.weapon;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.common.EffectCure;
import net.scwunge.rotarycraft.RotaryCraft;

import java.util.Set;

/**
 * Frozen Solid, the Freeze Gun's effect, as the original: no walking, no jumping (slimes cannot hop either) and no fall damage.
 * Milk does not cure it.
 */
public class FreezeEffect extends MobEffect {
    public FreezeEffect() {
        super(MobEffectCategory.HARMFUL, 0x289EFF);
        addAttributeModifier(Attributes.MOVEMENT_SPEED, RotaryCraft.id("frozen_speed"), -10, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        addAttributeModifier(Attributes.JUMP_STRENGTH, RotaryCraft.id("frozen_jump"), -1, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(LivingEntity entity, int amplifier) {
        entity.fallDistance = 0;
        return true;
    }

    @Override
    public void fillEffectCures(Set<EffectCure> cures, MobEffectInstance instance) {
        // none: the original's freeze has no curative items
    }
}
