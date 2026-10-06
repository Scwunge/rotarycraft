package net.scwunge.rotarycraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Which ore an Extractor product came from ("iron", "tin", ...) and the tint it is drawn with. */
public record OreProduct(String type, int color) {
    public static final Codec<OreProduct> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.STRING.fieldOf("type").forGetter(OreProduct::type),
            Codec.INT.fieldOf("color").forGetter(OreProduct::color)
    ).apply(i, OreProduct::new));
    public static final StreamCodec<ByteBuf, OreProduct> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, OreProduct::type,
            ByteBufCodecs.INT, OreProduct::color,
            OreProduct::new);
}
