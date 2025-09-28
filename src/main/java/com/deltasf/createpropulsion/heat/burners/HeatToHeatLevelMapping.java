package com.deltasf.createpropulsion.heat.burners;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

public class HeatToHeatLevelMapping {
    public static float getMinHeatPercent(HeatLevel heatLevel) {
        if (heatLevel == HeatLevel.KINDLED) return 0.6f;
        if (heatLevel == HeatLevel.FADING) return 0.3f;
        return 0.0f;
    } 

    public static HeatLevel getHeatLevel(float percentage) {
        if (percentage > 0.6f) return HeatLevel.KINDLED;
        if (percentage > 0.3f) return HeatLevel.FADING;
        return HeatLevel.NONE; 
    }
}
