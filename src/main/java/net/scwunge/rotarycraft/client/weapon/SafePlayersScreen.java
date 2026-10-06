package net.scwunge.rotarycraft.client.weapon;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.weapon.WeaponNetwork;

import java.util.ArrayList;
import java.util.List;

/** A turret's whitelist, opened by its owner with a Cannon Key: the players it will not shoot, each with a button to take them off. */
public class SafePlayersScreen extends Screen {
    private static final int ROWS = 10;
    private final BlockPos pos;
    private final List<String> names;
    private int scroll;

    public SafePlayersScreen(BlockPos pos, List<String> names) {
        super(Component.translatable("gui.rotarycraft.safe_players"));
        this.pos = pos;
        this.names = new ArrayList<>(names);
    }

    @Override
    protected void init() {
        clearWidgets();
        int x = width / 2;
        int top = height / 2 - ROWS * 11;
        for (int i = 0; i < ROWS && scroll + i < names.size(); i++) {
            String name = names.get(scroll + i);
            addRenderableWidget(Button.builder(Component.translatable("gui.rotarycraft.safe_players.remove"), b -> {
                PacketDistributor.sendToServer(new WeaponNetwork.RemoveSafePlayer(pos, name));
                names.remove(name);
                scroll = Math.max(0, Math.min(scroll, names.size() - ROWS));
                init();
            }).bounds(x + 40, top + i * 22, 60, 20).build());
        }
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose()).bounds(x - 50, top + ROWS * 22 + 6, 100, 20).build());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scroll = Math.max(0, Math.min(scroll - (int) Math.signum(scrollY), Math.max(0, names.size() - ROWS)));
        init();
        return true;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = width / 2;
        int top = height / 2 - ROWS * 11;
        g.drawCenteredString(font, title, x, top - 16, 0xFFFFFF);
        if (names.isEmpty()) {
            g.drawCenteredString(font, Component.translatable("gui.rotarycraft.safe_players.none"), x, top + 6, 0xAAAAAA);
        }
        for (int i = 0; i < ROWS && scroll + i < names.size(); i++) {
            g.drawString(font, names.get(scroll + i), x - 100, top + i * 22 + 6, 0xFFFFFF);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
