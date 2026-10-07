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
        registrar.playToServer(CvtValue.TYPE, CvtValue.CODEC, (p, ctx) -> ctx.enqueueWork(() -> CvtValue.handle(p, ctx)));
        registrar.playToServer(CoilValue.TYPE, CoilValue.CODEC, (p, ctx) -> ctx.enqueueWork(() -> CoilValue.handle(p, ctx)));
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

    /** A CVT screen's number: the ratio in manual mode (negative for torque), or the target torque in auto mode. */
    public record CvtValue(BlockPos pos, int value, boolean target) implements CustomPacketPayload {
        public static final Type<CvtValue> TYPE = new Type<>(RotaryCraft.id("cvt_value"));
        public static final StreamCodec<ByteBuf, CvtValue> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CvtValue::pos,
                ByteBufCodecs.INT, CvtValue::value, ByteBufCodecs.BOOL, CvtValue::target, CvtValue::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only someone standing at the gear can change it. */
        static void handle(CvtValue p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (player.distanceToSqr(p.pos.getCenter()) <= 64 && player.level().isLoaded(p.pos)
                    && player.level().getBlockEntity(p.pos) instanceof AdvancedGearBlockEntity gear && gear.kind() == AdvancedGearBlock.Kind.CVT) {
                if (p.target) {
                    gear.setTargetTorque(p.value);
                } else {
                    gear.setRatio(p.value);
                }
            }
        }
    }

    /** An energy coil screen's number: the speed (or the torque) it gives out when released. */
    public record CoilValue(BlockPos pos, int value, boolean torque) implements CustomPacketPayload {
        public static final Type<CoilValue> TYPE = new Type<>(RotaryCraft.id("coil_value"));
        public static final StreamCodec<ByteBuf, CoilValue> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, CoilValue::pos,
                ByteBufCodecs.INT, CoilValue::value, ByteBufCodecs.BOOL, CoilValue::torque, CoilValue::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only someone standing at the coil can change it. */
        static void handle(CoilValue p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (player.distanceToSqr(p.pos.getCenter()) <= 64 && player.level().isLoaded(p.pos)
                    && player.level().getBlockEntity(p.pos) instanceof AdvancedGearBlockEntity gear && gear.kind().isCoil()) {
                if (p.torque) {
                    gear.setReleaseTorque(p.value);
                } else {
                    gear.setReleaseOmega(p.value);
                }
            }
        }
    }
}
