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
import org.lwjgl.opengl.ARBInstancedArrays;
import org.lwjgl.opengl.GL31;
import org.lwjgl.system.MemoryUtil;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.internal.ships.VsiQueryableShipData;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

public class InstancedParticleRenderer {
    private static final int R = 225;
    private static final int G = 225;
    private static final int B = 225;

    private static final double MAX_RENDER_DISTANCE = 256;
    private static final double MAX_RENDER_DISTANCE_SQ = MAX_RENDER_DISTANCE * MAX_RENDER_DISTANCE;

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
    private static final FloatBuffer UPLOAD_BUFFER = MemoryUtil.memAllocFloat(MAX_PARTICLES);
    
    private static final Matrix4f TEMP_MATRIX = new Matrix4f();
    private static final Vector4d TEMP_VEC4 = new Vector4d();
    private static final Vector3d TEMP_CAM_POS = new Vector3d();

    public static void init() {
        if (initialized) return;
        programId = ParticleShaderLoader.createProgram("hap.vert", "hap.frag");
        
        //Uniforms!!!
        uProjectionMat = GL31.glGetUniformLocation(programId, "uProjMat");
        uModelViewMat = GL31.glGetUniformLocation(programId, "uModelViewMat");
        uCamRight = GL31.glGetUniformLocation(programId, "uCamRight");
        uCamUp = GL31.glGetUniformLocation(programId, "uCamUp");
        uShipRotation = GL31.glGetUniformLocation(programId, "uShipRotation");
        uRelativeAnchor = GL31.glGetUniformLocation(programId, "uRelativeAnchor");
        uPartialTick = GL31.glGetUniformLocation(programId, "uPartialTick");
        uColor = GL31.glGetUniformLocation(programId, "uColor");

        //Buffers
        VAOId = GL31.glGenVertexArrays();
        GL31.glBindVertexArray(VAOId);
        
        float[] quadData = {
            -0.5f, -0.5f, 0.0f, 0.0f,
             0.5f, -0.5f, 1.0f, 0.0f,
             0.5f,  0.5f, 1.0f, 1.0f,
            -0.5f,  0.5f, 0.0f, 1.0f
        };
        
        quadVBOId = GL31.glGenBuffers();
        GL31.glBindBuffer(GL31.GL_ARRAY_BUFFER, quadVBOId);
        GL31.glBufferData(GL31.GL_ARRAY_BUFFER, quadData, GL31.GL_STATIC_DRAW);

        //Quad position
        GL31.glEnableVertexAttribArray(0);
        GL31.glVertexAttribPointer(0, 2, GL31.GL_FLOAT, false, 16, 0);
        
        //Quad UV
        GL31.glEnableVertexAttribArray(1);
        GL31.glVertexAttribPointer(1, 2, GL31.GL_FLOAT, false, 16, 8);

        instanceVBOId = GL31.glGenBuffers();
        GL31.glBindBuffer(GL31.GL_ARRAY_BUFFER, instanceVBOId);
        GL31.glBufferData(GL31.GL_ARRAY_BUFFER, TOTAL_BUFFER_SIZE, GL31.GL_STREAM_DRAW);

        setupAttrib(2, 0);
        setupAttrib(3, ARRAY_SIZE_BYTES);
        setupAttrib(4, ARRAY_SIZE_BYTES * 2);
        setupAttrib(5, ARRAY_SIZE_BYTES * 3);
        setupAttrib(6, ARRAY_SIZE_BYTES * 4);
        setupAttrib(7, ARRAY_SIZE_BYTES * 5);
        setupAttrib(8, ARRAY_SIZE_BYTES * 6);
        setupAttrib(9, ARRAY_SIZE_BYTES * 7);
        
        generateWhitePixelTexture();

        GL31.glBindVertexArray(0);
        GL31.glBindBuffer(GL31.GL_ARRAY_BUFFER, 0);
        
        initialized = true;
    }

    private static void setupAttrib(int index, long offset) {
        GL31.glEnableVertexAttribArray(index);
        GL31.glVertexAttribPointer(index, 1, GL31.GL_FLOAT, false, 0, offset);
        ARBInstancedArrays.glVertexAttribDivisorARB(index, 1);
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
        
        GL31.glUseProgram(programId);
        GL31.glBindVertexArray(VAOId);

        //Bind pixel texture
        GL31.glActiveTexture(GL31.GL_TEXTURE0);
        GL31.glBindTexture(GL31.GL_TEXTURE_2D, textureId);
        int uTexLoc = GL31.glGetUniformLocation(programId, "uTex");
        GL31.glUniform1i(uTexLoc, 0);

        //Projection Matrix
        projectionMatrix.get(MATRIX_BUFFER);
        GL31.glUniformMatrix4fv(uProjectionMat, false, MATRIX_BUFFER);

        //ModelView Matrix
        Matrix4f modelViewMatrix = ms.last().pose();
        modelViewMatrix.get(MATRIX_BUFFER);
        GL31.glUniformMatrix4fv(uModelViewMat, false, MATRIX_BUFFER);

        //Camera Vectors
        Vector3f camRight = camera.getLeftVector().mul(-1.0f);
        GL31.glUniform3f(uCamRight, camRight.x(), camRight.y(), camRight.z());

        Vector3f camUp = camera.getUpVector();
        GL31.glUniform3f(uCamUp, camUp.x(), camUp.y(), camUp.z());

        Vector3d camPos = TEMP_CAM_POS.set(camera.getPosition().x, camera.getPosition().y, camera.getPosition().z);

        float alpha = PropulsionConfig.BALLOON_PARTICLES_ALPHA.get().floatValue() / 255.0f;
        GL31.glUniform4f(uColor, R/255.0f, G/255.0f, B/255.0f, alpha);

        VsiQueryableShipData<ClientShip> loadedShips = VSGameUtilsKt.getShipObjectWorld(mc.level).getLoadedShips();
        
        //Rendering
        Long2ObjectMap<ShipParticleHandler> handlers = BalloonParticleSystem.getAllHandlers();
        for (Long2ObjectMap.Entry<ShipParticleHandler> entry : handlers.long2ObjectEntrySet()) {
            long shipId = entry.getLongKey();
            ShipParticleHandler handler = entry.getValue();
            if (handler.isEmpty()) continue;

            ClientShip ship = loadedShips.getById(shipId);
            if (ship == null) continue;

            Matrix4dc shipToWorld = ship.getRenderTransform().getShipToWorld();
            //Calculate anchor position in world space
            TEMP_VEC4.set(handler.getAnchorX(), handler.getAnchorY(), handler.getAnchorZ(), 1.0);
            shipToWorld.transform(TEMP_VEC4);

            double distSq = camPos.distanceSquared(TEMP_VEC4.x, TEMP_VEC4.y, TEMP_VEC4.z);
            if (distSq > MAX_RENDER_DISTANCE_SQ) continue;

            //pT based on simulation (if not ticking/paused -> pT = 1)
            float t = (isPaused || handler.lastSimulatedTick == gameTime) ? partialTick : 1.0f;
            GL31.glUniform1f(uPartialTick, t);

            HapData data = handler.data;
            int count = Math.min(data.count, MAX_PARTICLES);
            if (count <= 0) continue;

            //Upload stuff
            GL31.glBindBuffer(GL31.GL_ARRAY_BUFFER, instanceVBOId);
            
            uploadData(0, data.px, count);
            uploadData(ARRAY_SIZE_BYTES, data.py, count);
            uploadData(ARRAY_SIZE_BYTES * 2, data.pz, count);
            uploadData(ARRAY_SIZE_BYTES * 3, data.x, count);
            uploadData(ARRAY_SIZE_BYTES * 4, data.y, count);
            uploadData(ARRAY_SIZE_BYTES * 5, data.z, count);
            uploadData(ARRAY_SIZE_BYTES * 6, data.life, count);
            uploadData(ARRAY_SIZE_BYTES * 7, data.scale, count);

            //Ship uniforms
            TEMP_MATRIX.set(shipToWorld);
            //Translation components set to zero cus we only need rotation and scale from this matrix
            TEMP_MATRIX.m30(0); TEMP_MATRIX.m31(0); TEMP_MATRIX.m32(0);
            TEMP_MATRIX.get(MATRIX_BUFFER);
            GL31.glUniformMatrix4fv(uShipRotation, false, MATRIX_BUFFER);

            //Anchor relative to camera
            float rx = (float)(TEMP_VEC4.x - camPos.x);
            float ry = (float)(TEMP_VEC4.y - camPos.y);
            float rz = (float)(TEMP_VEC4.z - camPos.z);
            
            GL31.glUniform3f(uRelativeAnchor, rx, ry, rz);

            //Perform a draw
            GL31.glDrawArraysInstanced(GL31.GL_TRIANGLE_FAN, 0, 4, count);
        }

        //Tidy up after outselves
        GL31.glBindVertexArray(0);
        GL31.glUseProgram(0);

        GL31.glBindBuffer(GL31.GL_ARRAY_BUFFER, 0); 
        GL31.glBindTexture(GL31.GL_TEXTURE_2D, 0);

        RenderSystem.depthMask(true); 
        RenderSystem.disableBlend();
    }
    
    private static void uploadData(long offset, float[] data, int count) {
        UPLOAD_BUFFER.clear();
        UPLOAD_BUFFER.put(data, 0, count);
        UPLOAD_BUFFER.flip();
        GL31.glBufferSubData(GL31.GL_ARRAY_BUFFER, offset, UPLOAD_BUFFER);
    }
    
    public static void destroy() {
        if (!initialized) return;
        GL31.glDeleteVertexArrays(VAOId);
        GL31.glDeleteBuffers(quadVBOId);
        GL31.glDeleteBuffers(instanceVBOId);
        GL31.glDeleteTextures(textureId);
        GL31.glDeleteProgram(programId);
        
        MemoryUtil.memFree(MATRIX_BUFFER);
        MemoryUtil.memFree(UPLOAD_BUFFER);
        
        initialized = false;
    }

    //TODO: Move in some utility class
    private static void generateWhitePixelTexture() {
        textureId = GL31.glGenTextures();
        GL31.glBindTexture(GL31.GL_TEXTURE_2D, textureId);
        ByteBuffer whitePixel = MemoryUtil.memAlloc(4);
        whitePixel.put((byte)255).put((byte)255).put((byte)255).put((byte)255);
        whitePixel.flip();
        GL31.glTexImage2D(GL31.GL_TEXTURE_2D, 0, GL31.GL_RGBA, 1, 1, 0, GL31.GL_RGBA, GL31.GL_UNSIGNED_BYTE, whitePixel);
        GL31.glTexParameteri(GL31.GL_TEXTURE_2D, GL31.GL_TEXTURE_MIN_FILTER, GL31.GL_NEAREST);
        GL31.glTexParameteri(GL31.GL_TEXTURE_2D, GL31.GL_TEXTURE_MAG_FILTER, GL31.GL_NEAREST);
        MemoryUtil.memFree(whitePixel);
    }
}