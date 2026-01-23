package com.deltasf.createpropulsion.balloons.particles.rendering;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.lwjgl.opengl.GL33;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class ParticleShaderLoader {
    public static int createProgram(String vertFilename, String fragFilename) {
        String vertSource = loadResource(vertFilename);
        String fragSource = loadResource(fragFilename);

        int vertexId = GL33.glCreateShader(GL33.GL_VERTEX_SHADER);
        GL33.glShaderSource(vertexId, vertSource);
        GL33.glCompileShader(vertexId);
        if (GL33.glGetShaderi(vertexId, GL33.GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Vert Shader Error (" + vertFilename + "): " + GL33.glGetShaderInfoLog(vertexId));

        int fragmentId = GL33.glCreateShader(GL33.GL_FRAGMENT_SHADER);
        GL33.glShaderSource(fragmentId, fragSource);
        GL33.glCompileShader(fragmentId);
        if (GL33.glGetShaderi(fragmentId, GL33.GL_COMPILE_STATUS) == 0)
            throw new RuntimeException("Frag Shader Error (" + fragFilename + "): " + GL33.glGetShaderInfoLog(fragmentId));

        int programId = GL33.glCreateProgram();
        GL33.glAttachShader(programId, vertexId);
        GL33.glAttachShader(programId, fragmentId);
        GL33.glLinkProgram(programId);
        if (GL33.glGetProgrami(programId, GL33.GL_LINK_STATUS) == 0)
            throw new RuntimeException("Program Link Error: " + GL33.glGetProgramInfoLog(programId));

        GL33.glDeleteShader(vertexId);
        GL33.glDeleteShader(fragmentId);
        return programId;
    }

    private static String loadResource(String filename) {
        ResourceLocation location = new ResourceLocation("createpropulsion", "shaders/" + filename);
        try {
            Resource resource = Minecraft.getInstance().getResourceManager().getResourceOrThrow(location);
            try (InputStream in = resource.open()) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            throw new RuntimeException("Failed to load shader: " + location, e);
        }
    }
}