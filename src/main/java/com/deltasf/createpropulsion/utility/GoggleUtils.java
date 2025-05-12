package com.deltasf.createpropulsion.utility;

import net.minecraft.ChatFormatting;

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
}
