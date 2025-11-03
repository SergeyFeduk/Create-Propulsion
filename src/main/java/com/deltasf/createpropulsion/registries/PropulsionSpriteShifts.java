package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;

import net.minecraft.resources.ResourceLocation;

public class PropulsionSpriteShifts {
    public static final CTSpriteShiftEntry WING_TEXTURE =
        getCT(AllCTTypes.OMNIDIRECTIONAL, "wing");

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return CTSpriteShifter.getCT(type,
            new ResourceLocation(CreatePropulsion.ID, "block/" + blockTextureName),
            new ResourceLocation(CreatePropulsion.ID, "block/" + blockTextureName + "_connected")
        );
    }

}
