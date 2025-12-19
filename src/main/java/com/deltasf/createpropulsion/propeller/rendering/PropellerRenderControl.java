package com.deltasf.createpropulsion.propeller.rendering;

import com.deltasf.createpropulsion.accessors.IInstancedInstancerExposer;
import com.deltasf.createpropulsion.registries.PropulsionInstanceTypes;
import dev.engine_room.flywheel.backend.engine.AbstractInstancer;
import dev.engine_room.flywheel.backend.engine.InstancerKey;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class PropellerRenderControl {
    public static int renderStage = 0;
    
    // We store the specific draw objects (InstancedDraw) to skip this frame
    public static final Set<Object> hiddenDraws = new HashSet<>();

    public static boolean shouldSkip(InstancerKey<?> key) {
        boolean isBlur = key.type() == PropulsionInstanceTypes.PROPELLER_BLUR;
        if (renderStage == 0) return isBlur;
        else return !isBlur;
    }

    // Helper to find the draws for an instancer and hide them
    public static void hideInstancer(AbstractInstancer<?> instancer) {
        if (instancer instanceof IInstancedInstancerExposer exposer) {
            List<?> draws = exposer.createpropulsion$getDraws();
            if (draws != null) {
                hiddenDraws.addAll(draws);
            }
        }
    }

    // Helper to filter the main render list
    public static List<?> filterDraws(List<?> originalDraws) {
        if (hiddenDraws.isEmpty()) return originalDraws;

        List<Object> filtered = new ArrayList<>(originalDraws.size());
        for (Object draw : originalDraws) {
            if (!hiddenDraws.contains(draw)) {
                filtered.add(draw);
            }
        }
        return filtered;
    }
}