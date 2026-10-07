package net.scwunge.rotarycraft.client.machine;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.decor.MusicBoxBlockEntity;
import net.scwunge.rotarycraft.decor.MusicNote;
import net.scwunge.rotarycraft.machine.LayoutMenu;

/**
 * The Music Box screen on the original picture: save, load and demo buttons, a button for each of the sixteen channels, the note lengths and the voices, a
 * rest button, backspace and clears, and a keyboard of four octaves and a note: click a key to write that note in the chosen channel, length and voice.
 */
public class MusicScreen extends LayoutScreen {
    private static final ResourceLocation BUTTONS = RotaryCraft.id("textures/gui/music_buttons.png");
    private static final ItemStack[] VOICE_ICONS = {new ItemStack(Items.GRASS_BLOCK), new ItemStack(Items.OAK_PLANKS), new ItemStack(Items.OBSIDIAN), new ItemStack(Items.STONE),
            new ItemStack(Items.SAND), new ItemStack(Items.GLASS)};
    private static final int[] WHITE_SEMITONES = {0, 2, 4, 5, 7, 9, 11};
    private static final int KEYS_X = 12;
    private static final int KEYS_Y = 120;
    private static final int WHITE_W = 8;
    private static final int WHITE_H = 36;
    private static final int BLACK_W = 5;
    private static final int BLACK_H = 22;
    private static final boolean[] BLACK = {false, true, false, true, false, false, true, false, true, false, true, false};

    private int channel;
    private MusicNote.Length length = MusicNote.Length.WHOLE;
    private int voice = 1;
    private int flash = -1;
    private int flashTicks;

    public MusicScreen(LayoutMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        titleLabelY = -100;
        inventoryLabelY = -100;
    }

    private void press(int id) {
        minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    private void add(String text, int x, int y, int w, int h, Runnable action) {
        addRenderableWidget(Button.builder(Component.translatable(text), b -> action.run()).bounds(leftPos + x, topPos + y, w, h).build());
    }

    @Override
    protected void init() {
        super.init();
        add("gui.rotarycraft.music_box.save", 10, 6, 40, 20, () -> press(MusicBoxBlockEntity.BUTTON_SAVE));
        add("gui.rotarycraft.music_box.load", 50, 6, 40, 20, () -> press(MusicBoxBlockEntity.BUTTON_LOAD));
        add("gui.rotarycraft.music_box.demo", 168, 6, 80, 20, () -> press(MusicBoxBlockEntity.BUTTON_DEMO));
        add("gui.rotarycraft.music_box.rest", 20, 160, 216, 20, () -> press(MusicBoxBlockEntity.restId(channel, length)));
        add("gui.rotarycraft.music_box.backspace", 20, 185, 64, 20, () -> press(MusicBoxBlockEntity.backspaceId(channel)));
        add("gui.rotarycraft.music_box.clear_channel", 84, 185, 88, 20, () -> press(MusicBoxBlockEntity.clearChannelId(channel)));
        add("gui.rotarycraft.music_box.clear_music", 172, 185, 64, 20, () -> press(MusicBoxBlockEntity.BUTTON_CLEAR_ALL));
    }

    private static int whiteIndex(int pitch) {
        int count = 0;
        for (int i = 0; i < pitch % 12; i++) {
            if (!BLACK[i]) {
                count++;
            }
        }
        return pitch / 12 * 7 + count;
    }

    private static int blackLeft(int pitch) {
        return whiteIndex(pitch - 1) * WHITE_W + WHITE_W - BLACK_W / 2 - 1;
    }

    /** The pitch whose key is at the point, or -1: black keys sit over the white ones. */
    private int keyAt(double mx, double my) {
        double x = mx - leftPos - KEYS_X;
        double y = my - topPos - KEYS_Y;
        if (x < 0 || y < 0 || y >= WHITE_H || x >= 29 * WHITE_W) {
            return -1;
        }
        if (y < BLACK_H) {
            for (int pitch = 1; pitch < MusicNote.HIGHEST; pitch++) {
                if (BLACK[pitch % 12] && x >= blackLeft(pitch) && x < blackLeft(pitch) + BLACK_W) {
                    return pitch;
                }
            }
        }
        int white = (int) (x / WHITE_W);
        return white == 28 ? 48 : white / 7 * 12 + WHITE_SEMITONES[white % 7];
    }

    private boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int key = keyAt(mouseX, mouseY);
        if (key >= 0 && key <= MusicNote.HIGHEST && button == 0) {
            press(MusicBoxBlockEntity.addNoteId(channel, length, MusicNote.Voice.LIST[voice], key));
            flash = key;
            flashTicks = 4;
            return true;
        }
        for (int i = 0; i < MusicBoxBlockEntity.CHANNELS; i++) {
            if (inside(mouseX, mouseY, 9 + i * 15, 95, 12, 12)) {
                channel = i;
                return true;
            }
        }
        for (int i = 0; i < 5; i++) {
            if (inside(mouseX, mouseY, 10 + 16 * i, 53, 16, 16)) {
                length = MusicNote.Length.values()[i];
                return true;
            }
        }
        for (int i = 0; i < 6; i++) {
            if (inside(mouseX, mouseY, 152 + 16 * i, 53, 16, 16)) {
                voice = i + 1;
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    protected void containerTick() {
        super.containerTick();
        if (flashTicks > 0 && --flashTicks == 0) {
            flash = -1;
        }
    }

    @Override
    protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        super.renderBg(g, partialTick, mouseX, mouseY);
        for (int i = 0; i < 5; i++) {
            g.blit(BUTTONS, leftPos + 10 + 16 * i, topPos + 53, i * 16 + (length.ordinal() == i ? 80 : 0), 32, 16, 16);
        }
        for (int i = 0; i < 6; i++) {
            g.renderItem(VOICE_ICONS[i], leftPos + 152 + 16 * i, topPos + 53);
        }
        g.renderOutline(leftPos + 152 + (voice - 1) * 16, topPos + 53, 16, 16, 0xFF000000);
        for (int i = 0; i < MusicBoxBlockEntity.CHANNELS; i++) {
            int x = leftPos + 9 + i * 15;
            g.fill(x - 1, topPos + 94, x + 13, topPos + 108, i == channel ? 0xFFFFFFFF : 0xFF404040);
            g.fill(x, topPos + 95, x + 12, topPos + 107, 0xFF000000 | MusicBoxBlockEntity.CHANNEL_COLORS[i]);
        }
        int color = 0xFF000000 | MusicBoxBlockEntity.CHANNEL_COLORS[channel];
        int hovered = keyAt(mouseX, mouseY);
        for (int white = 0; white < 29; white++) {
            int pitch = white == 28 ? 48 : white / 7 * 12 + WHITE_SEMITONES[white % 7];
            int x = leftPos + KEYS_X + white * WHITE_W;
            g.fill(x, topPos + KEYS_Y, x + WHITE_W, topPos + KEYS_Y + WHITE_H, 0xFF202020);
            g.fill(x + 1, topPos + KEYS_Y + 1, x + WHITE_W - 1, topPos + KEYS_Y + WHITE_H - 1, pitch == flash || pitch == hovered ? color : 0xFFF4F4F4);
        }
        for (int pitch = 1; pitch < MusicNote.HIGHEST; pitch++) {
            if (BLACK[pitch % 12]) {
                int x = leftPos + KEYS_X + blackLeft(pitch);
                g.fill(x, topPos + KEYS_Y, x + BLACK_W, topPos + KEYS_Y + BLACK_H, pitch == flash || pitch == hovered ? color : 0xFF000000);
            }
        }
    }

    @Override
    protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawCenteredString(font, Component.translatable("gui.rotarycraft.music_box.length"), 51, 42, 0x000000);
        g.drawCenteredString(font, Component.translatable("gui.rotarycraft.music_box.instrument"), 200, 42, 0x000000);
        g.drawCenteredString(font, Component.translatable("gui.rotarycraft.music_box.channels"), 128, 85, 0x000000);
    }
}
