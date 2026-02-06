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
import net.minecraft.world.level.Level;

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

    private static int uPartialTick;
    private static int uColor;

    private static final int FLOAT_SIZE = 4;
    private static final int MAX_PARTICLES = 16384;
    private static final int ARRAY_SIZE_BYTES = MAX_PARTICLES * FLOAT_SIZE; 
    private static final int TOTAL_BUFFER_SIZE = ARRAY_SIZE_BYTES * 8; 
    
    private static final FloatBuffer MATRIX_BUFFER = MemoryUtil.memAllocFloat(16);
    private static final Matrix4f TEMP_MATRIX = new Matrix4f();
    private static final Vector4d TEMP_VEC4 = new Vector4d();

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
        uPartialTick = GL33.glGetUniformLocation(programId, "uPartialTick");
        uColor = GL33.glGetUniformLocation(programId, "uColor");

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

        //Quad position
        GL33.glEnableVertexAttribArray(0);
        GL33.glVertexAttribPointer(0, 2, GL33.GL_FLOAT, false, 16, 0);
        
        //Quad UV
        GL33.glEnableVertexAttribArray(1);
        GL33.glVertexAttribPointer(1, 2, GL33.GL_FLOAT, false, 16, 8);

        instanceVBOId = GL33.glGenBuffers();
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, instanceVBOId);
        GL33.glBufferData(GL33.GL_ARRAY_BUFFER, TOTAL_BUFFER_SIZE, GL33.GL_STREAM_DRAW);

        setupAttrib(2, 0);
        setupAttrib(3, ARRAY_SIZE_BYTES);
        setupAttrib(4, ARRAY_SIZE_BYTES * 2);
        setupAttrib(5, ARRAY_SIZE_BYTES * 3);
        setupAttrib(6, ARRAY_SIZE_BYTES * 4);
        setupAttrib(7, ARRAY_SIZE_BYTES * 5);
        setupAttrib(8, ARRAY_SIZE_BYTES * 6);
        setupAttrib(9, ARRAY_SIZE_BYTES * 7);
        
        generateWhitePixelTexture();

        GL33.glBindVertexArray(0);
        GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, 0);
        
        initialized = true;
    }

    private static void setupAttrib(int index, long offset) {
        GL33.glEnableVertexAttribArray(index);
        GL33.glVertexAttribPointer(index, 1, GL33.GL_FLOAT, false, 0, offset);
        GL33.glVertexAttribDivisor(index, 1);
    }

    public static void render(PoseStack ms, Matrix4f projectionMatrix, Camera camera, float partialTick) {
        if (!initialized) init();
        if (BalloonParticleSystem.getAllHandlers().isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        Level level = mc.level;
        if (level == null) return;
        long gameTime = level.getGameTime();
        boolean isPaused = mc.isPaused();
        
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

        float alpha = PropulsionConfig.BALLOON_PARTICLES_ALPHA.get().floatValue() / 255.0f;
        GL33.glUniform4f(uColor, R/255.0f, G/255.0f, B/255.0f, alpha);

        //Rendering
        Long2ObjectMap<ShipParticleHandler> handlers = BalloonParticleSystem.getAllHandlers();
        for (Long2ObjectMap.Entry<ShipParticleHandler> entry : handlers.long2ObjectEntrySet()) {
            long shipId = entry.getLongKey();
            ShipParticleHandler handler = entry.getValue();
            if (handler.isEmpty()) continue;

            ClientShip ship = (ClientShip) VSGameUtilsKt.getShipObjectWorld(mc.level).getLoadedShips().getById(shipId);
            if (ship == null) continue;

            float t = (isPaused || handler.lastSimulatedTick == gameTime) ? partialTick : 1.0f;
            GL33.glUniform1f(uPartialTick, t);

            HapData data = handler.data;
            int count = Math.min(data.count, MAX_PARTICLES);

            //Upload stuff
            GL33.glBindBuffer(GL33.GL_ARRAY_BUFFER, instanceVBOId);
            
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, 0, data.px);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES, data.py);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES * 2, data.pz);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES * 3, data.x);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES * 4, data.y);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES * 5, data.z);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES * 6, data.life);
            GL33.glBufferSubData(GL33.GL_ARRAY_BUFFER, ARRAY_SIZE_BYTES * 7, data.scale);

            //Ship uniforms
            Matrix4dc shipToWorld = ship.getRenderTransform().getShipToWorld();
            
            TEMP_MATRIX.set(shipToWorld);
            //Translation components set to zero cus we only need rotation and scale from this matrix
            TEMP_MATRIX.m30(0); TEMP_MATRIX.m31(0); TEMP_MATRIX.m32(0);
            TEMP_MATRIX.get(MATRIX_BUFFER);
            GL33.glUniformMatrix4fv(uShipRotation, false, MATRIX_BUFFER);

            //Calculate anchor position in world space
            TEMP_VEC4.set(handler.getAnchorX(), handler.getAnchorY(), handler.getAnchorZ(), 1.0);
            shipToWorld.transform(TEMP_VEC4);

            //Anchor relative to camera
            float rx = (float)(TEMP_VEC4.x - camPos.x);
            float ry = (float)(TEMP_VEC4.y - camPos.y);
            float rz = (float)(TEMP_VEC4.z - camPos.z);
            
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