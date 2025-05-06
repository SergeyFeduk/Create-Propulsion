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
    public PoseStack.Pose poseSnapshot;
    
    public BeamRenderData() {
        this.startColor = new Vector4f();
        this.endColor = new Vector4f();

        this.normalBottom = new Vector3f();
        this.normalRight = new Vector3f();
        this.normalTop = new Vector3f();
        this.normalLeft = new Vector3f();

        this.sBottomLeft = new Vector3f();
        this.sBottomRight = new Vector3f();
        this.sTopRight = new Vector3f();
        this.sTopLeft = new Vector3f();
        this.eBottomLeft = new Vector3f();
        this.eBottomRight = new Vector3f();
        this.eTopRight = new Vector3f();
        this.eTopLeft = new Vector3f();

        this.poseSnapshot = null;
    }
}
