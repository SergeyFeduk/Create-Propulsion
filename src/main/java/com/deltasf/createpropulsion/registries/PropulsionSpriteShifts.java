package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.simibubi.create.foundation.block.connected.AllCTTypes;
import com.simibubi.create.foundation.block.connected.CTSpriteShiftEntry;
import com.simibubi.create.foundation.block.connected.CTSpriteShifter;
import com.simibubi.create.foundation.block.connected.CTType;

import net.createmod.catnip.render.SpriteShiftEntry;
import net.createmod.catnip.render.SpriteShifter;
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

    public static final SpriteShiftEntry HOT_AIR_PUMP = get("hot_air_pump", "hot_air_pump");

    private static SpriteShiftEntry get(String original, String target) {
        return SpriteShifter.get(
            ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/" + original),
            ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "block/" + target)
        );
    }
}