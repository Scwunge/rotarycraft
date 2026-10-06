package net.scwunge.rotarycraft.weapon;

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
import net.scwunge.rotarycraft.client.weapon.WeaponClientHooks;
import net.scwunge.rotarycraft.weapon.turret.TurretBlockEntity;

import java.util.List;

/** The weapons' packets: a turret's whitelist sent to its owner's screen, and names the owner takes off it. */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public final class WeaponNetwork {
    private WeaponNetwork() {}

    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("weapons-1").optional();
        // The handler body only runs on the client, so the client hooks never load on a dedicated server.
        registrar.playToClient(SafePlayers.TYPE, SafePlayers.CODEC, (p, ctx) -> ctx.enqueueWork(() -> WeaponClientHooks.openSafePlayers(p.pos(), p.names())));
        registrar.playToServer(RemoveSafePlayer.TYPE, RemoveSafePlayer.CODEC, (p, ctx) -> ctx.enqueueWork(() -> RemoveSafePlayer.handle(p, ctx)));
        registrar.playToServer(CannonSettings.TYPE, CannonSettings.CODEC, (p, ctx) -> ctx.enqueueWork(() -> CannonSettings.handle(p, ctx)));
    }

    public record SafePlayers(BlockPos pos, List<String> names) implements CustomPacketPayload {
        public static final Type<SafePlayers> TYPE = new Type<>(RotaryCraft.id("safe_players"));
        public static final StreamCodec<ByteBuf, SafePlayers> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, SafePlayers::pos,
                ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list(256)), SafePlayers::names, SafePlayers::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** The TNT Cannon's screen sending its settings: mode, compass bearing, elevation, speed, fuse and target block. */
    public record CannonSettings(BlockPos pos, boolean targetMode, int phi, int theta, int velocity, int fuse, BlockPos target) implements CustomPacketPayload {
        public static final Type<CannonSettings> TYPE = new Type<>(RotaryCraft.id("cannon_settings"));
        public static final StreamCodec<ByteBuf, CannonSettings> CODEC = StreamCodec.of((buf, p) -> {
            BlockPos.STREAM_CODEC.encode(buf, p.pos);
            buf.writeBoolean(p.targetMode).writeInt(p.phi).writeInt(p.theta).writeInt(p.velocity).writeInt(p.fuse);
            BlockPos.STREAM_CODEC.encode(buf, p.target);
        }, buf -> new CannonSettings(BlockPos.STREAM_CODEC.decode(buf), buf.readBoolean(), buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt(),
                BlockPos.STREAM_CODEC.decode(buf)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only someone standing at the cannon can change it. */
        static void handle(CannonSettings p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (player.distanceToSqr(p.pos.getCenter()) <= 64 && player.level().isLoaded(p.pos)
                    && player.level().getBlockEntity(p.pos) instanceof net.scwunge.rotarycraft.weapon.turret.TntCannonBlockEntity cannon) {
                cannon.configure(p.targetMode, p.phi, p.theta, p.velocity, p.fuse, p.target);
            }
        }
    }

    public record RemoveSafePlayer(BlockPos pos, String name) implements CustomPacketPayload {
        public static final Type<RemoveSafePlayer> TYPE = new Type<>(RotaryCraft.id("remove_safe_player"));
        public static final StreamCodec<ByteBuf, RemoveSafePlayer> CODEC = StreamCodec.composite(BlockPos.STREAM_CODEC, RemoveSafePlayer::pos,
                ByteBufCodecs.stringUtf8(64), RemoveSafePlayer::name, RemoveSafePlayer::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        /** Only the turret's owner, standing near it, can take a name off its list. */
        static void handle(RemoveSafePlayer p, IPayloadContext ctx) {
            Player player = ctx.player();
            if (player.distanceToSqr(p.pos.getCenter()) <= 64 && player.level().isLoaded(p.pos)
                    && player.level().getBlockEntity(p.pos) instanceof TurretBlockEntity turret && turret.isOwner(player.getUUID())) {
                turret.removeSafePlayer(p.name);
            }
        }
    }
}
