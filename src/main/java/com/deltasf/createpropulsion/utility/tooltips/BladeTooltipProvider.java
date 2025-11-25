package com.deltasf.createpropulsion.utility.tooltips;

import java.util.List;

import com.deltasf.createpropulsion.propeller.blades.PropellerBladeItem;
import com.deltasf.createpropulsion.registries.PropulsionItems;
import com.simibubi.create.content.equipment.goggles.GogglesItem;
import com.simibubi.create.content.kinetics.base.IRotate.StressImpact;
import com.simibubi.create.foundation.item.TooltipHelper;
import com.simibubi.create.foundation.utility.CreateLang;
import net.createmod.catnip.lang.FontHelper.Palette;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class BladeTooltipProvider implements ITooltipProvider {
    private static final float epsilon = 1e-4f;

    @Override
    public void addTooltip(ItemTooltipEvent event, List<Component> tooltipList) {
        ItemStack stack = event.getItemStack();
        if (!stack.is(PropulsionItems.PROPELLER_BLADE_TAG)) return;

        if (!(stack.getItem() instanceof PropellerBladeItem bladeItem)) return;

        if (event.getEntity() == null) return;

        boolean isWearingGoggles = GogglesItem.isWearingGoggles(event.getEntity());

        TooltipHandler.wrapShiftHoldText(tooltipList, "createpropulsion.tooltip.holdForBladeSummary", () -> {
            //Air efficiency
            float airEfficiencyPercent = Math.round(bladeItem.getAirEfficiency() * 100.0f);
            Component airEfficiencyLine = Component.translatable("createpropulsion.tooltip.airEfficiency")
                .append(": ")
                .withStyle(Palette.STANDARD_CREATE.primary())
                .append(Component.literal(String.valueOf(airEfficiencyPercent)).withStyle(Palette.STANDARD_CREATE.highlight()))
                .append(Component.literal("%").withStyle(Palette.STANDARD_CREATE.primary()));
            tooltipList.add(airEfficiencyLine);
            //Water efficiency
            float waterEfficiency = Math.round(bladeItem.getFluidEfficiency() * 100.0f);
            Component waterEfficiencyLine = Component.translatable("createpropulsion.tooltip.waterEfficiency")
                .append(": ")
                .withStyle(Palette.STANDARD_CREATE.primary())
                .append(Component.literal(String.valueOf(waterEfficiency)).withStyle(Palette.STANDARD_CREATE.highlight()))
                .append(Component.literal("%").withStyle(Palette.STANDARD_CREATE.primary()));
            tooltipList.add(waterEfficiencyLine);
            //Stress impact
            float stressImpact = bladeItem.getStressImpact();
            StressImpact impact = mapStressImpact(stressImpact);

            Component kineticStressLine = CreateLang.translate("tooltip.stressImpact").style(ChatFormatting.GRAY).component();

            MutableComponent stressImpactLine = Component.literal(TooltipHelper.makeProgressBar(3, impact.ordinal() + 1))
                .withStyle(impact.getAbsoluteColor());

            if (isWearingGoggles) {
                LangBuilder rpmUnit = CreateLang.translate("generic.unit.rpm");
                stressImpactLine.append(CreateLang.number(stressImpact).component())
                    .append(Component.literal("x "))
                    .append(rpmUnit.component());
            } else {
                stressImpactLine.append(CreateLang.translate("tooltip.stressImpact." + CreateLang.asId(impact.name())).
                    style(impact.getAbsoluteColor())
                    .component());
            }

            tooltipList.add(kineticStressLine);
            tooltipList.add(stressImpactLine);
            //Optional: if does not produce parasitic torque 
            float parasiticTorque = bladeItem.getTorqueFactor();
            if (parasiticTorque <= epsilon) {
                Component noTorqueLine = Component.translatable("createpropulsion.tooltip.noTorque").withStyle(ChatFormatting.GREEN);
                tooltipList.add(noTorqueLine);
            }
        });
    }

    private static StressImpact mapStressImpact(float stressImpact) {
        if (stressImpact > 10.0f) {
            return StressImpact.HIGH;
        } else if (stressImpact > 4.0f) {
            return StressImpact.MEDIUM;
        }
        return StressImpact.LOW;
    }
}
