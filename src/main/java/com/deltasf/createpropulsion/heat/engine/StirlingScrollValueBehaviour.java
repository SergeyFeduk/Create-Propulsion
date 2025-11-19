package com.deltasf.createpropulsion.heat.engine;

import com.deltasf.createpropulsion.utility.value_boxes.DualRowValueBehaviour;
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

public class StirlingScrollValueBehaviour extends DualRowValueBehaviour {
    protected static final int STEP = 64;
    protected static final int OPTIONS_PER_ROW = 3;

    public StirlingScrollValueBehaviour(Component label, SmartBlockEntity be, ValueBoxTransform slot) {
        super(label, be, slot);
        this.withFormatter(v -> Integer.toString(getUnsignedRPM()));
    }

    public int getRPM() {
        return getValue() * STEP;
    }

    public int getUnsignedRPM() {
        return Math.abs(getValue()) * STEP;
    }

    private int getRpmFromBoardIndex(int boardIndex) {
        return (boardIndex + 1) * STEP;
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> rows = ImmutableList.of(
            Component.literal("\u27f3").withStyle(ChatFormatting.BOLD),
            Component.literal("\u27f2").withStyle(ChatFormatting.BOLD)
        );
        ValueSettingsFormatter formatter = new ValueSettingsFormatter(this::formatSettings);
        return new ValueSettingsBoard(label, OPTIONS_PER_ROW, 1, rows, formatter);
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int internalValue = valueSetting.value() + 1;
        int newValue = valueSetting.row() == 0 ? -internalValue : internalValue;

        if (this.getValue() == newValue) return;

        setValue(newValue);
        playFeedbackSound(this);
    }

    @Override
    public ValueSettings getValueSettings() {
        int row = getValue() < 0 ? 0 : 1;
        int index = Math.abs(getValue()) - 1;
        return new ValueSettings(row, index);
    }

    public MutableComponent formatSettings(ValueSettings settings) {
        return Lang.number(getRpmFromBoardIndex(settings.value())).component();
    }
}