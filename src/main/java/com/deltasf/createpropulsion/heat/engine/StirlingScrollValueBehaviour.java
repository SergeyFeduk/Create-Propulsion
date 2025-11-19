package com.deltasf.createpropulsion.heat.engine;

import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueBoxTransform;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.utility.Lang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class StirlingScrollValueBehaviour extends ScrollValueBehaviour {
    protected final static int STEP = 64;
    //-256 -192 -128 -64 | 64 128 192 256
    // -4   -3   -2   -1 |  1  2   3   4

    public StirlingScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        this.withFormatter(v -> Integer.toString(getRPM()));
    }

    public int getRPM() {
        return Math.abs(getValue() + 1) * STEP;
    }

    public int getSign() {
        return getValue() > 0 ? 1 : 0;
    }

    public int getRpmFromBoardValue(int boardValue) {
        return (boardValue + 1) * STEP;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(
            Component.literal("\u27f3").withStyle(ChatFormatting.BOLD),
            Component.literal("\u27f2").withStyle(ChatFormatting.BOLD)
        );
        
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(label, 4, 1, rows, formatter);
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int val = Math.max(1, valueSetting.value());
        System.out.println("setValueSettings");
        if (!valueSetting.equals(getValueSettings()))
			playFeedbackSound(this);
		setValue(valueSetting.row() == 0 ? -val : val);
        System.out.println(value);
    }

    @Override 
    public ValueSettings getValueSettings() {
        return new ValueSettings(value < 0 ? 0 : 1, Math.abs(value));
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return Lang.number(getRpmFromBoardValue(settings.value()))
            .add(Lang.text(settings.row() == 0 ? " \u27f3" : " \u27f2")
            .style(ChatFormatting.BOLD))
            .component();
    }
}