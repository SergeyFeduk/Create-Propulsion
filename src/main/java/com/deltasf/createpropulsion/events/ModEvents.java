package com.deltasf.createpropulsion.events;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.registries.PropulsionBlocks;
import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.deltasf.createpropulsion.registries.PropulsionBlocks.EnvelopeColor;

import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModEvents {

    //Washing optical lenses in cauldron
    @SubscribeEvent
    public static void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            CauldronInteraction.WATER.put(PropulsionItems.OPTICAL_LENS.get(), CauldronInteraction.DYED_ITEM);
            populateEnvelopeCauldronInteractions();
        });
    }

    private static void populateEnvelopeCauldronInteractions() {
        for(EnvelopeColor color : EnvelopeColor.values()) {
            if (color == EnvelopeColor.WHITE) continue; //Do not wash white
            Item envelopeItem = PropulsionBlocks.getEnvelope(color).asItem();
            CauldronInteraction.WATER.put(envelopeItem, CLEAN_ENVELOPE);
            Item envelopedShaftItem = PropulsionBlocks.getEnvelopedShaft(color).asItem();
            CauldronInteraction.WATER.put(envelopedShaftItem, CLEAN_ENVELOPE);
        }
    }

    public static final CauldronInteraction CLEAN_ENVELOPE = (blockState, level, blockPos, player, hand, stack) -> {
        if (stack.isEmpty())
            return InteractionResult.PASS;

        ItemStack whiteEnvelope = new ItemStack(
            PropulsionBlocks.getEnvelope(EnvelopeColor.WHITE).asItem(),
            stack.getCount()
        );
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, whiteEnvelope));

        LayeredCauldronBlock.lowerFillLevel(blockState, level, blockPos);
        player.awardStat(Stats.USE_CAULDRON);
        level.playSound(null, blockPos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 1.0F, 1.0F);

        return InteractionResult.sidedSuccess(level.isClientSide);
    };
}
