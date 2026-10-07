package net.scwunge.rotarycraft.logistics;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

import java.util.List;

/** What a Scale-able Chest held when it was broken: it keeps its inventory in the item, the way the original did, rather than spilling nine hundred stacks. */
public record ChestContents(List<Entry> entries) {
    public record Entry(int slot, ItemStack stack) {
        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.fieldOf("slot").forGetter(Entry::slot),
                ItemStack.CODEC.fieldOf("item").forGetter(Entry::stack)
        ).apply(i, Entry::new));
        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_INT, Entry::slot, ItemStack.STREAM_CODEC, Entry::stack, Entry::new);
    }

    public static final Codec<ChestContents> CODEC = Entry.CODEC.listOf().xmap(ChestContents::new, ChestContents::entries);
    public static final StreamCodec<RegistryFriendlyByteBuf, ChestContents> STREAM_CODEC = Entry.STREAM_CODEC.apply(ByteBufCodecs.list()).map(ChestContents::new, ChestContents::entries);
}
