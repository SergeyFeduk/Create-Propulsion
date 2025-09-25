package com.deltasf.createpropulsion.utility;

import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public class GoggleUtils {
    //Just reversed version of create TooltipHelper.makeProgressBar
    public static String makeObstructionBar(int length, int filledLength) {
        String bar = " ";
        int i;
        for(i = 0; i < length; ++i) {
            bar = bar + "▒";
        }

        for(i = 0; i < filledLength - length; ++i) {
           bar = bar + "█";
        }
        return bar + " ";
    }

    //efficiency must be in range 0 ~ 100
    public static ChatFormatting efficiencyColor(float efficiency) {
        ChatFormatting tooltipColor;
        if (efficiency < 10) {
            tooltipColor = ChatFormatting.RED;
        } else if (efficiency < 60) {
            tooltipColor = ChatFormatting.GOLD;
        } else if (efficiency < 100) {
            tooltipColor = ChatFormatting.YELLOW;
        } else {
            tooltipColor = ChatFormatting.GREEN;
        }
        return tooltipColor;
    }

    public static LangBuilder LensTooltip(ItemStack lensStack, List<Component> tooltip) {
        //- {Lensname}
        return Lang.builder()
            .text("- ")
            .add(Lang.itemName(lensStack).style(ChatFormatting.GOLD));
    }
}
