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

/** The Mob Radar's container: no slots; while it is open the server tells the screen what the radar sees, five times a second. */
public class RadarMenu extends AbstractContainerMenu {
    private final MobRadarBlockEntity radar;
    private final Inventory inventory;
    private MobRadarBlockEntity.Scan scan = new MobRadarBlockEntity.Scan(0, List.of());

    public RadarMenu(int id, Inventory inventory, MobRadarBlockEntity radar) {
        super(SurveyRegistry.RADAR_MENU.get(), id);
        this.radar = radar;
        this.inventory = inventory;
    }

    public static RadarMenu fromNetwork(int id, Inventory inventory, RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new RadarMenu(id, inventory, (MobRadarBlockEntity) inventory.player.level().getBlockEntity(pos));
    }

    public MobRadarBlockEntity.Scan scan() {
        return scan;
    }

    public void setScan(MobRadarBlockEntity.Scan scan) {
        this.scan = scan;
    }

    @Override
    public void broadcastChanges() {
        super.broadcastChanges();
        if (inventory.player instanceof ServerPlayer player && player.level().getGameTime() % 4 == 0) {
            MobRadarBlockEntity.Scan now = radar.scan();
            PacketDistributor.sendToPlayer(player, SurveyNetwork.RadarData.of(containerId, now));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        return !radar.isRemoved() && player.distanceToSqr(radar.getBlockPos().getCenter()) <= 64;
    }
}
