package net.scwunge.rotarycraft.client.machine;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.block.BevelGearBlock;
import net.scwunge.rotarycraft.block.SplitterBlock;
import net.scwunge.rotarycraft.blockentity.BevelGearBlockEntity;
import net.scwunge.rotarycraft.blockentity.SplitterBlockEntity;
import net.scwunge.rotarycraft.client.weapon.ReikaModel;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * The Bevel Gear and the Splitter, which turn through any of 24 or 16 arrangements of shafts. The original's renderers each had a table from
 * the arrangement to the rotations of the model; these are those tables (the bevel's in reika_models/bevel_orientations.json, written by
 * tools/gen_models.py from the original's source).
 */
public final class JunctionRenderers {
    private JunctionRenderers() {}

    private static final Map<String, Orientation> BEVEL = new HashMap<>();

    /** The pose of the model for a bevel gear with this input and output: translation, then turns about Y and X, and which way the gears turn. */
    private record Orientation(float y, float x, float tx, float ty, float tz, int dir) {
    }

    private static void loadBevel() {
        if (!BEVEL.isEmpty()) {
            return;
        }
        try (var in = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(RotaryCraft.id("reika_models/bevel_orientations.json")))) {
            JsonArray list = JsonParser.parseReader(in).getAsJsonArray();
            for (JsonElement e : list) {
                JsonObject o = e.getAsJsonObject();
                JsonArray t = o.getAsJsonArray("t");
                BEVEL.put(o.get("read").getAsString() + ">" + o.get("write").getAsString(), new Orientation(o.get("y").getAsFloat(), o.get("x").getAsFloat(),
                        t.get(0).getAsFloat(), t.get(1).getAsFloat(), t.get(2).getAsFloat(), o.get("dir").getAsInt()));
            }
        } catch (Exception ex) {
            com.mojang.logging.LogUtils.getLogger().error("Couldn't read the bevel gear's orientations", ex);
        }
    }

    private static ResourceLocation texture(String name) {
        return RotaryCraft.id("textures/machine/" + name + ".png");
    }

    public static class Bevel implements BlockEntityRenderer<BevelGearBlockEntity> {
        private static final ReikaModel MODEL = new ReikaModel("bevel_gear");

        static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, Direction in, Direction out, double phi) {
            loadBevel();
            pose.pushPose();
            ReikaModel.enterModelSpace(pose);
            int dir = 1;
            Orientation o = BEVEL.get(in.getName() + ">" + out.getName());
            if (o != null) {
                pose.translate(o.tx(), o.ty(), o.tz());
                pose.mulPose(Axis.YP.rotationDegrees(o.y()));
                pose.mulPose(Axis.XP.rotationDegrees(o.x()));
                dir = o.dir();
            } else {
                pose.mulPose(Axis.YP.rotationDegrees(90));
            }
            MODEL.renderAnimated(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture("bevel_gear"))), light, overlay, phi * dir, 0);
            pose.popPose();
        }

        @Override
        public void render(BevelGearBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            double speed = be.getOmega() <= 0 ? 0 : Math.pow(Math.log(be.getOmega() + 1) / Math.log(2), 1.05);
            if (be.getLevel() != null) {
                long now = be.getLevel().getGameTime();
                if (be.phiTime != now) {
                    if (be.phiTime != 0 && now - be.phiTime < 40) {
                        be.phi = (float) ((be.phi + (now - be.phiTime) * speed) % 360);
                    }
                    be.phiTime = now;
                }
            }
            draw(pose, buffers, light, overlay, be.getBlockState().getValue(BevelGearBlock.INPUT), be.facing(), be.phi + speed * partialTick);
        }

        @Override
        public AABB getRenderBoundingBox(BevelGearBlockEntity be) {
            return new AABB(be.getBlockPos()).inflate(1);
        }

        public static class Item extends BlockEntityWithoutLevelRenderer {
            public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
                super(dispatcher, models);
            }

            @Override
            public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                draw(pose, buffers, light, overlay, Direction.WEST, Direction.NORTH, 0);
            }
        }
    }

    public static class Splitter implements BlockEntityRenderer<SplitterBlockEntity> {
        private static final ReikaModel MERGE = new ReikaModel("splitter");
        private static final ReikaModel SPLIT = new ReikaModel("splitter2");
        private static final float[] YAW = {-90, 0, 90, 180, -90, 0, 90, 180, 270, 0, 90, 180, -90, 0, 90, 180};

        /** The original's number for this arrangement of shafts (0 to 7 merge, 8 to 15 split), from the side it leaves by and the side it bends to. */
        static int meta(Direction front, Direction bent, boolean splitting) {
            Direction[] ring = {Direction.WEST, Direction.NORTH, Direction.EAST, Direction.SOUTH};
            int f = java.util.Arrays.asList(ring).indexOf(front);
            int b = java.util.Arrays.asList(ring).indexOf(bent);
            if (f < 0 || b < 0) {
                return 0;
            }
            boolean clockwise = b == (f + 1) % 4;
            if (!splitting) {
                // merge: front W bent N is 0, N E 1, E S 2, S W 3; the other way round is 4 to 7 (W S, N W, E N, S E)
                return clockwise ? f : 4 + f;
            }
            // split: reads from the side opposite the front; bent to the side after it is 8 to 11, the side before it 12 to 15 (by the original's table)
            int read = (f + 2) % 4;
            // 8: read W, write E, bent N; 9: read N, write S, bent E; 10: read E, write W, bent S; 11: read S, write N, bent W
            boolean bentIsNextOfRead = b == (read + 1) % 4;
            return bentIsNextOfRead ? 8 + read : 12 + read;
        }

        static void draw(PoseStack pose, MultiBufferSource buffers, int light, int overlay, boolean bedrock, int meta, double phi, boolean failed) {
            pose.pushPose();
            ReikaModel.enterModelSpace(pose);
            pose.mulPose(Axis.YP.rotationDegrees(YAW[meta] - 90));
            ReikaModel model = meta < 4 || meta >= 8 && meta < 12 ? MERGE : SPLIT;
            model.renderAnimated(pose, buffers.getBuffer(RenderType.entityCutoutNoCull(texture(bedrock ? "splitter_bedrock" : "splitter"))), light, overlay, -phi, 0, failed);
            pose.popPose();
        }

        @Override
        public void render(SplitterBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
            double speed = be.getOmega() <= 0 ? 0 : Math.pow(Math.log(be.getOmega() + 1) / Math.log(2), 1.05);
            if (be.getLevel() != null) {
                long now = be.getLevel().getGameTime();
                if (be.phiTime != now) {
                    if (be.phiTime != 0 && now - be.phiTime < 40) {
                        be.phi = (float) ((be.phi + (now - be.phiTime) * speed) % 360);
                    }
                    be.phiTime = now;
                }
            }
            draw(pose, buffers, light, overlay, false, meta(be.facing(), be.getBlockState().getValue(SplitterBlock.BENT), be.isSplitting()), be.phi + speed * partialTick, false);
        }

        @Override
        public AABB getRenderBoundingBox(SplitterBlockEntity be) {
            return new AABB(be.getBlockPos()).inflate(1);
        }

        public static class Item extends BlockEntityWithoutLevelRenderer {
            public Item(BlockEntityRenderDispatcher dispatcher, EntityModelSet models) {
                super(dispatcher, models);
            }

            @Override
            public void renderByItem(ItemStack stack, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
                draw(pose, buffers, light, overlay, false, 0, 0, false);
            }
        }
    }
}
