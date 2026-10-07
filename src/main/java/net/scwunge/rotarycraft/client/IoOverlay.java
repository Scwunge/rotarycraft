package net.scwunge.rotarycraft.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.scwunge.rotarycraft.RotaryCraft;
import net.scwunge.rotarycraft.blockentity.EngineBlockEntity;
import net.scwunge.rotarycraft.blockentity.PowerBlockEntity;
import net.scwunge.rotarycraft.charged.IoGogglesItem;
import net.scwunge.rotarycraft.farm.FarmBlockEntity;
import net.scwunge.rotarycraft.process.EnergyConverterBlockEntity;

/**
 * What the IO goggles show: a coloured block against each shaft machine's input face (green) and output face (red), for the machines within 24 blocks. Engines always show their output;
 * machines that take power on any side show only what they have.
 */
@EventBusSubscriber(modid = RotaryCraft.MOD_ID, value = Dist.CLIENT)
public final class IoOverlay {
    public static final int RANGE = 24;

    private IoOverlay() {}

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || !(mc.player.getItemBySlot(EquipmentSlot.HEAD).getItem() instanceof IoGogglesItem)) {
            return;
        }
        Level level = mc.level;
        Vec3 camera = event.getCamera().getPosition();
        PoseStack pose = event.getPoseStack();
        MultiBufferSource.BufferSource buffers = mc.renderBuffers().bufferSource();
        VertexConsumer vc = buffers.getBuffer(RenderType.debugFilledBox());
        ChunkPos center = mc.player.chunkPosition();
        int reach = RANGE / 16 + 1;
        for (int cx = -reach; cx <= reach; cx++) {
            for (int cz = -reach; cz <= reach; cz++) {
                if (!level.hasChunk(center.x + cx, center.z + cz)) {
                    continue;
                }
                for (BlockEntity be : level.getChunk(center.x + cx, center.z + cz).getBlockEntities().values()) {
                    if (be instanceof PowerBlockEntity power && be.getBlockPos().distSqr(mc.player.blockPosition()) <= RANGE * RANGE) {
                        sides(pose, vc, camera, power);
                    }
                }
            }
        }
        buffers.endBatch(RenderType.debugFilledBox());
    }

    private static void sides(PoseStack pose, VertexConsumer vc, Vec3 camera, PowerBlockEntity power) {
        if (power instanceof FarmBlockEntity) {
            return;
        }
        BlockPos pos = power.getBlockPos();
        Direction out = power.facing();
        if (power instanceof EngineBlockEntity || power.getTorqueOut(out) > 0 || power.getOmegaOut(out) > 0) {
            box(pose, vc, camera, pos.relative(out), 1F, 0F, 0F);
        }
        if (!(power instanceof EngineBlockEntity) && !(power instanceof EnergyConverterBlockEntity)) {
            box(pose, vc, camera, pos.relative(power.inputSide()), 0F, 1F, 0F);
        }
    }

    private static void box(PoseStack pose, VertexConsumer vc, Vec3 camera, BlockPos at, float r, float g, float b) {
        double e = 0.002;
        LevelRenderer.addChainedFilledBoxVertices(pose, vc, at.getX() - camera.x - e, at.getY() - camera.y - e, at.getZ() - camera.z - e,
                at.getX() + 1 - camera.x + e, at.getY() + 1 - camera.y + e, at.getZ() + 1 - camera.z + e, r, g, b, 0.35F);
    }
}
