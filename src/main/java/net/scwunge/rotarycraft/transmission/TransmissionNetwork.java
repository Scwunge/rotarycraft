package net.scwunge.rotarycraft.transmission;

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

/** The transmission screens' packets: the torque each side of a Distribution Clutch asks for. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class TransmissionNetwork {
    private TransmissionNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("transmission-1").optional();
        registrar.playToServer(DistributionRequests.TYPE, DistributionRequests.CODEC, (p, ctx) -> ctx.enqueueWork(() -> DistributionRequests.handle(p, ctx)));
    }

    /** The torques (N*m) a Distribution Clutch's screen asks each side (north, south, west, east) for. */
    public record DistributionRequests(BlockPos pos, int north, int south, int west, int east) implements CustomPacketPayload {
        public static final Type<DistributionRequests> TYPE = new Type<>(RotaryCraft.id("distribution_requests"));
        public static final StreamCodec<ByteBuf, DistributionRequests> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, DistributionRequests::pos,
                ByteBufCodecs.INT, DistributionRequests::north, ByteBufCodecs.INT, DistributionRequests::south,
                ByteBufCodecs.INT, DistributionRequests::west, ByteBufCodecs.INT, DistributionRequests::east, DistributionRequests::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only someone standing at the clutch can change it. */
        static void handle(DistributionRequests p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (player.distanceToSqr(p.pos.getCenter()) <= 64 && player.level().isLoaded(p.pos)
                    && player.level().getBlockEntity(p.pos) instanceof DistributionClutchBlockEntity clutch) {
                clutch.setTorqueRequests(new int[] {p.north, p.south, p.west, p.east});
            }
        }
    }
}
