package com.deltasf.createpropulsion.balloons.particles.rendering;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.balloons.particles.BalloonParticleSystem;
import com.deltasf.createpropulsion.balloons.particles.HapData;
import com.deltasf.createpropulsion.balloons.particles.ShipParticleHandler;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.joml.Matrix4dc;
import org.joml.Matrix4f;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL33;
import org.lwjgl.system.MemoryUtil;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class InstancedParticleRenderer {
    private static final int R = 225;
    private static final int G = 225;
    private static final int B = 225;

    private static boolean initialized = false;
    private static int programId;
    private static int VAOId;
    private static int quadVBOId;
    private static int instanceVBOId;
    private static int textureId;
    
    //Uniform Locations
    private static int uProjectionMat;
    private static int uModelViewMat;
    private static int uCamRight;
    private static int uCamUp;
    private static int uShipRotation;
    private static int uRelativeAnchor;

    private static final int MAX_PARTICLES = 16384;
    private static final int INSTANCE_STRIDE = 20; //12(pos) + 4(col) + 4(scale) = 20
    private static ByteBuffer instanceBuffer;
    
    private static final FloatBuffer MATRIX_BUFFER = MemoryUtil.memAllocFloat(16);

    public static void init() {
        if (initialized) return;
        programId = ParticleShaderLoader.createProgram("hap.vert", "hap.frag");
        
        //Uniforms!!!
        uProjectionMat = GL33.glGetUniformLocation(programId, "uProjMat");
        uModelViewMat = GL33.glGetUniformLocation(programId, "uModelViewMat");
        uCamRight = GL33.glGetUniformLocation(programId, "uCamRight");
        uCamUp = GL33.glGetUniformLocation(programId, "uCamUp");
        uShipRotation = GL33.glGetUniformLocation(programId, "uShipRotation");
        uRelativeAnchor = GL33.glGetUniformLocation(programId, "uRelativeAnchor");

        //Buffers
        VAOId = GL33.glGenVertexArrays();
        GL33.glBindVertexArray(VAOId);
        float[] quadData = {
            -0.5f, -0.5f, 0.0f, 0.0f,
             0.5f, -0.5f, 1.0f, 0.0f,
             0.5f,  0.5f, 1.0f, 1.0f,
            -0.5f,  0.5f, 0.0f, 1.0f
        };
        
        quadVBOId = GL33.glGenBuffers();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, quadVBOId);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, quadData, GL33.GL_STATIC_DRAW);

        //Quad position (vec2)
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 2, GL33.GL_FLOAT, false, 4 * 4, 0);
        
        //UV (vec2)
        GL33.glEnableVertexAttribArray(1);
        GL33.glVertexAttribPointer(1, 2, GL33.GL_FLOAT, false, 4 * 4, 2 * 4);

        //Instace VBO
        instanceVBOId = GL33.glGenBuffers();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, instanceVBOId);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, MAX_PARTICLES * INSTANCE_STRIDE, GL33.GL_STREAM_DRAW);

        //Position (vec3)
        GL33.glEnableVertexAttribArray(2);
        GL33.glVertexAttribPointer(2, 3, GL33.GL_FLOAT, false, INSTANCE_STRIDE, 0);
        GL33.glVertexAttribDivisor(2, 1);

        //Color (vec4 as bytes)
        GL33.glEnableVertexAttribArray(3);
        GL33.glVertexAttribPointer(3, 4, GL33.GL_UNSIGNED_BYTE, true, INSTANCE_STRIDE, 12);
        GL33.glVertexAttribDivisor(3, 1);

        //Scale (float)
        GL33.glEnableVertexAttribArray(4);
        GL33.glVertexAttribPointer(4, 1, GL33.GL_FLOAT, false, INSTANCE_STRIDE, 16);
        GL33.glVertexAttribDivisor(4, 1);

        generateWhitePixelTexture();

        GL33.glBindVertexArray(0);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        
        instanceBuffer = MemoryUtil.memAlloc(MAX_PARTICLES * INSTANCE_STRIDE);
        initialized = true;
    }

    public static void render(PoseStack ms, Matrix4f projectionMatrix, Camera camera, float partialTick) {
        if (!initialized) init();
        if (BalloonParticleSystem.getAllHandlers().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        
        //Prepare state
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        
        GL33.glUseProgram(programId);
        GL33.glBindVertexArray(VAOId);

        //Bind pixel texture
        GL33.glActiveTexture(GL33.GL_TEXTURE0);
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
        int uTexLoc = GL33.glGetUniformLocation(programId, "uTex");
        GL33.glUniform1i(uTexLoc, 0);

        //Projection Matrix
        projectionMatrix.get(MATRIX_BUFFER);
        GL33.glUniformMatrix4fv(uProjectionMat, false, MATRIX_BUFFER);

        //ModelView Matrix
        Matrix4f modelViewMatrix = ms.last().pose();
        modelViewMatrix.get(MATRIX_BUFFER);
        GL33.glUniformMatrix4fv(uModelViewMat, false, MATRIX_BUFFER);

        //Camera Vectors
        Vector3f camRight = camera.getLeftVector().mul(-1.0f);
        GL33.glUniform3f(uCamRight, camRight.x(), camRight.y(), camRight.z());

        Vector3f camUp = camera.getUpVector();
        GL33.glUniform3f(uCamUp, camUp.x(), camUp.y(), camUp.z());

        Vector3d camPos = new Vector3d(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);

        byte alpha = PropulsionConfig.BALLOON_PARTICLES_ALPHA.get().byteValue();

        //Rendering
        Long2ObjectMap<ShipParticleHandler> handlers = BalloonParticleSystem.getAllHandlers();
        for (Long2ObjectMap.Entry<ShipParticleHandler> entry : handlers.long2ObjectEntrySet()) {
            long shipId = entry.getLongKey();
            ShipParticleHandler handler = entry.getValue();
            if (handler.isEmpty()) continue;

            ClientShip ship = (ClientShip) VSGameUtilsKt.getShipObjectWorld(mc.level).getLoadedShips().getById(shipId);
            if (ship == null) continue;

            instanceBuffer.clear();
            HapData data = handler.data;
            int count = Math.min(data.count, MAX_PARTICLES);

            //CPU -> ByteBuffer
            for (int i = 0; i < count; i++) {
                float px = data.px[i] + (data.x[i] - data.px[i]) * partialTick;
                float py = data.py[i] + (data.y[i] - data.py[i]) * partialTick;
                float pz = data.pz[i] + (data.z[i] - data.pz[i]) * partialTick;
                
                //Local position
                instanceBuffer.putFloat(px);
                instanceBuffer.putFloat(py);
                instanceBuffer.putFloat(pz);

                //Color
                instanceBuffer.put((byte) R);
                instanceBuffer.put((byte) G);
                instanceBuffer.put((byte) B);
                instanceBuffer.put(   alpha);

                //Scale
                float scale = data.scale[i];
                if (data.life[i] < 0.2f) {
                    scale *= (data.life[i] / 0.2f);
                }
                instanceBuffer.putFloat(scale);
            }
            
            instanceBuffer.flip();

            //Upload
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, instanceVBOId);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, instanceBuffer);

            //Ship uniforms
            Matrix4dc shipToWorld = ship.getRenderTransform().getShipToWorld();
            Matrix4f shipToWorldF = new Matrix4f(shipToWorld);
            
            //Translation components set to zero cus we only need rotation and scale from this matrix
            shipToWorldF.m30(0); shipToWorldF.m31(0); shipToWorldF.m32(0);
            shipToWorldF.get(MATRIX_BUFFER);
            GL33.glUniformMatrix4fv(uShipRotation, false, MATRIX_BUFFER);

            //Calculate anchor position in world space
            Vector4d anchorWorld = new Vector4d(handler.getAnchorX(), handler.getAnchorY(), handler.getAnchorZ(), 1.0);
            shipToWorld.transform(anchorWorld);

            //Anchor relative to camera
            float rx = (float)(anchorWorld.x - camPos.x);
            float ry = (float)(anchorWorld.y - camPos.y);
            float rz = (float)(anchorWorld.z - camPos.z);
            
            GL33.glUniform3f(uRelativeAnchor, rx, ry, rz);

            //Perform a draw
            GL33.glDrawArraysInstanced(GL33.GL_TRIANGLE_FAN, 0, 4, count);
        }

        //Tidy up after outselves
        GL33.glBindVertexArray(0);
        GL33.glUseProgram(0);

        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0); 
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, 0);

        RenderSystem.depthMask(true); 
        RenderSystem.disableBlend();
    }
    
    public static void destroy() {
        if (!initialized) return;
        GL33.glDeleteVertexArrays(VAOId);
        GL33.glDeleteBuffers(quadVBOId);
        GL33.glDeleteBuffers(instanceVBOId);
        GL33.glDeleteTextures(textureId);
        GL33.glDeleteProgram(programId);
        MemoryUtil.memFree(instanceBuffer);
        MemoryUtil.memFree(MATRIX_BUFFER);
        initialized = false;
    }

    //TODO: Move in some utility class
    private static void generateWhitePixelTexture() {
        textureId = GL33.glGenTextures();
        GL33.glBindTexture(GL33.GL_TEXTURE_2D, textureId);
        ByteBuffer whitePixel = MemoryUtil.memAlloc(4);
        whitePixel.put((byte)255).put((byte)255).put((byte)255).put((byte)255);
        whitePixel.flip();
        GL33.glTexImage2D(GL33.GL_TEXTURE_2D, 0, GL33.GL_RGBA, 1, 1, 0, GL33.GL_RGBA, GL33.GL_UNSIGNED_BYTE, whitePixel);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MIN_FILTER, GL33.GL_NEAREST);
        GL33.glTexParameteri(GL33.GL_TEXTURE_2D, GL33.GL_TEXTURE_MAG_FILTER, GL33.GL_NEAREST);
        MemoryUtil.memFree(whitePixel);
    }
}