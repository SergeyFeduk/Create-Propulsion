package com.deltasf.createpropulsion;

import net.minecraftforge.fml.ModList;

public class PropulsionCompatibility {
    public static final boolean CDG_ACTIVE = ModList.get().isLoaded("createdieselgenerators");
    public static final boolean CBC_ACTIVE = ModList.get().isLoaded("createbigcannons");
    public static final boolean TFMG_ACTIVE = ModList.get().isLoaded("tfmg");
    public static final boolean SHIMMER_ACTIVE = ModList.get().isLoaded("shimmer");
}
