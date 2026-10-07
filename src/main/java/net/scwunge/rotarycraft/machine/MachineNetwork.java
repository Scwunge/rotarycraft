package net.scwunge.rotarycraft.machine;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.scwunge.rotarycraft.RotaryCraft;

/** The packets of the layout machines' screens: a number typed in one of a screen's boxes. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class MachineNetwork {
    private MachineNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("machines-1").optional().playToServer(FieldValue.TYPE, FieldValue.CODEC, (p, ctx) -> ctx.enqueueWork(() -> FieldValue.handle(p, ctx)));
    }

    /** A number typed in field {@code field} of the machine at {@code pos}. */
    public record FieldValue(BlockPos pos, int field, int value) implements CustomPacketPayload {
        public static final Type<FieldValue> TYPE = new Type<>(RotaryCraft.id("machine_field"));
        public static final StreamCodec<ByteBuf, FieldValue> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, FieldValue::pos, ByteBufCodecs.VAR_INT,
                FieldValue::field, ByteBufCodecs.VAR_INT, FieldValue::value, FieldValue::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only someone with that machine's screen open, and standing at it, can change it; the number is held to the box's limits. */
        static void handle(FieldValue p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (!(player.containerMenu instanceof LayoutMenu menu) || !menu.pos().equals(p.pos) || player.distanceToSqr(p.pos.getCenter()) > 64
                    || p.field < 0 || p.field >= menu.layout().fields().size() || !(player.level().getBlockEntity(p.pos) instanceof MachineHost host)) {
                return;
            }
            GuiLayout.Field field = menu.layout().fields().get(p.field);
            host.setField(player, p.field, Math.max(field.min(), Math.min(field.max(), p.value)));
        }
    }
}
