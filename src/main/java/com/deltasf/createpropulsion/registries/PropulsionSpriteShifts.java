package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;

import net.minecraft.resources.ResourceLocation;

public class PropulsionSpriteShifts {
    public static final CTSpriteShiftEntry WING_TEXTURE = getCT(AllCTTypes.OMNIDIRECTIONAL, "wing");
    public static final CTSpriteShiftEntry TEMPERED_WING_TEXTURE = getCT(AllCTTypes.OMNIDIRECTIONAL, "tempered_wing");

    private static CTSpriteShiftEntry getCT(CTType type, String blockTextureName) {
        return CTSpriteShifter.getCT(type,
            ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/" + blockTextureName),
            ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/" + blockTextureName + "_connected")
        );
    }

}