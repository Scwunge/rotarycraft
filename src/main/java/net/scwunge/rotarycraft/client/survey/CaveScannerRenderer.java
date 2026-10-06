package net.scwunge.rotarycraft.client.survey;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;
import net.scwunge.rotarycraft.survey.CaveScannerBlockEntity;
import org.joml.Vector3f;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

/**
 * The Cave Scanner, drawn from the original's model, and the cave outline: every corner in its box where cave air (underground
 * air, below sky-light) meets rock, drawn as a point coloured by height, seen through the ground, with the box's edges. The
 * client works the outline out a little each frame, so a big box fills in over a moment, and redoes it every second or so.
 */
public class CaveScannerRenderer implements BlockEntityRenderer<CaveScannerBlockEntity> {
    static final ReikaModel MODEL = new ReikaModel("cave");
    static final ResourceLocation TEXTURE = RotaryCraft.id("textures/machine/cave_scanner.png");
    private static final int MAX_POINTS = 60000;
    private static final long BUDGET_NANOS = 2_000_000;
    private static final int[] DEPTH_COLORS = new int[512];

    private final Map<BlockPos, Field> fields = new HashMap<>();

    /** One scanner's outline, and the scan of it in progress. */
    private static final class Field {
        BlockPos source;
        int range;
        IntArrayList points = new IntArrayList();
        IntArrayList building;
        long next;
        long startedAt = Long.MIN_VALUE;
        int cursor;
        long lastSeen;
    }

    static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        pose.pushPose();
        ReikaModel.enterModelSpace(pose);
        MODEL.renderAll(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE)), light, overlay);
        pose.popPose();
    }

    @Override
    public void render(CaveScannerBlockEntity scanner, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        draw(pose, buffers, light, overlay);
        Level level = scanner.getLevel();
        if (level == null) {
            return;
        }
        long time = level.getGameTime();
        if (fields.size() > 8) {
            fields.values().removeIf(f -> time - f.lastSeen > 200);
        }
        if (!scanner.isOn()) {
            fields.remove(scanner.getBlockPos());
            return;
        }
        Field f = fields.computeIfAbsent(scanner.getBlockPos(), k -> new Field());
        f.lastSeen = time;
        int range = scanner.range();
        if (!scanner.source().equals(f.source) || range != f.range) {
            f.source = scanner.source();
            f.range = range;
            f.building = null;
            f.next = 0;
        }
        advance(f, level, time);
        BlockPos origin = scanner.getBlockPos();
        drawPoints(f, pose, buffers, origin);
    }

    /** Scans some more of the box, within the frame's budget. */
    private static void advance(Field f, Level level, long time) {
        if (f.building == null) {
            if (time < f.next) {
                return;
            }
            f.building = new IntArrayList();
            f.cursor = 0;
        }
        int side = 2 * f.range + 2;
        int total = side * side * side;
        long deadline = System.nanoTime() + BUDGET_NANOS;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        while (f.cursor < total) {
            if ((f.cursor & 255) == 0 && System.nanoTime() > deadline) {
                return;
            }
            int c = f.cursor++;
            int x = f.source.getX() - f.range + c % side;
            int y = f.source.getY() - f.range + (c / side) % side;
            int z = f.source.getZ() - f.range + c / (side * side);
            if (f.building.size() < MAX_POINTS * 3 && isCaveCorner(level, at, x, y, z)) {
                f.building.add(x);
                f.building.add(y);
                f.building.add(z);
            }
        }
        f.points = f.building;
        f.building = null;
        f.next = time + (f.range < 64 ? 20 : f.range < 128 ? 40 : 80);
    }

    /** Whether the corner at (x, y, z) has cave air and rock around it. */
    private static boolean isCaveCorner(Level level, BlockPos.MutableBlockPos at, int x, int y, int z) {
        boolean cave = false;
        boolean rock = false;
        for (int dx = -1; dx <= 0; dx++) {
            for (int dy = -1; dy <= 0; dy++) {
                for (int dz = -1; dz <= 0; dz++) {
                    at.set(x + dx, y + dy, z + dz);
                    if (!level.isLoaded(at) || at.getY() < level.getMinBuildHeight() || at.getY() >= level.getMaxBuildHeight()) {
                        return false;
                    }
                    BlockState state = level.getBlockState(at);
                    if (state.isAir()) {
                        cave |= level.getBrightness(LightLayer.SKY, at) < 8;
                    } else {
                        rock = true;
                    }
                }
            }
        }
        return cave && rock;
    }

    private static int depthColor(int y) {
        int i = Math.max(0, Math.min(511, y + 128));
        if (DEPTH_COLORS[i] == 0) {
            DEPTH_COLORS[i] = 0xFF000000 | Color.HSBtoRGB(((Math.abs(y) - 12) % 64) / 64f, 1, 1);
        }
        return DEPTH_COLORS[i];
    }

    private static void drawPoints(Field f, PoseStack pose, MultiBufferSource buffers, BlockPos origin) {
        var camera = Minecraft.getInstance().gameRenderer.getMainCamera();
        Vector3f left = camera.getLeftVector().mul(0.07f, new Vector3f());
        Vector3f up = camera.getUpVector().mul(0.07f, new Vector3f());
        VertexConsumer vc = buffers.getBuffer(CaveRenderTypes.POINTS);
        var matrix = pose.last().pose();
        IntArrayList p = f.points;
        for (int i = 0; i + 2 < p.size(); i += 3) {
            float x = p.getInt(i) - origin.getX(), y = p.getInt(i + 1) - origin.getY(), z = p.getInt(i + 2) - origin.getZ();
            int color = depthColor(p.getInt(i + 1));
            vc.addVertex(matrix, x + left.x + up.x, y + left.y + up.y, z + left.z + up.z).setColor(color);
            vc.addVertex(matrix, x - left.x + up.x, y - left.y + up.y, z - left.z + up.z).setColor(color);
            vc.addVertex(matrix, x - left.x - up.x, y - left.y - up.y, z - left.z - up.z).setColor(color);
            vc.addVertex(matrix, x + left.x - up.x, y + left.y - up.y, z + left.z - up.z).setColor(color);
        }
        BlockPos s = f.source;
        int r = f.range;
        LevelRenderer.renderLineBox(pose, buffers.getBuffer(RenderType.lines()), s.getX() - r - origin.getX(), s.getY() - r - origin.getY(),
                s.getZ() - r - origin.getZ(), s.getX() + 1 + r - origin.getX(), s.getY() + 1 + r - origin.getY(), s.getZ() + 1 + r - origin.getZ(), 1, 1, 1, 1);
    }

    @Override
    public AABB getRenderBoundingBox(CaveScannerBlockEntity scanner) {
        return new AABB(scanner.getBlockPos()).minmax(new AABB(scanner.source()).inflate(scanner.range() + 1));
    }

    @Override
    public boolean shouldRenderOffScreen(CaveScannerBlockEntity scanner) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    /** The item form. */
    public static class Item extends BlockEntityWithoutLevelRenderer {
        public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
            super(dispatcher, models);
        }

        @Override
        public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            draw(pose, buffers, light, overlay);
        }
    }
}
