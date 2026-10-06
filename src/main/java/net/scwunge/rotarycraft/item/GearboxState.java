package net.scwunge.rotarycraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** A broken-out gearbox keeps its wear, lubricant and fitted bearing, as in the original. */
public record GearboxState(int damage, int lubricant, String bearing) {
    public static final Codec<GearboxState> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("damage", 0).forGetter(GearboxState::damage),
            Codec.INT.optionalFieldOf("lubricant", 0).forGetter(GearboxState::lubricant),
            Codec.STRING.optionalFieldOf("bearing", "").forGetter(GearboxState::bearing)
    ).apply(i, GearboxState::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, GearboxState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, GearboxState::damage,
            ByteBufCodecs.VAR_INT, GearboxState::lubricant,
            ByteBufCodecs.STRING_UTF8, GearboxState::bearing,
            GearboxState::new);
}
