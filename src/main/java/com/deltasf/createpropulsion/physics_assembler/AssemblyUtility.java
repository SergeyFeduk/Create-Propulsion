package com.deltasf.createpropulsion.physics_assembler;

import com.simibubi.create.AllSpecialTextures;
import com.simibubi.create.CreateClient;

import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;

public class AssemblyUtility {
    public static final int PASSIVE_COLOR = 0xaf68c5;
    public static final int HIGHLIGHT_COLOR = 0xda97f0;
    public static final int CANCEL_COLOR = 0xFF5555;

    public static void renderOutline(String key, AABB boundingBox, int color, float lineWidth, boolean showFace) {
        var outline = CreateClient.OUTLINER.showAABB(key, boundingBox)
            .colored(color)
            .lineWidth(lineWidth)
            .disableLineNormals();

        if (showFace) {
            outline.withFaceTexture(AllSpecialTextures.SELECTION);
        }
    }

    public static int lerpColor(float progress, int color1, int color2) {
        int r1 = (color1 >> 16) & 0xFF, g1 = (color1 >> 8) & 0xFF, b1 = color1 & 0xFF;
        int r2 = (color2 >> 16) & 0xFF, g2 = (color2 >> 8) & 0xFF, b2 = color2 & 0xFF;
        int r = (int) Mth.lerp(progress, r1, r2), g = (int) Mth.lerp(progress, g1, g2), b = (int) Mth.lerp(progress, b1, b2);
        return (r << 16) | (g << 8) | b;
    }
}
