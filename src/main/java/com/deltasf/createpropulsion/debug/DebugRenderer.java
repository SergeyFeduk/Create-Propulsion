package com.deltasf.createpropulsion.debug;

import org.joml.Matrix4dc;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
public class DebugRenderer {
    private static class TimedBoxData {
        final Vec3 position;
        final Vec3 size;
        final Quaternionf rotation;
        final Color color;
        final boolean onlyInDebugMode;
        int remainingTicks;

        TimedBoxData(Level level, Vec3 position, Vec3 size, Quaternionf rotation, Color color, boolean onlyInDebugMode, int initialTicks) {
            //Test if on ship and if so - modify position to be in world space and multiply rotation to account for ships rotation
            boolean inShipyard = VSGameUtilsKt.isBlockInShipyard(level, position);
            if (inShipyard) {
                //Ship case
                Ship ship = VSGameUtilsKt.getShipManagingPos(level, position.x, position.y, position.z);
                Matrix4dc shipToWorldMatrix = ship.getShipToWorld();
                Quaterniondc shipRotation = ship.getTransform().getShipToWorldRotation();
                //Transform Position
                Vector3d posInShip = VectorConversionsMCKt.toJOML(position);
                Vector3d posInWorld = shipToWorldMatrix.transformPosition(posInShip, new Vector3d());
                //Transform Rotation
                Quaterniond rotInShip = new Quaterniond(rotation.x, rotation.y, rotation.z, rotation.w);
                Quaterniond rotInWorld = shipRotation.mul(rotInShip, new Quaterniond());
                //Set values
                this.position = VectorConversionsMCKt.toMinecraft(posInWorld);
                this.size = size;
                this.rotation = new Quaternionf((float)rotInWorld.x, (float)rotInWorld.y, (float)rotInWorld.z, (float)rotInWorld.w);
            } else {
                //World case
                this.position = position;
                this.size = size;
                this.rotation = rotation;
            }
            //Invariant
            this.color = color;
            this.onlyInDebugMode = onlyInDebugMode;
            this.remainingTicks = initialTicks;
        }
    }

    private static final Map<String, TimedBoxData> timedBoxes = new ConcurrentHashMap<>();

    //Full
    public static void drawBox(String identifier, Vec3 center, Vec3 size, Quaternionf rotation, Color color, boolean onlyInDebugMode, int ticksToRender) {
        if (identifier == null || identifier.isEmpty()) {
            System.err.println("[DebugRenderer] Error: Null or empty identifier provided for drawBox.");
            return;
        }
        if (ticksToRender <= 0) {
            timedBoxes.remove(identifier);
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return; //In case we do not have a level yet for some reason
        
        TimedBoxData data = new TimedBoxData(level, center, size, rotation, color, onlyInDebugMode, ticksToRender);
        timedBoxes.put(identifier, data);
    }

    //Basic
    public static void drawBox(String identifier, Vec3 center, Vec3 size, int ticksToRender) {
        drawBox(identifier, center, size, new Quaternionf(), Color.WHITE, false, ticksToRender);
    }

    public static void drawBox(String identifier, Vec3 center, Vec3 size, Color color, int ticksToRender) {
        drawBox(identifier, center, size, new Quaternionf(), color, false, ticksToRender);
    }
    //AABB
    public static void drawBox(String identifier, AABB aabb, int ticksToRender) {
        drawBox(identifier, aabb, Color.WHITE, ticksToRender);
    }

    public static void drawBox(String identifier, AABB aabb, Color color, int ticksToRender) {
        if (aabb == null) {
            System.err.println("[DebugRenderer] Error: Null AABB provided for drawBox with identifier: " + identifier);
            removeBox(identifier);
            return;
        }
        Vec3 center = aabb.getCenter();
        Vec3 size = new Vec3(aabb.getXsize(), aabb.getYsize(), aabb.getZsize());
        drawBox(identifier, center, size, new Quaternionf(), color, false, ticksToRender);
    }

    //BlockPos
    public static void drawBox(String identifier, BlockPos blockPos, int ticksToRender) {
        drawBox(identifier, blockPos, Color.WHITE, ticksToRender);
    }

    public static void drawBox(String identifier, BlockPos blockPos, Color color, int ticksToRender) {
         if (blockPos == null) {
            System.err.println("[DebugRenderer] Error: Null BlockPos provided for drawBox with identifier: " + identifier);
            removeBox(identifier);
            return;
        }
        Vec3 center = blockPos.getCenter();
        Vec3 size = new Vec3(1.0, 1.0, 1.0);
        drawBox(identifier, center, size, new Quaternionf(), color, false, ticksToRender);
    }

    public static void removeBox(String identifier) {
        if (identifier != null) {
            timedBoxes.remove(identifier);
        }
    }

    //Decay boxes
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            if (timedBoxes.isEmpty()) {
                return;
            }

            Minecraft mc = Minecraft.getInstance();
            if (mc != null && !mc.isPaused()) {
                timedBoxes.entrySet().removeIf(entry -> {
                    TimedBoxData data = entry.getValue();
                    data.remainingTicks--;
                    return data.remainingTicks <= 0;
                });
            }
        }
    }

    //Render boxes
    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS || timedBoxes.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null || mc.gameRenderer == null || mc.renderBuffers() == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer vertexConsumer = bufferSource.getBuffer(DebugLineRenderType.DEBUG_LINE);
        Vec3 cameraPos = mc.gameRenderer.getMainCamera().getPosition();

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        boolean isDebugMode = mc.options.renderDebug;

        for (TimedBoxData boxData : timedBoxes.values()) {
            if (boxData.onlyInDebugMode && !isDebugMode) {
                continue;
            }

            renderWireBox(poseStack, vertexConsumer, boxData);
        }

        poseStack.popPose();
    }

    private static void renderWireBox(PoseStack poseStack, VertexConsumer vertexConsumer, TimedBoxData data) {
        poseStack.pushPose();

        poseStack.translate(data.position.x, data.position.y, data.position.z);
        poseStack.mulPose(data.rotation);

        Matrix4f matrix = poseStack.last().pose();

        float halfW = (float) data.size.x / 2.0f;
        float halfH = (float) data.size.y / 2.0f;
        float halfD = (float) data.size.z / 2.0f;

        Vector3f p0 = new Vector3f(-halfW, -halfH, -halfD);
        Vector3f p1 = new Vector3f( halfW, -halfH, -halfD);
        Vector3f p2 = new Vector3f( halfW, -halfH,  halfD);
        Vector3f p3 = new Vector3f(-halfW, -halfH,  halfD);
        Vector3f p4 = new Vector3f(-halfW,  halfH, -halfD);
        Vector3f p5 = new Vector3f( halfW,  halfH, -halfD);
        Vector3f p6 = new Vector3f( halfW,  halfH,  halfD);
        Vector3f p7 = new Vector3f(-halfW,  halfH,  halfD);

        float r = data.color.getRed() / 255.0f;
        float g = data.color.getGreen() / 255.0f;
        float b = data.color.getBlue() / 255.0f;
        float a = data.color.getAlpha() / 255.0f;

        drawLine(vertexConsumer, matrix, p0, p1, r, g, b, a);
        drawLine(vertexConsumer, matrix, p1, p2, r, g, b, a);
        drawLine(vertexConsumer, matrix, p2, p3, r, g, b, a);
        drawLine(vertexConsumer, matrix, p3, p0, r, g, b, a);
        drawLine(vertexConsumer, matrix, p4, p5, r, g, b, a);
        drawLine(vertexConsumer, matrix, p5, p6, r, g, b, a);
        drawLine(vertexConsumer, matrix, p6, p7, r, g, b, a);
        drawLine(vertexConsumer, matrix, p7, p4, r, g, b, a);
        drawLine(vertexConsumer, matrix, p0, p4, r, g, b, a);
        drawLine(vertexConsumer, matrix, p1, p5, r, g, b, a);
        drawLine(vertexConsumer, matrix, p2, p6, r, g, b, a);
        drawLine(vertexConsumer, matrix, p3, p7, r, g, b, a);

        poseStack.popPose();
    }

    private static void drawLine(VertexConsumer consumer, Matrix4f matrix, Vector3f pos1, Vector3f pos2, float r, float g, float b, float a) {
        consumer.vertex(matrix, pos1.x(), pos1.y(), pos1.z()).color(r, g, b, a).endVertex();
        consumer.vertex(matrix, pos2.x(), pos2.y(), pos2.z()).color(r, g, b, a).endVertex();
    }
}
