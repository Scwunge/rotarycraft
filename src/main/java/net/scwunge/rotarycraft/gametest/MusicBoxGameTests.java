package net.scwunge.rotarycraft.gametest;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.decor.MusicBoxBlockEntity;
import net.scwunge.rotarycraft.decor.MusicDiscItem;
import net.scwunge.rotarycraft.decor.MusicNote;
import net.scwunge.rotarycraft.registry.DecorRegistry;

import java.util.List;

/** The Music Box: what the screen writes, what it plays and when, discs and the demo. */
@GameTestHolder(RotaryCraft.MOD_ID)
@PrefixGameTestTemplate(false)
public class MusicBoxGameTests {
    static final String SMALL = RotaryGameTests.TEMPLATE;
    static final BlockPos AT = new BlockPos(2, 2, 2);

    static MusicBoxBlockEntity box(GameTestHelper helper, boolean powered) {
        if (powered) {
            WeaponGameTests.spinningFlywheel(helper, AT.below(), 1024, 1);
        }
        helper.setBlock(AT, DecorRegistry.MUSIC_BOX.block().get().defaultBlockState());
        return helper.getBlockEntity(AT);
    }

    static void fourQuarters(MusicBoxBlockEntity box) {
        for (int i = 0; i < 4; i++) {
            box.addNote(0, new MusicNote(MusicNote.Length.QUARTER, 20 + i, MusicNote.Voice.GUITAR));
        }
    }

    @GameTest(template = SMALL, batch = "music_screen", timeoutTicks = 40)
    public static void theScreenButtonsWriteAndEraseNotes(GameTestHelper helper) {
        MusicBoxBlockEntity box = box(helper, false);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(box.menuButton(player, MusicBoxBlockEntity.addNoteId(3, MusicNote.Length.EIGHTH, MusicNote.Voice.PLING, 30)), "note refused");
        helper.assertTrue(box.channel(3).size() == 1 && box.channel(3).get(0).equals(new MusicNote(MusicNote.Length.EIGHTH, 30, MusicNote.Voice.PLING)), "note " + box.channel(3));
        helper.assertTrue(box.menuButton(player, MusicBoxBlockEntity.addNoteId(15, MusicNote.Length.WHOLE, MusicNote.Voice.CLAVE, 48)) && box.channel(15).get(0).pitch() == 48
                && box.channel(15).get(0).voice() == MusicNote.Voice.CLAVE, "the top note of the last channel");
        helper.assertFalse(box.menuButton(player, MusicBoxBlockEntity.addNoteId(0, MusicNote.Length.WHOLE, MusicNote.Voice.GUITAR, 49)), "a pitch above the keyboard");
        helper.assertTrue(box.menuButton(player, MusicBoxBlockEntity.restId(3, MusicNote.Length.HALF)) && box.channel(3).get(1).isRest() && box.channel(3).get(1).length() == MusicNote.Length.HALF, "rest");
        helper.assertTrue(box.menuButton(player, MusicBoxBlockEntity.backspaceId(3)) && box.channel(3).size() == 1, "backspace");
        helper.assertTrue(box.menuButton(player, MusicBoxBlockEntity.clearChannelId(3)) && box.channel(3).isEmpty() && box.channel(15).size() == 1, "clear one channel");
        helper.assertTrue(box.menuButton(player, MusicBoxBlockEntity.BUTTON_CLEAR_ALL) && box.length() == 0, "clear all");
        helper.assertFalse(box.menuButton(player, 9), "a button that is not there");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "music_loop", timeoutTicks = 100)
    public static void withAKilowattItPlaysOverAndOver(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("musicBox");
        MusicBoxBlockEntity box = box(helper, true);
        fourQuarters(box);
        helper.runAfterDelay(80, () -> {
            restore.run();
            helper.assertTrue(box.notesPlayed() >= 6, "it should have gone round more than once: " + box.notesPlayed() + " notes");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "music_silent", timeoutTicks = 100)
    public static void withoutPowerOrASignalItIsSilent(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("musicBox");
        MusicBoxBlockEntity box = box(helper, false);
        fourQuarters(box);
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(box.notesPlayed() == 0, "it played with nothing to drive it");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "music_once", timeoutTicks = 150)
    public static void aRedstoneSignalPlaysItOnceThrough(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("musicBox");
        MusicBoxBlockEntity box = box(helper, false);
        fourQuarters(box);
        helper.runAfterDelay(5, () -> helper.setBlock(AT.north(), Blocks.REDSTONE_BLOCK));
        helper.runAfterDelay(120, () -> {
            restore.run();
            helper.assertTrue(box.notesPlayed() == 4, "four notes, once: " + box.notesPlayed());
            helper.assertFalse(box.isPlayingOnce(), "it should have stopped");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "music_rests", timeoutTicks = 100)
    public static void restsTakeTimeButMakeNoSound(GameTestHelper helper) {
        Runnable restore = DecorGameTests.enable("musicBox");
        MusicBoxBlockEntity box = box(helper, false);
        box.addNote(0, new MusicNote(MusicNote.Length.QUARTER, -1, MusicNote.Voice.GUITAR));
        box.addNote(0, new MusicNote(MusicNote.Length.QUARTER, 10, MusicNote.Voice.BASS));
        box.trigger();
        helper.runAfterDelay(60, () -> {
            restore.run();
            helper.assertTrue(box.notesPlayed() == 1, "only the note sounds: " + box.notesPlayed());
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "music_off", timeoutTicks = 100)
    public static void aSwitchedOffMusicBoxIsSilent(GameTestHelper helper) {
        Runnable restore = DecorGameTests.disable("musicBox");
        MusicBoxBlockEntity box = box(helper, true);
        fourQuarters(box);
        helper.runAfterDelay(40, () -> {
            restore.run();
            helper.assertTrue(box.notesPlayed() == 0, "it played while switched off");
            helper.succeed();
        });
    }

    @GameTest(template = SMALL, batch = "music_disc", timeoutTicks = 40)
    public static void aDiscCarriesTheMusic(GameTestHelper helper) {
        MusicBoxBlockEntity box = box(helper, false);
        fourQuarters(box);
        box.addNote(5, new MusicNote(MusicNote.Length.SIXTEENTH, 3, MusicNote.Voice.SNARE));
        ItemStack disc = new ItemStack(DecorRegistry.MUSIC_DISC.get());
        helper.assertFalse(box.loadFromDisc(disc), "a blank disc has nothing to load");
        helper.assertFalse(box.saveToDisc(new ItemStack(net.minecraft.world.item.Items.STICK)), "only discs hold music");
        helper.assertTrue(box.saveToDisc(disc), "save");
        helper.assertTrue(MusicDiscItem.counts(disc)[0] == 4 && MusicDiscItem.counts(disc)[5] == 1, "the disc should say what it holds");
        box.clearMusic();
        helper.assertTrue(box.length() == 0, "cleared");
        helper.assertTrue(box.loadFromDisc(disc) && box.channel(0).size() == 4 && box.channel(5).get(0).voice() == MusicNote.Voice.SNARE, "load");
        helper.succeed();
    }

    @GameTest(template = SMALL, batch = "music_files", timeoutTicks = 40)
    public static void theOriginalsMusicFileFormatRoundTripsAndTheDemoLoads(GameTestHelper helper) {
        MusicBoxBlockEntity box = box(helper, false);
        fourQuarters(box);
        box.addNote(2, new MusicNote(MusicNote.Length.HALF, 7, MusicNote.Voice.BASS));
        List<String> lines = box.toLines();
        helper.assertTrue(lines.size() == 4 && lines.get(0).startsWith("2:20:1;-;1:7:2;"), "lines " + lines);
        MusicBoxBlockEntity other = box(helper, false);
        other.fromLines(lines);
        helper.assertTrue(other.channel(0).equals(box.channel(0)) && other.channel(2).equals(box.channel(2)), "round trip");
        other.loadDemo();
        helper.assertTrue(other.length() > 100 && other.isPlayingOnce(), "the demo is a long piece that plays once: " + other.length() + " rows");
        helper.assertTrue(other.channel(0).get(0).equals(new MusicNote(MusicNote.Length.WHOLE, 19, MusicNote.Voice.GUITAR)), "first note " + other.channel(0).get(0));
        helper.succeed();
    }
}
