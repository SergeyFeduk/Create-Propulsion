package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.optical_sensors.OpticalLensItem;
import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
import com.deltasf.createpropulsion.propeller.blades.AndesitePropellerBladeItem;
import com.deltasf.createpropulsion.propeller.blades.CopperPropellerBladeItem;
import com.deltasf.createpropulsion.propeller.blades.WoodenPropellerBladeItem;
import com.deltasf.createpropulsion.utility.BurnableItem;
//import com.deltasf.createpropulsion.design_goggles.DesignGogglesItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.util.entry.ItemEntry;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class PropulsionItems {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    public static final ItemEntry<BurnableItem> PINE_RESIN = REGISTRATE.item("pine_resin", p -> new BurnableItem(p, 1200)).register();
    //Lenses
    public static final ItemEntry<OpticalLensItem> OPTICAL_LENS = REGISTRATE.item("optical_lens", OpticalLensItem::new).register();
    public static final ItemEntry<Item> FLUID_LENS = REGISTRATE.item("fluid_lens", Item::new).register();
    public static final ItemEntry<Item> FOCUS_LENS = REGISTRATE.item("focus_lens", Item::new).register();
    public static final ItemEntry<Item> INVISIBILITY_LENS = REGISTRATE.item("invisibility_lens", Item::new).register();
    public static final ItemEntry<Item> UNFINISHED_LENS = REGISTRATE.item("unfinished_lens", Item::new).register();
    //Propeller blades
    public static final ItemEntry<WoodenPropellerBladeItem> WOODEN_BLADE = REGISTRATE.item("wooden_blade", WoodenPropellerBladeItem::new).register();
    public static final ItemEntry<CopperPropellerBladeItem> COPPER_BLADE = REGISTRATE.item("copper_blade", CopperPropellerBladeItem::new).register();
    public static final ItemEntry<AndesitePropellerBladeItem> ANDESITE_BLADE = REGISTRATE.item("andesite_blade", AndesitePropellerBladeItem::new).register();

    //public static final ItemEntry<DesignGogglesItem> DESIGN_GOGGLES = REGISTRATE.item("design_goggles", DesignGogglesItem::new).register();
    public static final ItemEntry<AssemblyGaugeItem> ASSEMBLY_GAUGE = REGISTRATE.item("assembly_gauge", AssemblyGaugeItem::new)
        .properties(p -> p.stacksTo(1))
        .register();


    //Tags
    public static final TagKey<Item> OPTICAL_LENS_TAG = makeTag("optical_lens");
    public static final TagKey<Item> PROPELLER_BLADE_TAG = makeTag("blade");

    public static TagKey<Item> makeTag(String key) {
        ResourceLocation resource = new ResourceLocation(CreatePropulsion.ID, key);
        TagKey<Item> tag = TagKey.create(Registries.ITEM, resource);
        //No datagen :(
        return tag;
    }
}
