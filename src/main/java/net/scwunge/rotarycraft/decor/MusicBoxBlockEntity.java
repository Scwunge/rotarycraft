package net.scwunge.rotarycraft.decor;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.state.BlockState;
import net.scwunge.rotarycraft.config.MachineConfig;
import net.scwunge.rotarycraft.machine.GuiLayout;
import net.scwunge.rotarycraft.machine.InventoryMachineBlockEntity;
import net.scwunge.rotarycraft.power.PowerRequirement;
import net.scwunge.rotarycraft.registry.DecorRegistry;
import net.scwunge.rotarycraft.registry.MachineSoundRegistry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Music Box (TileEntityMusicBox): a sequencer with sixteen channels, each a line of notes (a length, a pitch of up to 49 semitones and one of six voices) written
 * on its screen. Without power it plays once through when it gets a redstone signal; with 1 kW or more from any side it plays over and over. Music can be put
 * on a Music Disc (and the box loaded from one) and a demo piece is built in. The screen's buttons carry what to do in the id the screen sends: see
 * {@link #addNoteId}, {@link #restId}, {@link #backspaceId}, {@link #clearChannelId}.
 */
public class MusicBoxBlockEntity extends InventoryMachineBlockEntity {
    public static final String NAME = "music_box";
    public static final int CHANNELS = 16;
    public static final int LOOP_POWER = 1024;
    public static final PowerRequirement REQUIREMENT = new PowerRequirement(0, 0, 0);
    public static final GuiLayout LAYOUT = GuiLayout.named(NAME).size(256, 217).noInventory().customScreen().build();
    /** The channel colours of the screen. */
    public static final int[] CHANNEL_COLORS = {0x3636FF, 0xD336FF, 0xFFACAC, 0xFF3636, 0xFFAC36, 0xD3D336, 0x65BC8F, 0x36D336, 0x36FFFF, 0x58ABF9, 0x8484FF, 0xFF36FF, 0x8436FF,
            0xB49C8A, 0x8FA9B5, 0x94B581};

    public static final int BUTTON_SAVE = 100;
    public static final int BUTTON_LOAD = 101;
    public static final int BUTTON_DEMO = 102;
    public static final int BUTTON_CLEAR_ALL = 106;

    private final List<MusicNote>[] channels = new List[CHANNELS];
    private final int[] delay = new int[CHANNELS];
    private final int[] index = new int[CHANNELS];
    private boolean playingOnce;
    private boolean lastSignal;
    private int notesPlayed;

    public MusicBoxBlockEntity(BlockPos pos, BlockState state) {
        super(DecorRegistry.MUSIC_BOX.type().get(), pos, state, 0, NAME);
        for (int i = 0; i < CHANNELS; i++) {
            channels[i] = new ArrayList<>();
        }
    }

    @Override
    public GuiLayout layout() {
        return LAYOUT;
    }

    @Override
    public PowerRequirement requirement() {
        return REQUIREMENT;
    }

    @Override
    protected boolean omniSided() {
        return true;
    }

    // ---- screen button ids ----

    public static int addNoteId(int channel, MusicNote.Length length, MusicNote.Voice voice, int pitch) {
        return 10_000 + (((channel * 5 + length.ordinal()) * 6 + (voice.ordinal() - 1)) * 64 + pitch);
    }

    public static int restId(int channel, MusicNote.Length length) {
        return 1000 + channel * 8 + length.ordinal();
    }

    public static int backspaceId(int channel) {
        return 2000 + channel;
    }

    public static int clearChannelId(int channel) {
        return 3000 + channel;
    }

    @Override
    public boolean menuButton(Player player, int id) {
        if (id >= 10_000) {
            int n = id - 10_000;
            int pitch = n % 64;
            int voice = n / 64 % 6 + 1;
            int length = n / 64 / 6 % 5;
            int channel = n / 64 / 6 / 5;
            if (channel >= CHANNELS || pitch > MusicNote.HIGHEST) {
                return false;
            }
            addNote(channel, new MusicNote(MusicNote.Length.values()[length], pitch, MusicNote.Voice.values()[voice]));
            return true;
        }
        if (id >= 3000 && id < 3000 + CHANNELS) {
            clearChannel(id - 3000);
            return true;
        }
        if (id >= 2000 && id < 2000 + CHANNELS) {
            backspace(id - 2000);
            return true;
        }
        if (id >= 1000 && id < 1000 + CHANNELS * 8) {
            int channel = (id - 1000) / 8;
            int length = (id - 1000) % 8;
            if (length >= MusicNote.Length.values().length) {
                return false;
            }
            addNote(channel, new MusicNote(MusicNote.Length.values()[length], -1, MusicNote.Voice.GUITAR));
            return true;
        }
        switch (id) {
            case BUTTON_SAVE -> {
                return saveToDisc(player.getMainHandItem());
            }
            case BUTTON_LOAD -> {
                return loadFromDisc(player.getMainHandItem());
            }
            case BUTTON_DEMO -> {
                loadDemo();
                return true;
            }
            case BUTTON_CLEAR_ALL -> {
                clearMusic();
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ---- the music ----

    public List<MusicNote> channel(int channel) {
        return java.util.Collections.unmodifiableList(channels[channel]);
    }

    public int length() {
        int size = 0;
        for (List<MusicNote> channel : channels) {
            size = Math.max(size, channel.size());
        }
        return size;
    }

    public void addNote(int channel, MusicNote note) {
        channels[channel].add(note);
        setChanged();
    }

    public void backspace(int channel) {
        if (!channels[channel].isEmpty()) {
            channels[channel].remove(channels[channel].size() - 1);
            setChanged();
        }
    }

    public void clearChannel(int channel) {
        channels[channel].clear();
        index[channel] = 0;
        setChanged();
    }

    public void clearMusic() {
        for (int i = 0; i < CHANNELS; i++) {
            clearChannel(i);
        }
    }

    /** How many notes it has sounded (for tests, and a look at what it is doing). */
    public int notesPlayed() {
        return notesPlayed;
    }

    public boolean isPlayingOnce() {
        return playingOnce;
    }

    /** Starts it from the beginning, for one play-through. */
    public void trigger() {
        playingOnce = true;
        restart();
    }

    private void restart() {
        for (int i = 0; i < CHANNELS; i++) {
            index[i] = 0;
            delay[i] = 0;
        }
    }

    private boolean atEnd() {
        for (int i = 0; i < CHANNELS; i++) {
            if (index[i] < channels[i].size() - 1) {
                return false;
            }
        }
        return true;
    }

    private boolean noDelays() {
        for (int d : delay) {
            if (d > 0) {
                return false;
            }
        }
        return true;
    }

    // ---- the original's music files ----

    /** The music as lines of sixteen "length:pitch:voice" columns (or "-"), the original's file form. */
    public List<String> toLines() {
        List<String> lines = new ArrayList<>();
        for (int row = 0; row < length(); row++) {
            StringBuilder sb = new StringBuilder();
            for (List<MusicNote> channel : channels) {
                sb.append(channel.size() > row ? channel.get(row).serial() : "-").append(';');
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    public void fromLines(List<String> lines) {
        clearMusic();
        for (String line : lines) {
            String[] pieces = line.split(";");
            for (int i = 0; i < CHANNELS && i < pieces.length; i++) {
                MusicNote note = MusicNote.parse(pieces[i]);
                if (note != null) {
                    channels[i].add(note);
                }
            }
        }
        setChanged();
    }

    /** Loads the demo piece that comes with the mod, and plays it once. */
    public void loadDemo() {
        try (InputStream in = MusicBoxBlockEntity.class.getResourceAsStream("/data/rotarycraft/music/demo.rcmusic")) {
            if (in == null) {
                return;
            }
            List<String> lines = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (!line.isBlank()) {
                    lines.add(line);
                }
            }
            fromLines(lines);
            trigger();
        } catch (IOException | RuntimeException e) {
            // a broken file leaves the music as it was
        }
    }

    // ---- discs ----

    public boolean saveToDisc(ItemStack disc) {
        if (!disc.is(DecorRegistry.MUSIC_DISC.get())) {
            return false;
        }
        CompoundTag tag = new CompoundTag();
        for (int i = 0; i < CHANNELS; i++) {
            ListTag list = new ListTag();
            for (MusicNote note : channels[i]) {
                list.add(note.toTag());
            }
            tag.put("ch" + i, list);
        }
        disc.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        return true;
    }

    public boolean loadFromDisc(ItemStack disc) {
        if (!disc.is(DecorRegistry.MUSIC_DISC.get())) {
            return false;
        }
        CustomData data = disc.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return false;
        }
        CompoundTag tag = data.copyTag();
        clearMusic();
        for (int i = 0; i < CHANNELS; i++) {
            for (Tag t : tag.getList("ch" + i, Tag.TAG_COMPOUND)) {
                channels[i].add(MusicNote.fromTag((CompoundTag) t));
            }
        }
        setChanged();
        return true;
    }

    // ---- playing ----

    private static SoundEvent soundOf(String voice, String range) {
        return MachineSoundRegistry.get("note_" + voice + range).get();
    }

    /** Sounds a note at the box, as the original does: higher and lower than the sound files reach by shifting their pitch. */
    public void play(ServerLevel server, MusicNote note) {
        if (note.isRest()) {
            return;
        }
        float pitch = (float) Math.pow(2.0D, (note.pitch() - 24) / 12.0D);
        String range = "";
        if (pitch < 0.5F) {
            pitch *= 2F;
            range = "_low";
        } else if (pitch > 2F) {
            pitch *= 0.25F;
            range = "_high";
        }
        SoundEvent sound = switch (note.voice()) {
            case GUITAR -> soundOf("harp", range);
            case BASS -> soundOf("bass", range);
            case PLING -> soundOf("pling", range);
            case BASSDRUM -> SoundEvents.NOTE_BLOCK_BASEDRUM.value();
            case SNARE -> SoundEvents.NOTE_BLOCK_SNARE.value();
            case CLAVE -> SoundEvents.NOTE_BLOCK_HAT.value();
            default -> null;
        };
        if (sound == null) {
            return;
        }
        server.playSound(null, worldPosition, sound, SoundSource.RECORDS, 2F, Math.max(0.5F, Math.min(2F, pitch)));
        server.sendParticles(ParticleTypes.NOTE, worldPosition.getX() + 0.5, worldPosition.getY() + 1.2, worldPosition.getZ() + 0.5, 0, note.pitch() / 24D, 0, 0, 1);
        notesPlayed++;
    }

    @Override
    protected void machineTick(boolean powered) {
        if (!(level instanceof ServerLevel server)) {
            return;
        }
        boolean signal = level.hasNeighborSignal(worldPosition);
        if (signal && !lastSignal) {
            trigger();
        }
        lastSignal = signal;
        if (!MachineConfig.enabled("musicBox")) {
            return;
        }
        boolean looping = getPower() >= LOOP_POWER;
        if (looping) {
            playingOnce = false;
        } else if (!playingOnce) {
            restart();
            return;
        }
        for (int i = 0; i < CHANNELS; i++) {
            if (delay[i] > 0) {
                delay[i]--;
            }
            if (delay[i] == 0) {
                if (channels[i].isEmpty()) {
                    index[i] = 0;
                } else if (index[i] < channels[i].size()) {
                    MusicNote note = channels[i].get(index[i]);
                    play(server, note);
                    delay[i] = note.length().ticks;
                    index[i]++;
                }
            }
        }
        if (atEnd() && noDelays()) {
            restart();
            playingOnce = false;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        for (int i = 0; i < CHANNELS; i++) {
            ListTag list = new ListTag();
            for (MusicNote note : channels[i]) {
                list.add(note.toTag());
            }
            tag.put("ch" + i, list);
        }
        tag.putBoolean("once", playingOnce);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        for (int i = 0; i < CHANNELS; i++) {
            channels[i].clear();
            for (Tag t : tag.getList("ch" + i, Tag.TAG_COMPOUND)) {
                channels[i].add(MusicNote.fromTag((CompoundTag) t));
            }
        }
        playingOnce = tag.getBoolean("once");
    }
}
