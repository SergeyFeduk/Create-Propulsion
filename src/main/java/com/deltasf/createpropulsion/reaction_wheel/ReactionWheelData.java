package com.deltasf.createpropulsion.reaction_wheel;

import org.joml.Vector3i;

public class ReactionWheelData {
    public volatile float rotationSpeed;
    public volatile ReactionWheelMode mode; 
    public volatile Vector3i facing;

    public enum ReactionWheelMode {
        DIRECT,
        STABILIZATION
    }
}
