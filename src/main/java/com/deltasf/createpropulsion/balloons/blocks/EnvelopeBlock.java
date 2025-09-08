package com.deltasf.createpropulsion.balloons.blocks;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nonnull;

import com.deltasf.createpropulsion.balloons.registries.BalloonShipRegistry;
import com.deltasf.createpropulsion.balloons.registries.BalloonUpdater;

public class EnvelopeBlock extends AbstractEnvelopeBlock {
    public EnvelopeBlock(Properties properties) {
        super(properties);
    }
}
