package com.deltasf.createpropulsion;

import com.deltasf.createpropulsion.utility.BurnableItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.world.item.Item;

public class PropulsionItems {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    public static final ItemEntry<BurnableItem> PINE_RESIN = REGISTRATE.item("pine_resin", p -> new BurnableItem(p, 1200)).register();
    public static final ItemEntry<Item> OPTICAL_LENS = REGISTRATE.item("optical_lens", Item::new).register();
    public static final ItemEntry<Item> FLUID_LENS = REGISTRATE.item("fluid_lens", Item::new).register();
    //public static final ItemEntry<Item> INVISIBILITY_LENS = REGISTRATE.item("invisibility_lens", Item::new).register();
}
