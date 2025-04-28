package com.deltasf.createpropulsion.utility;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.optical_sensors.rendering.BeamRenderData;
import com.deltasf.createpropulsion.optical_sensors.rendering.OpticalSensorBeamRenderType;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class TranslucentBeamRenderer {
    private static final Queue<BeamRenderData> RENDER_QUEUE = new ConcurrentLinkedQueue<>();

    public static void scheduleBeamRender(BeamRenderData data) {
        RENDER_QUEUE.offer(data);
    }

    // The idea behind this is to perform two render passes:
    // First one happens BEFORE translucent world geometry and therefore renders beam behind it. It is located inside OpticalSensorRender
    // Second one happens AFTER ALL translucent world geometry and therefore renders beam above it
    // Both passes respect depth buffer and do not write to it in order to preserve all other translucent geometry
    // This however results in doubling of alpha in regions where both passes overlap, while in regions of no overlap alpha remains undoubled
    // The only way to fix that seems to be using stencil buffer which is set in the first pass and read in the second, and used in the second to double alpha

    @SubscribeEvent
    public static void onRenderLevelStageEnd(RenderLevelStageEvent event) {
        //This is second pass (after translucent)
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            renderAllBeams(event.getPoseStack());
            RENDER_QUEUE.clear();
        }
        
    }

    private static void renderAllBeams(PoseStack poseStack){
        if (RENDER_QUEUE.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null) {
            return;
        }
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        VertexConsumer buffer = bufferSource.getBuffer(OpticalSensorBeamRenderType.SOLID_TRANSLUCENT_BEAM);

        //Render all invoked
        for (BeamRenderData data : RENDER_QUEUE) {
            poseStack.pushPose();
            drawBeam(buffer, data);
            poseStack.popPose();
        }
    }

    public static void drawBeam(VertexConsumer buffer, BeamRenderData data){
        Matrix4f pose = data.poseSnapshot.pose();

        //Rendering
        drawQuad(buffer, pose, data.sBottomLeft, data.sBottomRight, data.eBottomRight, data.eBottomLeft, data.startColor, data.endColor, data.normalBottom);
        drawQuad(buffer, pose, data.sBottomRight, data.sTopRight, data.eTopRight, data.eBottomRight, data.startColor, data.endColor, data.normalRight);
        drawQuad(buffer, pose, data.sTopRight, data.sTopLeft, data.eTopLeft, data.eTopRight, data.startColor, data.endColor, data.normalTop);
        drawQuad(buffer, pose, data.sTopLeft, data.sBottomLeft, data.eBottomLeft, data.eTopLeft, data.startColor, data.endColor, data.normalLeft);
    }

    private static void drawQuad(VertexConsumer buffer, Matrix4f pose,
                          Vector3f v1, Vector3f v2, Vector3f v3, Vector3f v4,
                          Vector4f startColor, Vector4f endColor, Vector3f normal) {

        // Vertex 1 (Start)
        buffer.vertex(pose, v1.x(), v1.y(), v1.z())
              .color(startColor.x(), startColor.y(), startColor.z(), startColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();

        // Vertex 2 (Start)
        buffer.vertex(pose, v2.x(), v2.y(), v2.z())
              .color(startColor.x(), startColor.y(), startColor.z(), startColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();

        // Vertex 3 (End)
        buffer.vertex(pose, v3.x(), v3.y(), v3.z())
              .color(endColor.x(), endColor.y(), endColor.z(), endColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();

        // Vertex 4 (End)
        buffer.vertex(pose, v4.x(), v4.y(), v4.z())
              .color(endColor.x(), endColor.y(), endColor.z(), endColor.w())
              .normal(normal.x(), normal.y(), normal.z())
              .endVertex();
    }
}
