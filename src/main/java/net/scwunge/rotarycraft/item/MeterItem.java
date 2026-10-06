package net.scwunge.rotarycraft.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.scwunge.rotarycraft.blockentity.GearboxBlockEntity;
import net.scwunge.rotarycraft.blockentity.GeneratorBlockEntity;
import net.scwunge.rotarycraft.blockentity.MotorBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.blockentity.ShaftBlockEntity;

/** Angular Transducer: right-click a machine to read the torque, speed and power at it. */
public class MeterItem extends Item {
    public MeterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        BlockEntity be = context.getLevel().getBlockEntity(context.getClickedPos());
        if (be instanceof net.scwunge.rotarycraft.blockentity.PipeBlockEntity pipe) {
            Player p = context.getPlayer();
            if (!context.getLevel().isClientSide() && p != null) {
                var f = pipe.contents();
                p.sendSystemMessage(f.isEmpty() ? Component.translatable("message.rotarycraft.meter.pipe_empty")
                        : Component.translatable("message.rotarycraft.meter.pipe", f.getHoverName(), pipe.amount(),
                        net.scwunge.rotarycraft.pipe.PipeType.pressure(pipe.amount()) / 1000));
            }
            return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
        }
        if (!(be instanceof PowerBlockEntity machine)) {
            return InteractionResult.PASS;
        }
        Player player = context.getPlayer();
        if (!context.getLevel().isClientSide() && player != null) {
            player.sendSystemMessage(Component.translatable("message.rotarycraft.meter.reading",
                    machine.getTorque(), machine.getOmega(), formatWatts(machine.getPower())));
            if (be instanceof ShaftBlockEntity shaft && !shaft.material().isUnbreakable()) {
                player.sendSystemMessage(Component.translatable("message.rotarycraft.meter.shaft_limits",
                        (long) shaft.material().maxTorque, (long) shaft.material().maxSpeed));
            } else if (be instanceof GearboxBlockEntity gearbox) {
                player.sendSystemMessage(Component.translatable(gearbox.isReduction()
                        ? "message.rotarycraft.gearbox.reduction" : "message.rotarycraft.gearbox.acceleration", gearbox.ratio()));
            } else if (be instanceof GeneratorBlockEntity gen) {
                player.sendSystemMessage(Component.translatable("message.rotarycraft.meter.generator",
                        gen.fePerTick(), gen.energy().getEnergyStored(), gen.energy().getMaxEnergyStored()));
            } else if (be instanceof MotorBlockEntity motor) {
                player.sendSystemMessage(Component.translatable("message.rotarycraft.meter.motor",
                        MotorBlockEntity.fePerTick(), motor.energy().getEnergyStored(), motor.energy().getMaxEnergyStored()));
            }
        }
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide());
    }

    public static String formatWatts(long watts) {
        if (watts >= 1_000_000_000L) {
            return String.format("%.2f GW", watts / 1e9);
        }
        if (watts >= 1_000_000L) {
            return String.format("%.2f MW", watts / 1e6);
        }
        if (watts >= 1_000L) {
            return String.format("%.2f kW", watts / 1e3);
        }
        return watts + " W";
    }
}
