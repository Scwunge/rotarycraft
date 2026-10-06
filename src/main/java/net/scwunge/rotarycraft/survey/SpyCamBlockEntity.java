package net.scwunge.rotarycraft.survey;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.registry.SurveyRegistry;

import java.util.ArrayList;
import java.util.List;

/**
 * Spy Cam, as the original: a camera on a wound coil that, called up from a Screen with the same three dyes, shows a map of the
 * ground within 24 blocks of it, seen from above and shaded by how far down the top block is from the camera, with every creature
 * above the ground on it. It sees only what is at or below its own height.
 */
public class SpyCamBlockEntity extends RemoteMachineBlockEntity {
    public static final int RANGE = 24;
    public static final int SIDE = 2 * RANGE + 1;

    public SpyCamBlockEntity(BlockPos pos, BlockState state) {
        super(SurveyRegistry.SPY_CAM_BE.get(), pos, state);
    }

    @Override
    public void activate(Player player) {
        if (isOn() && player instanceof ServerPlayer sp) {
            sp.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SpyCamMenu(id, inventory, this), Component.translatable("gui.rotarycraft.spy_cam")),
                    buf -> buf.writeBlockPos(getBlockPos()));
        }
    }

    /** The map: a colour (0xRRGGBB, already shaded by depth) for each column, x across and z down the way offsets grow, and the creatures on it. */
    public record View(int[] colors, List<Integer> mobs) {}

    public View view() {
        Level world = level;
        int[] colors = new int[SIDE * SIDE];
        int y = worldPosition.getY();
        int[] tops = new int[SIDE * SIDE];
        for (int i = -RANGE; i <= RANGE; i++) {
            for (int j = -RANGE; j <= RANGE; j++) {
                int x = worldPosition.getX() + i, z = worldPosition.getZ() + j;
                int top = world.getMinBuildHeight();
                int color = 0;
                for (int dy = y; dy >= world.getMinBuildHeight(); dy--) {
                    BlockPos at = new BlockPos(x, dy, z);
                    if (!world.hasChunkAt(at)) {
                        break;
                    }
                    BlockState state = world.getBlockState(at);
                    if (!state.isAir()) {
                        top = dy;
                        color = state.getMapColor(world, at).col;
                        break;
                    }
                }
                float brightness = Math.max(0, 1 - (y - top) / (float) Math.max(1, y - world.getMinBuildHeight()) * 1.25f);
                int r = (int) ((color >> 16 & 255) * brightness), g = (int) ((color >> 8 & 255) * brightness), b = (int) ((color & 255) * brightness);
                colors[(i + RANGE) * SIDE + j + RANGE] = r << 16 | g << 8 | b;
                tops[(i + RANGE) * SIDE + j + RANGE] = top;
            }
        }
        List<Integer> mobs = new ArrayList<>();
        AABB zone = new AABB(worldPosition.getX() - RANGE, world.getMinBuildHeight(), worldPosition.getZ() - RANGE, worldPosition.getX() + 1 + RANGE, y + 1,
                worldPosition.getZ() + 1 + RANGE);
        for (LivingEntity e : world.getEntitiesOfClass(LivingEntity.class, zone)) {
            int ex = (int) Math.floor(e.getX()) - worldPosition.getX(), ez = (int) Math.floor(e.getZ()) - worldPosition.getZ();
            if (Math.abs(ex) <= RANGE && Math.abs(ez) <= RANGE && e.getY() >= tops[(ex + RANGE) * SIDE + ez + RANGE]) {
                mobs.add(ex + RANGE);
                mobs.add(ez + RANGE);
                mobs.add(MobRadarBlockEntity.icon(e.getType()));
            }
        }
        return new View(colors, mobs);
    }
}
