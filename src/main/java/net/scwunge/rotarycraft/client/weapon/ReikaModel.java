package net.scwunge.rotarycraft.client.weapon;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;
import net.scwunge.rotarycraft.RotaryCraft;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * One of the original's machine models (Techne boxes with ModelBase semantics), converted by tools/modelbase2json.py and drawn
 * with vanilla ModelParts, part by part, so renderers can turn groups of parts as the original's renderAll did.
 */
public final class ReikaModel {
    private static final org.slf4j.Logger LOGGER = com.mojang.logging.LogUtils.getLogger();
    private final ResourceLocation location;
    private Map<String, ModelPart> parts;
    /** The original's render program (see tools/modelanim.py), or null if the model has none. */
    private JsonArray anim;

    public ReikaModel(String name) {
        this.location = RotaryCraft.id("reika_models/" + name + ".json");
    }

    private Map<String, ModelPart> parts() {
        if (parts == null) {
            parts = load();
        }
        return parts;
    }

    private Map<String, ModelPart> load() {
        Map<String, ModelPart> out = new HashMap<>();
        try (Reader in = new InputStreamReader(Minecraft.getInstance().getResourceManager().open(location), StandardCharsets.UTF_8)) {
            JsonObject json = JsonParser.parseReader(in).getAsJsonObject();
            JsonArray size = json.getAsJsonArray("texture_size");
            anim = json.has("anim") ? json.getAsJsonArray("anim") : null;
            for (Map.Entry<String, JsonElement> e : json.getAsJsonObject("parts").entrySet()) {
                JsonObject p = e.getValue().getAsJsonObject();
                JsonArray uv = p.getAsJsonArray("uv");
                CubeListBuilder cubes = CubeListBuilder.create().texOffs(uv.get(0).getAsInt(), uv.get(1).getAsInt()).mirror(p.get("mirror").getAsBoolean());
                for (JsonElement b : p.getAsJsonArray("boxes")) {
                    JsonArray box = b.getAsJsonArray();
                    cubes.addBox(box.get(0).getAsFloat(), box.get(1).getAsFloat(), box.get(2).getAsFloat(),
                            box.get(3).getAsFloat(), box.get(4).getAsFloat(), box.get(5).getAsFloat());
                }
                JsonArray pivot = p.getAsJsonArray("pivot");
                JsonArray rot = p.getAsJsonArray("rotation");
                MeshDefinition mesh = new MeshDefinition();
                mesh.getRoot().addOrReplaceChild("box", cubes, PartPose.offsetAndRotation(pivot.get(0).getAsFloat(), pivot.get(1).getAsFloat(),
                        pivot.get(2).getAsFloat(), rot.get(0).getAsFloat(), rot.get(1).getAsFloat(), rot.get(2).getAsFloat()));
                out.put(e.getKey(), LayerDefinition.create(mesh, size.get(0).getAsInt(), size.get(1).getAsInt()).bakeRoot());
            }
        } catch (Exception e) {
            LOGGER.error("Couldn't read model {}", location, e);
        }
        return out;
    }

    /**
     * Moves from a block at the origin into the original's model space (its renderers' translate(0.5, 1.5, 0.5) and
     * scale(1, -1, -1)).
     */
    public static void enterModelSpace(PoseStack pose) {
        pose.translate(0.5, 1.5, 0.5);
        pose.scale(1, -1, -1);
    }

    /** Whether the model has the original's own draw program, with its moving parts. */
    public boolean hasProgram() {
        parts();
        return anim != null;
    }

    /**
     * Draws the model as the original's renderAll did: its parts, with the translations and rotations it did between them, turning by
     * {@code phi} and {@code theta} degrees where it used those. {@code flags} are the booleans the original's renderers passed in
     * (a missing one is false).
     */
    public void renderAnimated(PoseStack pose, VertexConsumer buffer, int light, int overlay, double phi, double theta, boolean... flags) {
        Map<String, ModelPart> all = parts();
        if (anim == null) {
            renderAll(pose, buffer, light, overlay);
            return;
        }
        pose.pushPose();
        int skipping = 0;
        java.util.ArrayDeque<Boolean> inside = new java.util.ArrayDeque<>();
        int pushes = 0;
        for (JsonElement e : anim) {
            JsonArray op = e.getAsJsonArray();
            String code = op.get(0).getAsString();
            if (code.equals("if")) {
                int flag = op.get(1).getAsInt();
                boolean value = flag >= 0 && flag < flags.length && flags[flag];
                boolean pass = value == op.get(2).getAsBoolean();
                inside.push(pass);
                if (!pass) {
                    skipping++;
                }
                continue;
            }
            if (code.equals("end")) {
                if (!inside.isEmpty() && !inside.pop()) {
                    skipping--;
                }
                continue;
            }
            if (skipping > 0) {
                continue;
            }
            switch (code) {
                case "t" -> pose.translate(op.get(1).getAsDouble(), op.get(2).getAsDouble(), op.get(3).getAsDouble());
                case "s" -> pose.scale(op.get(1).getAsFloat(), op.get(2).getAsFloat(), op.get(3).getAsFloat());
                case "r" -> {
                    double angle = op.get(1).getAsDouble() + op.get(2).getAsDouble() * phi + op.get(3).getAsDouble() * theta;
                    org.joml.Vector3f axis = new org.joml.Vector3f(op.get(4).getAsFloat(), op.get(5).getAsFloat(), op.get(6).getAsFloat());
                    if (axis.lengthSquared() > 0) {
                        pose.mulPose(new org.joml.Quaternionf().fromAxisAngleDeg(axis.normalize(), (float) angle));
                    }
                }
                case "push" -> {
                    pose.pushPose();
                    pushes++;
                }
                case "pop" -> {
                    if (pushes > 0) {
                        pose.popPose();
                        pushes--;
                    }
                }
                case "p" -> {
                    ModelPart part = all.get(op.get(1).getAsString());
                    if (part != null) {
                        part.render(pose, buffer, light, overlay);
                    }
                }
                default -> {
                }
            }
        }
        while (pushes-- > 0) {
            pose.popPose();
        }
        pose.popPose();
    }

    /** The names of the model's parts. */
    public java.util.Set<String> names() {
        return parts().keySet();
    }

    /** Draws every part of the model. */
    public void renderAll(PoseStack pose, VertexConsumer buffer, int light, int overlay) {
        for (ModelPart part : parts().values()) {
            part.render(pose, buffer, light, overlay);
        }
    }

    /** Draws the named parts. */
    public void render(PoseStack pose, VertexConsumer buffer, int light, int overlay, String... names) {
        Map<String, ModelPart> all = parts();
        for (String name : names) {
            ModelPart part = all.get(name);
            if (part != null) {
                part.render(pose, buffer, light, overlay);
            }
        }
    }
}
