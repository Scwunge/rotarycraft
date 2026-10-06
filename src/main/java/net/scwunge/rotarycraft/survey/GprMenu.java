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

/** The GPR's container: no slots; while it is open the server sends the slice it has scanned whenever it changes. */
public class GprMenu extends AbstractContainerMenu {
    private final GprBlockEntity gpr;
    private final Inventory inventory;
    private long sentVersion = -1;
    private int range;
    private BlockPos centre = BlockPos.ZERO;
    private List<Integer> palette = List.of();
    private byte[] columns = new byte[0];

    public GprMenu(int id, Inventory inventory, GprBlockEntity gpr) {
        super(SurveyRegistry.GPR_MENU.get(), id);
        this.gpr = gpr;
        this.inventory = inventory;
    }

    public static GprMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new GprMenu(id, inventory, (GprBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public GprBlockEntity gpr() {
        return gpr;
    }

    public BlockPos pos() {
        return gpr.getBlockPos();
    }

    public int range() {
        return range;
    }

    public BlockPos centre() {
        return centre;
    }

    public List<Integer> palette() {
        return palette;
    }

    public byte[] columns() {
        return columns;
    }

    public void setData(int range, BlockPos centre, List<Integer> palette, byte[] columns) {
        this.range = range;
        this.centre = centre;
        this.palette = palette;
        this.columns = columns;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player && gpr.scannedRange() >= 0 && (gpr.version() != sentVersion || player.level().getGameTime() % 40 == 0)) {
            sentVersion = gpr.version();
            int r = gpr.scannedRange();
            PacketDistributor.sendToPlayer(player, new SurveyNetwork.GprData(containerId, r, gpr.centre(), List.copyOf(gpr.palette()), gpr.columns(r)));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !gpr.isRemoved() && player.distanceToSqr(gpr.getBlockPos().getCenter()) <= 64;
    }
}
