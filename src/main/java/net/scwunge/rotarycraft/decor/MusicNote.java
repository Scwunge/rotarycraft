package net.scwunge.rotarycraft.decor;

import net.minecraft.nbt.CompoundTag;

/** One note of a Music Box channel (TileEntityMusicBox.Note): how long, how high (0 is the original's lowest, F# of the bottom octave; below 0 is a rest) and which voice. */
public record MusicNote(Length length, int pitch, Voice voice) {
    /** The ticks each length lasts. */
    public enum Length {
        WHOLE(48), HALF(24), QUARTER(12), EIGHTH(6), SIXTEENTH(3);

        public final int ticks;

        Length(int ticks) {
            this.ticks = ticks;
        }

        public String display() {
            return name().charAt(0) + name().substring(1).toLowerCase(java.util.Locale.ROOT);
        }
    }

    /** The voices, in the original's order (the first, rest, is not one you pick). */
    public enum Voice {
        REST, GUITAR, BASS, PLING, BASSDRUM, SNARE, CLAVE;

        public static final Voice[] LIST = values();

        public boolean isPitched() {
            return ordinal() < 4;
        }
    }

    public static final int HIGHEST = 48;

    public boolean isRest() {
        return pitch < 0;
    }

    public MusicNote asRest() {
        return new MusicNote(length, -1, voice);
    }

    /** The original's "length:pitch:voice" form, used in its music files. */
    public String serial() {
        return length.ordinal() + ":" + pitch + ":" + voice.ordinal();
    }

    public static MusicNote parse(String s) {
        if (s.equals("-") || s.isBlank()) {
            return null;
        }
        String[] parts = s.split(":");
        return new MusicNote(Length.values()[Integer.parseInt(parts[0])], Integer.parseInt(parts[1]), Voice.values()[Integer.parseInt(parts[2])]);
    }

    public CompoundTag toTag() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("len", length.ordinal());
        tag.putInt("pch", pitch);
        tag.putInt("vc", voice.ordinal());
        return tag;
    }

    public static MusicNote fromTag(CompoundTag tag) {
        return new MusicNote(Length.values()[Math.floorMod(tag.getInt("len"), Length.values().length)], tag.getInt("pch"), Voice.values()[Math.floorMod(tag.getInt("vc"), Voice.values().length)]);
    }
}
