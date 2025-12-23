package com.deltasf.createpropulsion.registries;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.design_goggles.DesignGogglesItem;
import com.deltasf.createpropulsion.optical_sensors.OpticalLensItem;
import com.deltasf.createpropulsion.physics_assembler.AssemblyGaugeItem;
import com.deltasf.createpropulsion.propeller.blades.AndesitePropellerBladeItem;
import com.deltasf.createpropulsion.propeller.blades.CopperPropellerBladeItem;
import com.deltasf.createpropulsion.propeller.blades.WoodenPropellerBladeItem;
import com.deltasf.createpropulsion.utility.BurnableItem;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.tterrag.registrate.providers.DataGenContext;
import com.tterrag.registrate.providers.ProviderType;
import com.tterrag.registrate.providers.RegistrateItemModelProvider;
import com.tterrag.registrate.providers.RegistrateLangProvider;
import com.tterrag.registrate.util.entry.ItemEntry;
import com.tterrag.registrate.util.nullness.NonNullBiConsumer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public class PropulsionItems {
    public static final CreateRegistrate REGISTRATE = CreatePropulsion.registrate();
    public static void register() {} //Loads this class

    private static <I extends Item> NonNullBiConsumer<DataGenContext<Item, I>, RegistrateItemModelProvider> FUCK_OFF_ITEM() {
        return (ctx, prov) -> {};
    }

    private static <R, T extends R> NonNullBiConsumer<DataGenContext<R, T>, RegistrateLangProvider> FUCK_OFF_LANG() {
        return (ctx, prov) -> {};
    }

    //Tags
    public static final TagKey<Item> OPTICAL_LENS_TAG = makeTag("optical_lens");
    public static final TagKey<Item> PROPELLER_BLADE_TAG = makeTag("blade");

    public static final ItemEntry<BurnableItem> PINE_RESIN = REGISTRATE.item("pine_resin", p -> new BurnableItem(p, 1200))
        .model(FUCK_OFF_ITEM())
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    //Lenses
    public static final ItemEntry<OpticalLensItem> OPTICAL_LENS = REGISTRATE.item("optical_lens", OpticalLensItem::new)
        .model(FUCK_OFF_ITEM())
        .tag(OPTICAL_LENS_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<Item> FLUID_LENS = REGISTRATE.item("fluid_lens", Item::new)
        .model(FUCK_OFF_ITEM())
        .tag(OPTICAL_LENS_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<Item> FOCUS_LENS = REGISTRATE.item("focus_lens", Item::new)
        .model(FUCK_OFF_ITEM())
        .tag(OPTICAL_LENS_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<Item> INVISIBILITY_LENS = REGISTRATE.item("invisibility_lens", Item::new)
        .model(FUCK_OFF_ITEM())
        .tag(OPTICAL_LENS_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<Item> UNFINISHED_LENS = REGISTRATE.item("unfinished_lens", Item::new)
        .model(FUCK_OFF_ITEM())
        .tag(OPTICAL_LENS_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    //Propeller blades
    public static final ItemEntry<WoodenPropellerBladeItem> WOODEN_BLADE = REGISTRATE.item("wooden_blade", WoodenPropellerBladeItem::new)
        .model(FUCK_OFF_ITEM())
        .tag(PROPELLER_BLADE_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<CopperPropellerBladeItem> COPPER_BLADE = REGISTRATE.item("copper_blade", CopperPropellerBladeItem::new)
        .model(FUCK_OFF_ITEM())
        .tag(PROPELLER_BLADE_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<AndesitePropellerBladeItem> ANDESITE_BLADE = REGISTRATE.item("andesite_blade", AndesitePropellerBladeItem::new)
        .model(FUCK_OFF_ITEM())
        .tag(PROPELLER_BLADE_TAG)
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();

    public static final ItemEntry<DesignGogglesItem> DESIGN_GOGGLES = REGISTRATE.item("design_goggles", DesignGogglesItem::new)
        .model(FUCK_OFF_ITEM())
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .register();
    public static final ItemEntry<AssemblyGaugeItem> ASSEMBLY_GAUGE = REGISTRATE.item("assembly_gauge", AssemblyGaugeItem::new)
        .model(FUCK_OFF_ITEM())
        .setData(ProviderType.LANG, FUCK_OFF_LANG())
        .properties(p -> p.stacksTo(1))
        .register();

    public static TagKey<Item> makeTag(String key) {
        ResourceLocation resource = ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, key);
        return TagKey.create(Registries.ITEM, resource);
    }
}
