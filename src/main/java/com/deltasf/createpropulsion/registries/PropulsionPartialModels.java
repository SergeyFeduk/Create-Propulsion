package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;


public class PropulsionPartialModels {
    public static final PartialModel LODESTONE_TRACKER_INDICATOR = block("lodestone_tracker_overlay");

    private static PartialModel block(String path) {
        return PartialModel.of(new ResourceLocation(CreatePropulsion.ID, "block/" + path));
    }

    public static void register() {}
}