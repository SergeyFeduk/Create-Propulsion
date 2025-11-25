package com.deltasf.createpropulsion.reaction_wheel;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.joml.Vector3i;

@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY
)
public class ReactionWheelData {
    public volatile float rotationSpeed;
    public volatile ReactionWheelMode mode; 
    public volatile Vector3i facing;

    public ReactionWheelData() {}

    public enum ReactionWheelMode {
        DIRECT,
        STABILIZATION
    }
}
