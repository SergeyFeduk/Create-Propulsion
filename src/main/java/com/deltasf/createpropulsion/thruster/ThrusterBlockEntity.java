package com.deltasf.createpropulsion.thruster;

import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

import com.simibubi.create.content.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.LangBuilder;

import static com.deltasf.createpropulsion.thruster.ThrusterBlock.FACING;
import static com.deltasf.createpropulsion.thruster.ThrusterBlock.POWER;

import java.util.Dictionary;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import net.minecraft.network.chat.Component;

import com.deltasf.createpropulsion.Config;
import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.particles.PlumeParticleData;
import com.jesz.createdieselgenerators.fluids.FluidRegistry;

@SuppressWarnings({ "deprecation", "unchecked"})
public class ThrusterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int OBSTRUCTION_LENGTH = 10; //Prob should be a config
    private static final int BASE_MAX_THRUST = 200000; // 200 kN
    //Thruster data
    private ThrusterData thrusterData;
    public SmartFluidTankBehaviour tank;
    private BlockState state;
    private int emptyBlocks;
    //Ticking
    private int currentTick = 0;
    private boolean isThrustDirty = false;
    //Particles
    private ParticleType<PlumeParticleData> particleType;


    private static Set<Fluid> validFluids = new HashSet<Fluid>() {
        {
            //Not sure where to show these in game
            if (CreatePropulsion.CDG_ACTIVE) {
                add(FluidRegistry.PLANT_OIL.get().getSource());
                add(FluidRegistry.BIODIESEL.get().getSource());
                add(FluidRegistry.DIESEL.get().getSource());
                add(FluidRegistry.GASOLINE.get().getSource());
                add(FluidRegistry.ETHANOL.get().getSource());
            } else {
                add(net.minecraft.world.level.material.Fluids.LAVA);
            }
        }
    };
    //Yeah this is pretty ugly
    private static Dictionary<Fluid, FluidThrusterProperties> fluidsProperties = new Hashtable<Fluid, FluidThrusterProperties>();
    static {
        if (CreatePropulsion.CDG_ACTIVE) {
            fluidsProperties.put(FluidRegistry.PLANT_OIL.get().getSource(), new FluidThrusterProperties(0.8f, 1.1f));
            fluidsProperties.put(FluidRegistry.BIODIESEL.get().getSource(), new FluidThrusterProperties(0.9f, 1f));
            fluidsProperties.put(FluidRegistry.DIESEL.get().getSource(), new FluidThrusterProperties(1.0f, 0.9f));
            fluidsProperties.put(FluidRegistry.GASOLINE.get().getSource(), new FluidThrusterProperties(1.05f, 0.95f));
            fluidsProperties.put(FluidRegistry.ETHANOL.get().getSource(), new FluidThrusterProperties(0.85f, 1.2f));
        } else {
            fluidsProperties.put(net.minecraft.world.level.material.Fluids.LAVA, new FluidThrusterProperties(1,1));
        }
    }

    private static class FluidThrusterProperties {
        public float thrustMultiplier;
        public float consumptionMultiplier;

        public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier) {
            this.thrustMultiplier = thrustMultiplier;
            this.consumptionMultiplier = consumptionMultiplier;
        }
    }

    public ThrusterBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state) {
        super(CreatePropulsion.THRUSTER_BLOCK_ENTITY.get(), pos, state);
        thrusterData = new ThrusterData();
        this.state = state;
        particleType = (ParticleType<PlumeParticleData>)ParticleTypes.getPlumeType();
    }

    public ThrusterData getThrusterData() {
        return thrusterData;
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours){
        tank = SmartFluidTankBehaviour.single(this, 200);
        behaviours.add(tank);
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> cap, Direction side) {
        if (side == state.getValue(FACING) && cap == ForgeCapabilities.FLUID_HANDLER) {
            return tank.getCapability().cast();
        }
        
        return super.getCapability(cap, side);
    }

    //This ticking runs only on server. It is specified in ThrusterBlock
    @SuppressWarnings("null")
    @Override
    public void tick(){
        super.tick();
        currentTick++;
        
        int tick_rate = Config.THRUSTER_TICKS_PER_UPDATE.get();
        if (!(isThrustDirty || currentTick % tick_rate == 0)) {
            return;
        }
        state = getBlockState();
        if (currentTick % (tick_rate * 2) == 0) {
            //Every second fluid tick update obstruction
            int previousEmptyBlocks = emptyBlocks;
            calculateObstruction(level, worldPosition, state.getValue(FACING));
            if (previousEmptyBlocks != emptyBlocks) {
                setChanged();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
        isThrustDirty = false;
        float thrust = 0;
        //Has fluid and powered
        int power = state.getValue(POWER);
        if (validFluid() && power > 0){
            var properties = fluidsProperties.get(fluidStack().getRawFluid());
            float powerPercentage = power / 15.0f;
            //Redstone power clamped by obstruction value
            float obstruction = calculateObstructionEffect();
            float thrustPercentage = Math.min(powerPercentage, obstruction);
            int consumption =  obstruction > 0 ? calculateFuelConsumption(powerPercentage, properties.consumptionMultiplier, tick_rate) : 0;
            //Consume fluid
            tank.getPrimaryHandler().drain(consumption, IFluidHandler.FluidAction.EXECUTE);
            //Calculate thrust
            thrust = BASE_MAX_THRUST * Config.THRUSTER_THRUST_MULTIPLIER.get() * thrustPercentage * properties.thrustMultiplier;
        }
        thrusterData.setThrust(thrust);
    }

    private int calculateFuelConsumption(float powerPercentage, float fluidPropertiesConsumptionMultiplier, int tick_rate){
        return (int)Math.ceil(Config.THRUSTER_CONSUMPTION_MULTIPLIER.get() * powerPercentage * fluidPropertiesConsumptionMultiplier * 1.5f * tick_rate);
    }

    public void clientTick(Level level, BlockPos pos, BlockState state, ThrusterBlockEntity blockEntity){
        emitParticles(level, pos, state, blockEntity);
    }

    private void emitParticles(Level level, BlockPos pos, BlockState state, ThrusterBlockEntity blockEntity){
        if (blockEntity.emptyBlocks == 0) return;
        int power = state.getValue(POWER);
        if (power == 0) return;
        if (!validFluid()) return; 

        float powerPercentage = Math.max(power, 3) / 15.0f;
        float velocity = 4f * powerPercentage;
        float shipVelocityModifier = 0.15f;

        Direction direction = state.getValue(FACING);
        Direction oppositeDirection = direction.getOpposite();

        double particleX = pos.getX() + 0.5 + oppositeDirection.getStepX() * 0.85;
        double particleY = pos.getY() + 0.5 + oppositeDirection.getStepY() * 0.85;
        double particleZ = pos.getZ() + 0.5 + oppositeDirection.getStepZ() * 0.85;

        Vector3d baseParticleVelocity = new Vector3d(oppositeDirection.getStepX(), oppositeDirection.getStepY(), oppositeDirection.getStepZ()).mul(velocity);
        Vector3d rotatedShipVelocity = new Vector3d();
        
        ClientShip ship = VSGameUtilsKt.getShipObjectManagingPos((ClientLevel)level, pos);
        if (ship != null) {
            Quaterniondc shipRotation = ship.getRenderTransform().getShipToWorldRotation();
            Quaterniond reversedShipRotation = new Quaterniond(shipRotation).invert();
            Vector3dc shipVelocity = ship.getVelocity();
            // Rotate ship velocity by reversed ship rotation
            reversedShipRotation.transform(shipVelocity, rotatedShipVelocity);
            rotatedShipVelocity.mul(shipVelocityModifier);
            
        }
        baseParticleVelocity.add(rotatedShipVelocity);
        level.addParticle(new PlumeParticleData(particleType), true, particleX, particleY, particleZ, 
            baseParticleVelocity.x, baseParticleVelocity.y, baseParticleVelocity.z);
    }

    private float calculateObstructionEffect(){
        return (float)emptyBlocks / 10.0f;
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking){
        //Calculate obstruction if we just placed the thruster block 
        //isThrustDirty used as a obstruction calculation flag so we do not run it multiple times during the same tick
        if (currentTick == 0 && !isThrustDirty) {
            calculateObstruction(getLevel(), worldPosition, getBlockState().getValue(FACING));
        }
        //Thruster status
        LangBuilder status;
        if (fluidStack().isEmpty()) {
            status = Lang.translate("gui.goggles.thruster.status.no_fuel", new Object[0]).style(ChatFormatting.RED);
        } else if (!validFluid()) {
            status = Lang.translate("gui.goggles.thruster.status.wrong_fuel", new Object[0]).style(ChatFormatting.RED);
        } else if (getBlockState().getValue(POWER) == 0) {
            status = Lang.translate("gui.goggles.thruster.status.not_powered", new Object[0]).style(ChatFormatting.GOLD);
        } else if (emptyBlocks == 0) {
            status = Lang.translate("gui.goggles.thruster.obstructed", new Object[0]).style(ChatFormatting.RED);
        } else {
            status = Lang.translate("gui.goggles.thruster.status.working", new Object[0]).style(ChatFormatting.GREEN);
        }
        Lang.translate("gui.goggles.thruster.status", new Object[0]).text(":").space().add(status).forGoggles(tooltip);

        float efficiency = 100;
        ChatFormatting tooltipColor = ChatFormatting.GREEN;
        //Obstruction, if present
        if (emptyBlocks < OBSTRUCTION_LENGTH) {
            //Calculate efficiency
            efficiency = calculateObstructionEffect() * 100;
            if (efficiency < 10) {
                tooltipColor = ChatFormatting.RED;
            } else if (efficiency < 60) {
                tooltipColor = ChatFormatting.GOLD;
            } else if (efficiency < 100) {
                tooltipColor = ChatFormatting.YELLOW;
            } else {
                tooltipColor = ChatFormatting.GREEN;
            }
            //Add obstruction tooltip
            Lang.builder().add(Lang.translate("gui.goggles.thruster.obstructed", new Object[0])).space()
                .add(Lang.text(makeObstructionBar(emptyBlocks, OBSTRUCTION_LENGTH)))
                .style(tooltipColor)
            .forGoggles(tooltip);
        }
        //Efficiency
        Lang.builder().add(Lang.translate("gui.goggles.thruster.efficiency", new Object[0])).space()
            .add(Lang.number(efficiency)).add(Lang.text("%"))
            .style(tooltipColor)
            .forGoggles(tooltip);
        //Fluid tooltip
        containedFluidTooltip(tooltip, isPlayerSneaking, tank.getCapability().cast());
        return true;
    }

    private FluidStack fluidStack(){
        return tank.getPrimaryHandler().getFluid();
    }

    private boolean validFluid(){
        if (fluidStack().isEmpty()) return false;
        return validFluids.contains(fluidStack().getRawFluid());
    }

    public void calculateObstruction(Level level, BlockPos pos, Direction forwardDirection){
        //Starting from the block behind and iterate OBSTRUCTION_LENGTH blocks in that direction
        //Can't really use level.clip as we explicitly want to check for obstruction only in ship space
        for (emptyBlocks = 0; emptyBlocks < OBSTRUCTION_LENGTH; emptyBlocks++){
            BlockPos checkPos = pos.relative(forwardDirection.getOpposite(), emptyBlocks + 1);
            BlockState state = level.getBlockState(checkPos);
            if (!(state.isAir() || !state.isSolid())) break;
        }
        isThrustDirty = true;
    }

    @Override
    protected void write(CompoundTag compound, boolean clientPacket){
        super.write(compound, clientPacket);
        compound.putInt("emptyBlocks", emptyBlocks);
        compound.putInt("currentTick", currentTick);
        compound.putBoolean("isThrustDirty", isThrustDirty);
    }

    @Override
    protected void read(CompoundTag compound, boolean clientPacket){
        super.read(compound, clientPacket);
        emptyBlocks = compound.getInt("emptyBlocks");
        currentTick = compound.getInt("currentTick");
        isThrustDirty = compound.getBoolean("isThrustDirty");
    }

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
}
