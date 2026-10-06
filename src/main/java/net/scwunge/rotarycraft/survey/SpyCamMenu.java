package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

import java.util.List;

/** The Spy Cam's map screen: no slots; while it is open the server sends the view once a second. */
public class SpyCamMenu extends AbstractContainerMenu {
    private final SpyCamBlockEntity cam;
    private final Inventory inventory;
    private int[] colors = new int[SpyCamBlockEntity.SIDE * SpyCamBlockEntity.SIDE];
    private List<Integer> mobs = List.of();

    public SpyCamMenu(int id, Inventory inventory, SpyCamBlockEntity cam) {
        super(SurveyRegistry.SPY_CAM_MENU.get(), id);
        this.cam = cam;
        this.inventory = inventory;
    }

    public static SpyCamMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new SpyCamMenu(id, inventory, (SpyCamBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public int[] colors() {
        return colors;
    }

    public List<Integer> mobs() {
        return mobs;
    }

    public void setView(int[] colors, List<Integer> mobs) {
        this.colors = colors;
        this.mobs = mobs;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player && player.level().getGameTime() % 10 == 0 && cam.isOn()) {
            SpyCamBlockEntity.View view = cam.view();
            PacketDistributor.sendToPlayer(player, new SurveyNetwork.SpyCamData(containerId, view.colors(), view.mobs()));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    /** Open while it is on, and for someone who is near enough to a Screen to be using it (the view is a remote one, so distance is not checked). */
    @Override
    public boolean stillValid(Player player) {
        return !cam.isRemoved() && cam.isOn();
    }
}
