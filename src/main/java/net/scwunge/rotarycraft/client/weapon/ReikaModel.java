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
