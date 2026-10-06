package net.scwunge.rotarycraft.client.survey;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Marker;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.survey.CctvBlockEntity;

/**
 * Looking through a CCTV: the game's view is moved onto a marker standing at the camera and turned as it is aimed (the player does not
 * move). It ends when the viewer sneaks, is hurt, opens a screen, the camera loses its coil or is gone.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, value = Dist.CLIENT)
public final class CameraView {
    private static BlockPos camera;
    private static Marker marker;
    private static int age;

    private CameraView() {}

    public static void start(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        stop();
        camera = pos;
        marker = new Marker(EntityType.MARKER, mc.level);
        age = 0;
        place(mc);
        mc.setCameraEntity(marker);
    }

    public static boolean active() {
        return camera != null;
    }

    public static void stop() {
        Minecraft mc = Minecraft.getInstance();
        if (camera != null && mc.player != null) {
            mc.setCameraEntity(mc.player);
        }
        camera = null;
        marker = null;
    }

    private static boolean place(Minecraft mc) {
        if (mc.level == null || !(mc.level.getBlockEntity(camera) instanceof CctvBlockEntity cctv) || !cctv.isOn()) {
            return false;
        }
        float yaw = -cctv.phi();
        double rad = Math.toRadians(yaw);
        double x = camera.getX() + 0.5 - Math.sin(rad) * 0.45, y = camera.getY() + 0.45, z = camera.getZ() + 0.5 + Math.cos(rad) * 0.45;
        marker.setPos(x, y, z);
        marker.xo = x;
        marker.yo = y;
        marker.zo = z;
        marker.xOld = x;
        marker.yOld = y;
        marker.zOld = z;
        marker.setYRot(yaw);
        marker.setXRot(cctv.theta());
        marker.yRotO = yaw;
        marker.xRotO = cctv.theta();
        return true;
    }

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (camera == null) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        age++;
        boolean leave = mc.player == null || mc.level == null || mc.player.hurtTime > 0 || mc.screen != null || (age > 10 && mc.options.keyShift.isDown());
        if (leave || !place(mc)) {
            stop();
        }
    }
}
