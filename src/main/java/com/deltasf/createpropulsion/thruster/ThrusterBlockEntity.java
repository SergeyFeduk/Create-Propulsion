package com.deltasf.createpropulsion.thruster;

import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.registries.ForgeRegistries;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;

import java.awt.Color;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

import org.joml.Quaterniond;
import org.joml.Quaterniondc;
import org.joml.Quaternionf;
import org.joml.Vector3d;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;

import com.deltasf.createpropulsion.Config;
import com.deltasf.createpropulsion.CreatePropulsion;
import com.deltasf.createpropulsion.debug.DebugRenderer;
import com.simibubi.create.foundation.collision.Matrix3d;
import com.simibubi.create.foundation.collision.OrientedBB;
import com.simibubi.create.foundation.utility.CreateLang;
import com.deltasf.createpropulsion.particles.ParticleTypes;
import com.deltasf.createpropulsion.particles.PlumeParticleData;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.jesz.createdieselgenerators.fluids.FluidRegistry;
import com.drmangotea.tfmg.registry.TFMGFluids;

//Abandon all hope, ye who enter here
@SuppressWarnings({"deprecation", "unchecked"})
public class ThrusterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    private static final int OBSTRUCTION_LENGTH = 10; //Prob should be a config
    public static final int BASE_MAX_THRUST = 400000;
    public static final float BASE_FUEL_CONSUMPTION = 2;
    public static final int TICKS_PER_ENTITY_CHECK = 5;
    public static final int LOWEST_POWER_THRSHOLD = 5; 
    //Thruster data
    private ThrusterData thrusterData;
    public SmartFluidTankBehaviour tank;
    private BlockState state;
    private int emptyBlocks;
    //Ticking
    private int currentTick = 0;
    private int clientTick = 0;
    private boolean isThrustDirty = false;
    //Particles
    private ParticleType<PlumeParticleData> particleType;

    public static final TagKey<Fluid> FORGE_FUEL_TAG = TagKey.create(ForgeRegistries.FLUIDS.getRegistryKey(), new ResourceLocation("forge", "fuel")); 
    private static Dictionary<Fluid, FluidThrusterProperties> fluidsProperties = new Hashtable<Fluid, FluidThrusterProperties>();
    static {
        //Not sure where to show these in game, perhaps in item tooltip if wearing goggles/design goggles
        //Should really be json for datapacks, but I'm lazy so not yet
        if (CreatePropulsion.CDG_ACTIVE) {
            fluidsProperties.put(FluidRegistry.PLANT_OIL.get().getSource(), new FluidThrusterProperties(0.8f, 1.1f));
            fluidsProperties.put(FluidRegistry.BIODIESEL.get().getSource(), new FluidThrusterProperties(0.9f, 1f));
            fluidsProperties.put(FluidRegistry.DIESEL.get().getSource(), new FluidThrusterProperties(1.0f, 0.9f));
            fluidsProperties.put(FluidRegistry.GASOLINE.get().getSource(), new FluidThrusterProperties(1.05f, 0.95f));
            fluidsProperties.put(FluidRegistry.ETHANOL.get().getSource(), new FluidThrusterProperties(0.85f, 1.2f));
        } 
        if (CreatePropulsion.TFMG_ACTIVE) {
            fluidsProperties.put(TFMGFluids.NAPHTHA.get().getSource(), new FluidThrusterProperties(0.95f, 1.0f));
            fluidsProperties.put(TFMGFluids.KEROSENE.get().getSource(), new FluidThrusterProperties(1.0f, 0.9f));
            fluidsProperties.put(TFMGFluids.GASOLINE.get().getSource(), new FluidThrusterProperties(1.05f, 0.95f));
            fluidsProperties.put(TFMGFluids.DIESEL.get().getSource(), new FluidThrusterProperties(1.0f, 0.9f));     
        }
        if (fluidsProperties.isEmpty()) {
            fluidsProperties.put(net.minecraft.world.level.material.Fluids.LAVA, FluidThrusterProperties.DEFAULT);
        }
    };

    private static class FluidThrusterProperties {
        public float thrustMultiplier;
        public float consumptionMultiplier;
        
        public static final FluidThrusterProperties DEFAULT = new FluidThrusterProperties(1,1 );

        public FluidThrusterProperties(float thrustMultiplier, float consumptionMultiplier) {
            this.thrustMultiplier = thrustMultiplier;
            this.consumptionMultiplier = consumptionMultiplier;
        }
    }

    public FluidThrusterProperties getFuelProperties(Fluid fluid) {
        //Get properties from lookup, if not found check if they have fuel tag, otherwise this is not a fuel
        var properties = fluidsProperties.get(fluid);
        if (properties != null) return properties;
        if (fluid.is(FORGE_FUEL_TAG)) return FluidThrusterProperties.DEFAULT;
        return null;
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
        if (side == state.getValue(ThrusterBlock.FACING) && cap == ForgeCapabilities.FLUID_HANDLER) {
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
        boolean isFluidValid = validFluid();
        int power = state.getValue(ThrusterBlock.POWER);
        //Damage entities
        if (Config.THRUSTER_DAMAGE_ENTITIES.get() && isFluidValid && power > 0) doEntityDamageCheck(currentTick);
        if (!(isThrustDirty || currentTick % tick_rate == 0)) {
            return;
        }
        //Recalculate obstruction
        state = getBlockState();
        if (currentTick % (tick_rate * 2) == 0) {
            //Every second fluid tick update obstruction
            int previousEmptyBlocks = emptyBlocks;
            calculateObstruction(level, worldPosition, state.getValue(ThrusterBlock.FACING));
            if (previousEmptyBlocks != emptyBlocks) {
                setChanged();
                level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
            }
        }
        //Calculate thrust
        isThrustDirty = false;
        float thrust = 0;
        if (isFluidValid && power > 0){
            var properties = getFuelProperties(fluidStack().getRawFluid());
            float powerPercentage = power / 15.0f;
            //Redstone power clamped by obstruction value
            float obstruction = calculateObstructionEffect();
            float thrustPercentage = Math.min(powerPercentage, obstruction);
            int consumption =  obstruction > 0 ? calculateFuelConsumption(powerPercentage, properties.consumptionMultiplier, tick_rate) : 0;
            //Consume fluid
            tank.getPrimaryHandler().drain(consumption, IFluidHandler.FluidAction.EXECUTE);
            //Calculate thrust
            float thrustMultiplier = (float)(double)Config.THRUSTER_THRUST_MULTIPLIER.get();
            thrust = BASE_MAX_THRUST * thrustMultiplier * thrustPercentage * properties.thrustMultiplier;
        }
        thrusterData.setThrust(thrust);
    }

    private int calculateFuelConsumption(float powerPercentage, float fluidPropertiesConsumptionMultiplier, int tick_rate){
        float base_consumption = BASE_FUEL_CONSUMPTION * (float)(double)Config.THRUSTER_CONSUMPTION_MULTIPLIER.get();
        return (int)Math.ceil(base_consumption * powerPercentage * fluidPropertiesConsumptionMultiplier * tick_rate);
    }

    public void clientTick(Level level, BlockPos pos, BlockState state, ThrusterBlockEntity blockEntity){
        emitParticles(level, pos, state, blockEntity);
    }

    private void emitParticles(Level level, BlockPos pos, BlockState state, ThrusterBlockEntity blockEntity){
        if (blockEntity.emptyBlocks == 0) return;
        int power = state.getValue(ThrusterBlock.POWER);
        if (power == 0) return;
        if (!validFluid()) return;
        //Limit minumum velocity and particle count when power is lower than that
        clientTick++;
        if (power < LOWEST_POWER_THRSHOLD && clientTick % 2 == 0) {clientTick = 0; return; }

        float powerPercentage = Math.max(power, LOWEST_POWER_THRSHOLD) / 15.0f;
        float velocity = 4f * powerPercentage;
        float shipVelocityModifier = 0.15f;

        Direction direction = state.getValue(ThrusterBlock.FACING);
        Direction oppositeDirection = direction.getOpposite();

        double offsetFromNozzle = 0.875;
        double particleX = pos.getX() + 0.5 + oppositeDirection.getStepX() * offsetFromNozzle;
        double particleY = pos.getY() + 0.5 + oppositeDirection.getStepY() * offsetFromNozzle;
        double particleZ = pos.getZ() + 0.5 + oppositeDirection.getStepZ() * offsetFromNozzle;

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
        //Calculate obstruction if player looks at thruster with goggles. Always
        calculateObstruction(getLevel(), worldPosition, getBlockState().getValue(ThrusterBlock.FACING));

        //Thruster status
        LangBuilder status;
        if (fluidStack().isEmpty()) {
            status = CreateLang.translate("gui.goggles.thruster.status.no_fuel", new Object[0]).style(ChatFormatting.RED);
        } else if (!validFluid()) {
            status = CreateLang.translate("gui.goggles.thruster.status.wrong_fuel", new Object[0]).style(ChatFormatting.RED);
        } else if (getBlockState().getValue(ThrusterBlock.POWER) == 0) {
            status = CreateLang.translate("gui.goggles.thruster.status.not_powered", new Object[0]).style(ChatFormatting.GOLD);
        } else if (emptyBlocks == 0) {
            status = CreateLang.translate("gui.goggles.thruster.obstructed", new Object[0]).style(ChatFormatting.RED);
        } else {
            status = CreateLang.translate("gui.goggles.thruster.status.working", new Object[0]).style(ChatFormatting.GREEN);
        }
        CreateLang.translate("gui.goggles.thruster.status", new Object[0]).text(":").space().add(status).forGoggles(tooltip);

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
            CreateLang.builder().add(CreateLang.translate("gui.goggles.thruster.obstructed", new Object[0])).space()
                .add(CreateLang.text(makeObstructionBar(emptyBlocks, OBSTRUCTION_LENGTH)))
                .style(tooltipColor)
            .forGoggles(tooltip);
        }
        //Efficiency
        CreateLang.builder().add(CreateLang.translate("gui.goggles.thruster.efficiency", new Object[0])).space()
            .add(CreateLang.number(efficiency)).add(CreateLang.text("%"))
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
        var fluid = fluidStack().getRawFluid();
        return getFuelProperties(fluid) != null;
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
    
    //This is ugly af, fix it
    @SuppressWarnings("null")
    private void doEntityDamageCheck(int tick) {
        if (tick % TICKS_PER_ENTITY_CHECK != 0) return;
        int power = state.getValue(ThrusterBlock.POWER);
        float visualPowerPercent = ((float)Math.max(power, LOWEST_POWER_THRSHOLD) - LOWEST_POWER_THRSHOLD) / 15.0f;
        float distanceByPower = org.joml.Math.lerp(0.55f,1.5f, visualPowerPercent);

        Direction plumeDirection = getBlockState().getValue(ThrusterBlock.FACING).getOpposite();
        //Define OBB Dimensions
        double plumeStartOffset = 0.8;
        double plumeEndOffset = emptyBlocks * distanceByPower + plumeStartOffset;
        double plumeLength = Math.max(0, plumeEndOffset - plumeStartOffset);
        if (plumeLength <= 0.01) return;

        // Calculate OBB World Position and Orientation
        Vector3d obbCenterWorldJOML;
        Quaterniond obbRotationWorldJOML;
        Vector3d thrusterCenterBlockWorldJOML;
        double centerOffsetDistance = plumeStartOffset + (plumeLength / 2.0);
        Vector3d relativeCenterOffsetJOML = VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal()).mul(centerOffsetDistance);
        Quaterniond relativeRotationJOML = new Quaterniond().rotateTo(new Vector3d(0, 0, 1), VectorConversionsMCKt.toJOMLD(plumeDirection.getNormal()));

        Vector3d thrusterCenterBlockShipCoordsJOMLD = VectorConversionsMCKt.toJOML(Vec3.atCenterOf(worldPosition));

        Ship ship = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
        if (ship != null) {
            //Ship-space
            thrusterCenterBlockWorldJOML = ship.getShipToWorld().transformPosition(thrusterCenterBlockShipCoordsJOMLD, new Vector3d());
            obbCenterWorldJOML = ship.getShipToWorld().transformPosition(relativeCenterOffsetJOML.add(thrusterCenterBlockShipCoordsJOMLD, new Vector3d()), new Vector3d());
            obbRotationWorldJOML = ship.getTransform().getShipToWorldRotation().mul(relativeRotationJOML, new Quaterniond());
        } else {
            // World space
            thrusterCenterBlockWorldJOML = thrusterCenterBlockShipCoordsJOMLD;
            obbCenterWorldJOML = thrusterCenterBlockWorldJOML.add(relativeCenterOffsetJOML, new Vector3d());
            obbRotationWorldJOML = relativeRotationJOML;
        }

        //Calculate actuall nozzle position for distance-based damage calculation
        Vector3d nozzleOffsetLocal = new Vector3d(0, 0, 0.5); // Offset from block center to face along local Z
        Vector3d nozzleOffsetWorld = obbRotationWorldJOML.transform(nozzleOffsetLocal, new Vector3d()); // Rotate offset into world orientation
        Vector3d thrusterNozzleWorldPos = thrusterCenterBlockWorldJOML.add(nozzleOffsetWorld, new Vector3d());
        Vec3 thrusterNozzleWorldPosMC = VectorConversionsMCKt.toMinecraft(thrusterNozzleWorldPos);

        //Calculate AABB for Broad-Phase Query
        BlockPos blockBehind = worldPosition.relative(plumeDirection);
        int aabbEndOffset = (int)Math.floor(emptyBlocks * distanceByPower) + 1;
        BlockPos blockEnd = worldPosition.relative(plumeDirection, aabbEndOffset);
        AABB plumeAABB = new AABB(blockBehind).minmax(new AABB(blockEnd)).inflate(1.0); //Inflation is optional but it makes me a bit more confident

        //Calculate OBB for Narrow check
        Vector3d plumeHalfExtentsJOML = new Vector3d(0.7, 0.7, plumeLength / 2.0);
        Vec3 plumeCenterMC = VectorConversionsMCKt.toMinecraft(obbCenterWorldJOML);
        Vec3 plumeHalfExtentsMC = VectorConversionsMCKt.toMinecraft(plumeHalfExtentsJOML);
        
        Matrix3d plumeRotationMatrix = MathUtility.createMatrixFromQuaternion(obbRotationWorldJOML);
        OrientedBB plumeOBB = new OrientedBB(plumeCenterMC, plumeHalfExtentsMC, plumeRotationMatrix);

        //Debug OBB
        boolean debug = false;
        if (debug) {
            String identifier = "thruster_" + this.hashCode() + "_obb";
            Quaternionf debugRotation = new Quaternionf((float)obbRotationWorldJOML.x, (float)obbRotationWorldJOML.y, (float)obbRotationWorldJOML.z, (float)obbRotationWorldJOML.w);
            Vec3 debugSize = new Vec3(plumeHalfExtentsJOML.x * 2, plumeHalfExtentsJOML.y * 2, plumeHalfExtentsJOML.z * 2);
            Vec3 debugCenter = VectorConversionsMCKt.toMinecraft(obbCenterWorldJOML);

            DebugRenderer.drawBox(identifier, debugCenter, debugSize, debugRotation, Color.ORANGE, false, TICKS_PER_ENTITY_CHECK + 1);
        }

        //Query damage candidates
        List<Entity> damageCandidates = level.getEntities(null, plumeAABB);
        if (damageCandidates.isEmpty()) return;

        //Apply damage
        DamageSource fireDamageSource = level.damageSources().onFire();
        for (Entity entity : damageCandidates) {
            if (entity.isRemoved() || entity.fireImmune()) continue;
            AABB entityAABB = entity.getBoundingBox();
            if (plumeOBB.intersect(entityAABB) != null) {
                float invSqrDistance = visualPowerPercent * 8.0f / (float)Math.max(1, entity.position().distanceToSqr(thrusterNozzleWorldPosMC));
                float damageAmount = 3 + invSqrDistance;

                // Apply damage and fire
                entity.hurt(fireDamageSource, damageAmount);
                entity.setSecondsOnFire(3); 
            }
        }
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
