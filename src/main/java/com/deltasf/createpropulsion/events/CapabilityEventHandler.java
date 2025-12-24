package com.deltasf.createpropulsion.events;

import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.heat.HeatMapper;
import com.deltasf.createpropulsion.heat.IHeatConsumer;
import com.deltasf.createpropulsion.registries.PropulsionCapabilities;
import com.simibubi.create.content.fluids.tank.FluidTankBlockEntity;
import com.simibubi.create.content.kinetics.mixer.MechanicalMixerBlockEntity;
import com.simibubi.create.content.processing.basin.BasinBlockEntity;
import com.simibubi.create.content.processing.burner.BlazeBurnerBlock.HeatLevel;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CreatePropulsion.ID)
public class CapabilityEventHandler {
    @SubscribeEvent
    public static void onAttachCapabilities(AttachCapabilitiesEvent<BlockEntity> event) {
        BlockEntity be = event.getObject();

        if (be instanceof FluidTankBlockEntity tank) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "heat_consumer"),
                new BoilerHeatConsumerProvider(tank));
        }

        if (be instanceof BasinBlockEntity basin) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(CreatePropulsion.ID, "heat_consumer"),
                new BasinHeatConsumerProvider(basin));
        }
    }

    private static class BoilerHeatConsumerProvider implements ICapabilityProvider, IHeatConsumer {
        private final FluidTankBlockEntity tank;
        private final LazyOptional<IHeatConsumer> capability = LazyOptional.of(() -> this);

        public BoilerHeatConsumerProvider(FluidTankBlockEntity tank) {
            this.tank = tank;
        }

        @Override
        public boolean isActive() {
            return tank.boiler.isActive() && tank.boiler.waterSupply > 0;
        }

        @Override
        public float getOperatingThreshold() {
            return HeatMapper.getMinHeatPercent(HeatLevel.FADING);
        }

        @Override
        public float consumeHeat(float maxAvailable, boolean simulate) {
            return isActive() ? Math.min(1.0f, maxAvailable) : 0f; 
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == PropulsionCapabilities.HEAT_CONSUMER && side == Direction.DOWN) {
                return capability.cast();
            }
            return LazyOptional.empty();
        }
    }

    private static class BasinHeatConsumerProvider implements ICapabilityProvider, IHeatConsumer {
        private final BasinBlockEntity basin;
        private final LazyOptional<IHeatConsumer> capability = LazyOptional.of(() -> this);

        public BasinHeatConsumerProvider(BasinBlockEntity basin) {
            this.basin = basin;
        }

        @SuppressWarnings("null")
        @Override
        public boolean isActive() {
            if (basin.getLevel() == null) return false; 
            BlockEntity beAbove = basin.getLevel().getBlockEntity(basin.getBlockPos().above(2));
            //We keep burner always heated as keeping heat level for heated mixers is much more important than saving fuel
            //Also the correct solution is too hard to implement and it will eat a ton of performance
            return beAbove instanceof MechanicalMixerBlockEntity;
        }

        @Override
        public float getOperatingThreshold() {
            return HeatMapper.getMinHeatPercent(HeatLevel.KINDLED);
        }

        @SuppressWarnings("null")
        @Override
        public float consumeHeat(float maxAvailable, boolean simulate) {
            BlockEntity beAbove = basin.getLevel().getBlockEntity(basin.getBlockPos().above(2));
            //Consume heat only when mixer is actually running
            if (beAbove instanceof MechanicalMixerBlockEntity mixer) {
                return mixer.running ? Math.min(1.0f, maxAvailable) : 0f; 
            }
            return 0;
        }

        @Override
        public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
            if (cap == PropulsionCapabilities.HEAT_CONSUMER && side == Direction.DOWN) {
                return capability.cast();
            }
            return LazyOptional.empty();
        }
    }
}