package net.scwunge.rotarycraft.survey;

import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.survey.SurveyClientHooks;

import java.util.ArrayList;
import java.util.List;

/** The survey machines' packets. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class SurveyNetwork {
    private SurveyNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("survey-1").optional();
        // The handler bodies only run on the client, so the client hooks never load on a dedicated server.
        registrar.playToClient(RadarData.TYPE, RadarData.CODEC, (p, ctx) -> ctx.enqueueWork(() -> SurveyClientHooks.radarData(p)));
        registrar.playToClient(GprData.TYPE, GprData.CODEC, (p, ctx) -> ctx.enqueueWork(() -> SurveyClientHooks.gprData(p)));
        registrar.playToClient(ViewCamera.TYPE, ViewCamera.CODEC, (p, ctx) -> ctx.enqueueWork(() -> SurveyClientHooks.viewCamera(p.pos())));
        registrar.playToClient(SpyCamData.TYPE, SpyCamData.CODEC, (p, ctx) -> ctx.enqueueWork(() -> SurveyClientHooks.spyCamData(p)));
        registrar.playToServer(GprShift.TYPE, GprShift.CODEC, (p, ctx) -> ctx.enqueueWork(() -> GprShift.handle(p, ctx)));
    }

    /** What the Mob Radar sees, for the open screen: the range, and each creature as dx, dz, icon. */
    public record RadarData(int containerId, int range, List<Integer> blips) implements CustomPacketPayload {
        public static final Type<RadarData> TYPE = new Type<>(RotaryCraft.id("radar_data"));
        public static final StreamCodec<ByteBuf, RadarData> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, RadarData::containerId,
                ByteBufCodecs.VAR_INT, RadarData::range, ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(1024)), RadarData::blips, RadarData::new);

        static RadarData of(int containerId, MobRadarBlockEntity.Scan scan) {
            List<Integer> flat = new ArrayList<>();
            for (MobRadarBlockEntity.Blip b : scan.blips()) {
                flat.add(b.dx());
                flat.add(b.dz());
                flat.add(b.icon());
            }
            return new RadarData(containerId, scan.range(), flat);
        }

        public MobRadarBlockEntity.Scan scan() {
            List<MobRadarBlockEntity.Blip> out = new ArrayList<>();
            for (int i = 0; i + 2 < blips.size(); i += 3) {
                out.add(new MobRadarBlockEntity.Blip(blips.get(i), blips.get(i + 1), blips.get(i + 2)));
            }
            return new MobRadarBlockEntity.Scan(range, out);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The GPR's scanned slice for the open screen: palette of colours, and a byte per block (column by column, top to bottom). */
    public record GprData(int containerId, int range, BlockPos centre, List<Integer> palette, byte[] columns) implements CustomPacketPayload {
        public static final Type<GprData> TYPE = new Type<>(RotaryCraft.id("gpr_data"));
        public static final StreamCodec<ByteBuf, GprData> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, GprData::containerId,
                ByteBufCodecs.VAR_INT, GprData::range, BlockPos.STREAM_CODEC, GprData::centre,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(256)), GprData::palette, ByteBufCodecs.BYTE_ARRAY, GprData::columns, GprData::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The GPR's screen moving its plane one block (1 or -1) along the way it looks, or back under the machine (0). */
    public record GprShift(BlockPos pos, int amount) implements CustomPacketPayload {
        public static final Type<GprShift> TYPE = new Type<>(RotaryCraft.id("gpr_shift"));
        public static final StreamCodec<ByteBuf, GprShift> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, GprShift::pos,
                ByteBufCodecs.VAR_INT, GprShift::amount, GprShift::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only someone standing at the machine can move it. */
        static void handle(GprShift p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (player.distanceToSqr(p.pos.getCenter()) <= 64 && player.level().isLoaded(p.pos)
                    && player.level().getBlockEntity(p.pos) instanceof GprBlockEntity gpr) {
                gpr.shift(Integer.signum(p.amount));
            }
        }
    }

    /** A Screen calling up a CCTV for a player: their view moves into the camera at this position. */
    public record ViewCamera(BlockPos pos) implements CustomPacketPayload {
        public static final Type<ViewCamera> TYPE = new Type<>(RotaryCraft.id("view_camera"));
        public static final StreamCodec<ByteBuf, ViewCamera> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, ViewCamera::pos, ViewCamera::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The Spy Cam's view for the open screen: a colour for each of the 49 x 49 columns, and creatures as x index, z index, icon. */
    public record SpyCamData(int containerId, int[] colors, List<Integer> mobs) implements CustomPacketPayload {
        public static final Type<SpyCamData> TYPE = new Type<>(RotaryCraft.id("spy_cam_data"));
        public static final StreamCodec<ByteBuf, SpyCamData> CODEC = StreamCodec.composite(ByteBufCodecs.VAR_INT, SpyCamData::containerId,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(4096)).map(l -> l.stream().mapToInt(Integer::intValue).toArray(),
                        a -> java.util.Arrays.stream(a).boxed().toList()), SpyCamData::colors,
                ByteBufCodecs.VAR_INT.apply(ByteBufCodecs.list(1024)), SpyCamData::mobs, SpyCamData::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
