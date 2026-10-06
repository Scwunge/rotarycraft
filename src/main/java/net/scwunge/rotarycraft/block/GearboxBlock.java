package net.scwunge.rotarycraft.block;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.power.ShaftMaterial;
import net.scwunge.rotarycraft.registry.RotaryBlockEntities;
import net.scwunge.rotarycraft.registry.RotaryParts;


/**
 * A gearbox of one material and ratio. Right-click with an empty hand to switch between reduction and acceleration (and
 * see its wear and lubricant), with a gear of its material to repair it, with a bearing to fit that bearing.
 */
public class GearboxBlock extends MachineBlock {
    private final int ratio;
    private final ShaftMaterial material;

    public GearboxBlock(Properties props, ShaftMaterial material, int ratio) {
        super(props, RotaryBlockEntities.GEARBOX, GearboxBlockEntity::new);
        this.ratio = ratio;
        this.material = material;
    }

    public int ratio() {
        return ratio;
    }

    public ShaftMaterial material() {
        return material;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return simpleCodec(p -> new GearboxBlock(p, material, ratio));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                              InteractionHand hand, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof GearboxBlockEntity gearbox) {
            Item held = stack.getItem();
            if (held == RotaryParts.GEARS.get(material).get()) {
                if (!level.isClientSide() && gearbox.repairWithGear()) {
                    stack.consume(1, player);
                }
                return ItemInteractionResult.sidedSuccess(level.isClientSide());
            }
            for (var e : RotaryParts.BEARINGS.entrySet()) {
                if (held == e.getValue().get()) {
                    ShaftMaterial b = e.getKey();
                    if (!level.isClientSide() && gearbox.acceptsBearing(b) && gearbox.bearing() != b) {
                        if (gearbox.bearing() != material) {
                            net.minecraft.world.Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                                    new ItemStack(RotaryParts.BEARINGS.get(gearbox.bearing()).get()));
                        }
                        gearbox.setBearing(b);
                        stack.consume(1, player);
                    }
                    return ItemInteractionResult.sidedSuccess(level.isClientSide());
                }
            }
        }
        return super.useItemOn(stack, state, level, pos, player, hand, hit);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, java.util.List<Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        var s = stack.get(net.scwunge.rotarycraft.registry.RotaryComponents.GEARBOX_STATE.get());
        if (s != null) {
            tooltip.add(Component.translatable("item.rotarycraft.gearbox.state", GearboxBlockEntity.damagePercent(s.damage()), s.lubricant())
                    .withStyle(net.minecraft.ChatFormatting.GRAY));
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof GearboxBlockEntity gearbox) {
            gearbox.toggleMode();
            Component mode = Component.translatable(gearbox.isReduction()
                    ? "message.rotarycraft.gearbox.reduction" : "message.rotarycraft.gearbox.acceleration", ratio);
            Component status = GearboxBlockEntity.maxLubricant(material) > 0
                    ? Component.translatable("message.rotarycraft.gearbox.status", GearboxBlockEntity.damagePercent(gearbox.damage()),
                    gearbox.lubricant().getFluidAmount(), gearbox.lubricant().getCapacity())
                    : Component.translatable("message.rotarycraft.gearbox.status_dry", GearboxBlockEntity.damagePercent(gearbox.damage()));
            player.displayClientMessage(mode.copy().append(" - ").append(status), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
