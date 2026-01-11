package com.deltasf.createpropulsion.redstone_transmission;

import com.simibubi.create.content.redstone.displayLink.DisplayLinkContext;
import com.simibubi.create.content.redstone.displayLink.source.SingleLineDisplaySource;
import com.simibubi.create.content.redstone.displayLink.target.DisplayTargetStats;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.level.block.entity.BlockEntity;

public class RedstoneTransmissionDisplaySource extends SingleLineDisplaySource {
    @Override
    protected MutableComponent provideLine(DisplayLinkContext context, DisplayTargetStats stats) {
        BlockEntity sourceBE = context.getSourceBlockEntity();
        if(!(sourceBE instanceof RedstoneTransmissionBlockEntity rtbe)) return EMPTY_LINE;
        return Component.literal(Integer.toString(rtbe.get_current_shift()));
    }

    @Override
    protected String getTranslationKey() {
        return "redstone_transmission_shift";
    }

    @Override
    protected boolean allowsLabeling(DisplayLinkContext context) {
        return false;
    }
}
