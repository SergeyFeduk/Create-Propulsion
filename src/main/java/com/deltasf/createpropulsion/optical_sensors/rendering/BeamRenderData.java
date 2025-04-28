package com.deltasf.createpropulsion.optical_sensors.rendering;

import org.joml.Vector3f;
import org.joml.Vector4f;
import com.mojang.blaze3d.vertex.PoseStack;

public class BeamRenderData {
    public Vector4f startColor;
    public Vector4f endColor;

    public Vector3f normalBottom;
    public Vector3f normalRight;
    public Vector3f normalTop;
    public Vector3f normalLeft;

    public Vector3f sBottomLeft;
    public Vector3f sBottomRight;
    public Vector3f sTopRight;
    public Vector3f sTopLeft;

    public Vector3f eBottomLeft;
    public Vector3f eBottomRight;
    public Vector3f eTopRight;
    public Vector3f eTopLeft;
    public final PoseStack.Pose poseSnapshot;
    
    public BeamRenderData(
        Vector4f startColor,
        Vector4f endColor,

        Vector3f normalBottom,
        Vector3f normalRight,
        Vector3f normalTop,
        Vector3f normalLeft,

        Vector3f sBottomLeft,
        Vector3f sBottomRight,
        Vector3f sTopRight,
        Vector3f sTopLeft,
        Vector3f eBottomLeft,
        Vector3f eBottomRight,
        Vector3f eTopRight,
        Vector3f eTopLeft,
        PoseStack.Pose poseSnapshot
    ) {
        this.startColor = new Vector4f(startColor);
        this.endColor = new Vector4f(endColor);

        this.normalBottom = new Vector3f(normalBottom);
        this.normalRight = new Vector3f(normalRight);
        this.normalTop = new Vector3f(normalTop);
        this.normalLeft = new Vector3f(normalLeft);

        this.sBottomLeft = new Vector3f(sBottomLeft);
        this.sBottomRight = new Vector3f(sBottomRight);
        this.sTopRight = new Vector3f(sTopRight);
        this.sTopLeft = new Vector3f(sTopLeft);
        this.eBottomLeft = new Vector3f(eBottomLeft);
        this.eBottomRight = new Vector3f(eBottomRight);
        this.eTopRight = new Vector3f(eTopRight);
        this.eTopLeft = new Vector3f(eTopLeft);
        this.poseSnapshot = poseSnapshot;
    }
}
