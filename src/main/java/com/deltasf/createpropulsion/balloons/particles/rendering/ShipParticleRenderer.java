package com.deltasf.createpropulsion.balloons.particles.rendering;

import org.joml.Matrix4dc;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.HapData;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

//TODO: Remove this slop
public class ShipParticleRenderer {

    public static void render(PoseStack ms, Matrix4f projection, Camera camera) {
        Minecraft mc = Minecraft.getInstance();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.cutout());

        Long2ObjectMap<ShipParticleHandler> handlers = BalloonParticleSystem.getAllHandlers();
        if (handlers.isEmpty()) return;

        Vector3d camPos = new Vector3d(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);
        
        Vector3f camLeft = camera.getLeftVector();
        Vector3f camUp = camera.getUpVector();
        Vector3f camNormal = camera.getLookVector(); 
        camNormal.mul(-1f);

        for (Long2ObjectMap.Entry<ShipParticleHandler> entry : handlers.long2ObjectEntrySet()) {
            long shipId = entry.getLongKey();
            ShipParticleHandler handler = entry.getValue();
            if (handler.isEmpty()) continue;

            ClientShip ship = (ClientShip) VSGameUtilsKt.getShipObjectWorld(mc.level).getLoadedShips().getById(shipId);
            if (ship == null) continue;

            Matrix4dc shipToWorld = ship.getRenderTransform().getShipToWorld();
            Vector4d anchorWorld = new Vector4d(handler.getAnchorX(), handler.getAnchorY(), handler.getAnchorZ(), 1.0);
            shipToWorld.transform(anchorWorld);

            ms.pushPose();

            float tx = (float)(anchorWorld.x - camPos.x);
            float ty = (float)(anchorWorld.y - camPos.y);
            float tz = (float)(anchorWorld.z - camPos.z);
            ms.translate(tx, ty, tz);

            float m00 = (float)shipToWorld.m00(); float m01 = (float)shipToWorld.m01(); float m02 = (float)shipToWorld.m02();
            float m10 = (float)shipToWorld.m10(); float m11 = (float)shipToWorld.m11(); float m12 = (float)shipToWorld.m12();
            float m20 = (float)shipToWorld.m20(); float m21 = (float)shipToWorld.m21(); float m22 = (float)shipToWorld.m22();

            Matrix4f poseMatrix = ms.last().pose();
            
            renderParticles(handler.data, buffer, poseMatrix, 
                m00, m01, m02, m10, m11, m12, m20, m21, m22,
                camLeft, camUp, camNormal
            );

            ms.popPose();
        }
        
        bufferSource.endBatch(RenderType.cutout());
    }

    private static void renderParticles(HapData data, VertexConsumer buffer, Matrix4f poseMatrix,
                                        float m00, float m01, float m02,
                                        float m10, float m11, float m12,
                                        float m20, float m21, float m22,
                                        Vector3f camLeft, Vector3f camUp, Vector3f normal) {
        
        float size = 0.5f * (1.0f / 16.0f);
        int r = 225;
        int g = 225;
        int b = 225;
        int a = 255;
        
        int light = LightTexture.FULL_BRIGHT;
        int overlay = OverlayTexture.NO_OVERLAY;
        
        float c1x = (camLeft.x() * size) + (camUp.x() * size);
        float c1y = (camLeft.y() * size) + (camUp.y() * size);
        float c1z = (camLeft.z() * size) + (camUp.z() * size);

        float c2x = (camLeft.x() * size) - (camUp.x() * size);
        float c2y = (camLeft.y() * size) - (camUp.y() * size);
        float c2z = (camLeft.z() * size) - (camUp.z() * size);

        float c3x = -(camLeft.x() * size) - (camUp.x() * size);
        float c3y = -(camLeft.y() * size) - (camUp.y() * size);
        float c3z = -(camLeft.z() * size) - (camUp.z() * size);

        float c4x = -(camLeft.x() * size) + (camUp.x() * size);
        float c4y = -(camLeft.y() * size) + (camUp.y() * size);
        float c4z = -(camLeft.z() * size) + (camUp.z() * size);

        float u = 0.001f;
        float v = 0.001f;

        for (int i = 0; i < data.count; i++) {
            float rx = data.x[i];
            float ry = data.y[i];
            float rz = data.z[i];

            float wx = rx * m00 + ry * m10 + rz * m20;
            float wy = rx * m01 + ry * m11 + rz * m21;
            float wz = rx * m02 + ry * m12 + rz * m22;
            
            // Top Left
            buffer.vertex(poseMatrix, wx + c1x, wy + c1y, wz + c1z)
                  .color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light)
                  .normal(normal.x(), normal.y(), normal.z()).endVertex();

            // Bottom Left
            buffer.vertex(poseMatrix, wx + c2x, wy + c2y, wz + c2z)
                  .color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light)
                  .normal(normal.x(), normal.y(), normal.z()).endVertex();

            // Bottom Right
            buffer.vertex(poseMatrix, wx + c3x, wy + c3y, wz + c3z)
                  .color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light)
                  .normal(normal.x(), normal.y(), normal.z()).endVertex();

            // Top Right
            buffer.vertex(poseMatrix, wx + c4x, wy + c4y, wz + c4z)
                  .color(r, g, b, a).uv(u, v).overlayCoords(overlay).uv2(light)
                  .normal(normal.x(), normal.y(), normal.z()).endVertex();
        }
    }
}