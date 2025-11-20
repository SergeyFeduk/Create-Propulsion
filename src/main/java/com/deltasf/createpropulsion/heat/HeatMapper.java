package com.deltasf.createpropulsion.heat;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

public class HeatMapper {
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

    public static enum HeatLevelString {
        COLD, WARM, HOT, SEARING;
    }

    public static HeatLevelString getHeatString(float percentage) {
        if (percentage > 0.6f) return HeatLevelString.SEARING;
        if (percentage > 0.3f) return HeatLevelString.HOT;
        if (percentage > 0.1f) return HeatLevelString.WARM; 
        return HeatLevelString.COLD;
    }
}
