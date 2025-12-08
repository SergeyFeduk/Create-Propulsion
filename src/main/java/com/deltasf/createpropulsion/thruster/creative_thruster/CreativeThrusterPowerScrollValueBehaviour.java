package com.deltasf.createpropulsion.thruster.creative_thruster;

import com.google.common.collect.ImmutableList;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsBoard;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsFormatter;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;

public class CreativeThrusterPowerScrollValueBehaviour extends ScrollValueBehaviour {
    protected static final int MAX_THRUST = 1000000;
    protected static final int TOTAL_STEPS = 100;
    protected static final int FORCE_PER_STEP = MAX_THRUST / TOTAL_STEPS; 

    public float getTargetThrust() {
        return (value + 1) * FORCE_PER_STEP;
    }
 
    public CreativeThrusterPowerScrollValueBehaviour(SmartBlockEntity be) {
        super(CreateLang.builder().translate("gui.creative_thruster.power_behaviour", new Object[0]).component(), be, new CreativeThrusterValueBox());
        between(0, TOTAL_STEPS - 1); //Why is this even a thing :\
    }

    @Override
    public ValueSettingsBoard createBoard(Player player, BlockHitResult hitResult) {
        ImmutableList<Component> row = ImmutableList.of(CreateLang.builder().text("kN").component());
        return new ValueSettingsBoard(label, TOTAL_STEPS - 1, 10, row, new ValueSettingsFormatter(this::formatBoardValue));
    }

    @Override
    public void setValueSettings(Player player, ValueSettings valueSetting, boolean ctrlHeld) {
        int newValue = valueSetting.value();
        newValue = Math.max(0, Math.min(newValue, TOTAL_STEPS - 1));
        
        if (getValue() == newValue) return;

        setValue(newValue);
        playFeedbackSound(this);
    }

    @Override 
    public ValueSettings getValueSettings() {
        return new ValueSettings(0, value);
    }

    public MutableComponent formatBoardValue(ValueSettings settings) {
        int forceInNewtons = (settings.value() + 1) * FORCE_PER_STEP;
        int forceInKN = forceInNewtons / 1000;
        return CreateLang.builder().add(CreateLang.number(forceInKN)).text(" kN").component();
    }

    @Override
    public String formatValue() {
        int forceInNewtons = (value + 1) * FORCE_PER_STEP;
        int forceInKN = forceInNewtons / 1000;
        return forceInKN + " kN";
    }
}
