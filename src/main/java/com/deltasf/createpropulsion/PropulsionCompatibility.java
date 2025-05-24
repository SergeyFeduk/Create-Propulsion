package com.deltasf.createpropulsion;

import net.minecraftforge.fml.ModList;

public class PropulsionCompatibility {
    public static final boolean CBC_ACTIVE = ModList.get().isLoaded("createbigcannons");
    public static final boolean CC_ACTIVE = ModList.get().isLoaded("computercraft");
    public static final boolean JEI_ACTIVE = ModList.get().isLoaded("jei");

    public static final boolean SHIMMER_ACTIVE = ModList.get().isLoaded("shimmer");
}
