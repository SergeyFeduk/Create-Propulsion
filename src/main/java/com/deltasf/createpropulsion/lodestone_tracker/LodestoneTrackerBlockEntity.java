package com.deltasf.createpropulsion.lodestone_tracker;

import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniondc;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector4i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import com.deltasf.createpropulsion.PropulsionCompatibility;
import com.deltasf.createpropulsion.compat.computercraft.ComputerBehaviour;
import com.deltasf.createpropulsion.utility.MathUtility;
import com.simibubi.create.compat.computercraft.AbstractComputerBehaviour;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.CompassItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

@SuppressWarnings("null")
public class LodestoneTrackerBlockEntity extends SmartBlockEntity {
    //Total of 360 degrees occupied by 32 states, halved to account for angles being centered while segments are not
    private static final float ANGLE_TOLERANCE = 360.0f / 32.0f / 2.0f; 
    private final LodestoneTrackerItemHandler itemHandler = new LodestoneTrackerItemHandler(this);
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);
    //What am I doing with my life
    private ItemStack compass = ItemStack.EMPTY;
    private int currentTick = 0;
    private float targetAngle;
    private float previousAngle;
    private Vector4i redstoneOutputs = new Vector4i();
    public boolean isOutputDirty = false;
    //CC
    public AbstractComputerBehaviour computerBehaviour;

    public LodestoneTrackerBlockEntity(BlockEntityType<?> typeIn, BlockPos pos, BlockState state){
        super(typeIn, pos, state);
    }

    public float getAngle() {
        return targetAngle;
    }

    @Override
    public void tick() {
        super.tick();
        currentTick++;
        if (!compass.isEmpty()) {
            targetAngle = getAngleFromCompass(compass);
        }
        
        updateRedstoneOutput(targetAngle);
    }

    //All angular calculations are done in horizontal coordinate system due to simplicity
    public float getAngleFromCompass(ItemStack compass) {
        //Acquire target block position
        boolean targetLodestone = CompassItem.isLodestoneCompass(compass);
        GlobalPos targetBlockPosition;
        if (targetLodestone) {
            targetBlockPosition = CompassItem.getLodestonePosition(compass.getShareTag());
            if (targetBlockPosition == null) {
                //TODO: Print to check if this is correct
                //This is triggered with lodestone compass which has its lodestone block destroyed
                //In this case we just rotate the angle
                float angle = ((float)currentTick * 10.0f) % 360.0f;
                return angle;
            }
        } else {
            targetBlockPosition = CompassItem.getSpawnPosition(getLevel());
        }
        
        BlockPos targetBlock = targetBlockPosition.pos();
        //Acquire target and tracker world space positions
        Vector3d targetPosition = getWorldSpacePosition(targetBlock);
        Vector3d trackerPosition = getWorldSpacePosition(worldPosition);
        if (targetPosition == null || trackerPosition == null) return 0; // In case anything is on unloaded ship - set angle to 0 

        //Calculate angle between two positions
        float angle = getHorizontalAndVerticalAngles(targetPosition, trackerPosition).x + 180;

        //Account for tracker's ship rotation
        boolean trackerInShipyard = VSGameUtilsKt.isBlockInShipyard(level, worldPosition);
        if (trackerInShipyard) {
            Ship targetShip = VSGameUtilsKt.getShipManagingPos(level, worldPosition);
            Quaterniondc rotation = targetShip.getTransform().getShipToWorldRotation();
            Vector2f rotationHCS = MathUtility.toHorizontalCoordinateSystem(rotation);
            angle -= rotationHCS.x;
            angle = angle % 360.0f;
            if (angle < 0) angle += 360.0f;
        }

        return angle;
    }

    private void updateRedstoneOutput(float angle) {
        //Angle of 0/360 is pointing south
        if (!isOutputDirty && isAngleWithinTolerance(previousAngle, angle, ANGLE_TOLERANCE)) return;
        isOutputDirty = false;
        previousAngle = targetAngle;
        if (compass.isEmpty()) {
            redstoneOutputs.set(0, 0, 0, 0);
        } else {
            calculateRedstoneOutput(angle);
        }
        

        BlockState updatedState = getBlockState()
            .setValue(LodestoneTrackerBlock.POWER_NORTH, redstoneOutputs.x)
            .setValue(LodestoneTrackerBlock.POWER_EAST, redstoneOutputs.y)
            .setValue(LodestoneTrackerBlock.POWER_SOUTH, redstoneOutputs.z)
            .setValue(LodestoneTrackerBlock.POWER_WEST, redstoneOutputs.w);
            level.setBlock(worldPosition, updatedState, Block.UPDATE_CLIENTS | Block.UPDATE_NEIGHBORS);
    }

    private boolean isAngleWithinTolerance(float value, float target, float tolerance) {
        return target > value - tolerance && target < value + tolerance;
    }

    public void calculateRedstoneOutput(float angle) {
        float angleRad = (float)Math.toRadians(angle);
        float cosA = (float)Math.cos(angleRad);
        float sinA = (float)Math.sin(angleRad);

        float weightN = Math.max(0.0f, -cosA);
        float weightE = Math.max(0.0f, -sinA);
        float weightS = Math.max(0.0f, cosA);
        float weightW = Math.max(0.0f, sinA);

        float sumWeights = weightN + weightE + weightS + weightW;

        float fPowerN = (weightN / sumWeights) * 15.0f;
        float fPowerE = (weightE / sumWeights) * 15.0f;
        float fPowerS = (weightS / sumWeights) * 15.0f;
        float fPowerW = (weightW / sumWeights) * 15.0f;

        int intPowerN = (int) fPowerN;
        int intPowerE = (int) fPowerE;
        int intPowerS = (int) fPowerS;
        int intPowerW = (int) fPowerW;

        int currentSumInt = intPowerN + intPowerE + intPowerS + intPowerW;
        int remainderToDistribute = 15 - currentSumInt;

        if (remainderToDistribute > 0) {
            float fracN = fPowerN - intPowerN;
            float fracE = fPowerE - intPowerE;
            float fracS = fPowerS - intPowerS;
            float fracW = fPowerW - intPowerW;

            for (int k = 0; k < remainderToDistribute; k++) {
                float maxFrac = -1.0f;
                int bestDir = -1; // 0:N, 1:E, 2:S, 3:W

                if (fracN > maxFrac) { maxFrac = fracN; bestDir = 0; }
                if (fracE > maxFrac) { maxFrac = fracE; bestDir = 1; }
                if (fracS > maxFrac) { maxFrac = fracS; bestDir = 2; }
                if (fracW > maxFrac) { maxFrac = fracW; bestDir = 3; }
            
                if (bestDir == 0) {
                    intPowerN++;
                    fracN = -2.0f; // Mark as used
                } else if (bestDir == 1) {
                    intPowerE++;
                    fracE = -2.0f;
                } else if (bestDir == 2) {
                    intPowerS++;
                    fracS = -2.0f;
                } else if (bestDir == 3) {
                    intPowerW++;
                    fracW = -2.0f;
                } else {
                    break;
                }
            }
        }
        redstoneOutputs.set(intPowerN, intPowerE, intPowerS, intPowerW);
    }

    private static Vector2f getHorizontalAndVerticalAngles(Vector3d targetPosition, Vector3d trackerPosition) {
        Vector3d direction = new Vector3d();
        targetPosition.sub(trackerPosition, direction);

        if (direction.lengthSquared() == 0.0) {
            return new Vector2f(0.0f, 0.0f);
        }

        double horizontalDistance = Math.sqrt(direction.x * direction.x + direction.z * direction.z);

        float horizontalAngle;
        if (horizontalDistance == 0.0) {
            horizontalAngle = 0.0f;
        } else {
            horizontalAngle = (float) Math.toDegrees(Math.atan2(direction.x, -direction.z));
        }
        
        float verticalAngle = (float) Math.toDegrees(Math.atan2(direction.y, horizontalDistance));

        return new Vector2f(horizontalAngle, verticalAngle);
    }

    private Vector3d getWorldSpacePosition(BlockPos pos) {
        Vector3d position;
        boolean blockInShipyard = VSGameUtilsKt.isBlockInShipyard(level, pos);
        if (blockInShipyard) {
            Ship targetShip = VSGameUtilsKt.getShipManagingPos(level, pos);
            Vec3 blockCenter = pos.getCenter();
            if (targetShip == null) {
                //Ship is unloaded, return null for handling this in higher stage.
                return null;
            }
            position = VSGameUtilsKt.toWorldCoordinates(targetShip, blockCenter.x, blockCenter.y, blockCenter.z);
        } else {
            Vec3 blockCenter = pos.getCenter();
            position = new Vector3d(blockCenter.x, blockCenter.y, blockCenter.z);
        }
        return position;
    }

    @Override
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        if (PropulsionCompatibility.CC_ACTIVE) {
            behaviours.add(computerBehaviour = new ComputerBehaviour(this));
        }
    }

    //Compass methods
    public ItemStack getCompass() {
        return compass;
    }

    public boolean hasCompass() {
        return !compass.isEmpty();
    }

    public void setCompass(ItemStack item) {
        if (item.isEmpty() || item.getItem() == Items.COMPASS) {
            ItemStack oldCompass = this.compass;
            this.compass = item.copy();
            if (this.compass.getCount() > 1) {
                this.compass.setCount(1); // Enforce max stack size of 1
            }
            if (!ItemStack.matches(oldCompass, this.compass)) {
                setChanged();
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    public ItemStack removeCompass() {
        ItemStack extracted = this.compass.copy();
        setCompass(ItemStack.EMPTY);
        return extracted;
    }

    //Capability
    @NotNull
    @Override
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return itemHandlerCap.cast();
        }
        if (PropulsionCompatibility.CC_ACTIVE && computerBehaviour.isPeripheralCap(cap)) {
            return computerBehaviour.getPeripheralCapability();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        itemHandlerCap.invalidate();
    }

    //NBT
     @Override
    protected void write(CompoundTag tag, boolean clientPacket) {
        super.write(tag, clientPacket);
        tag.put("CompassItem", compass.save(new CompoundTag()));
        tag.putInt("currentTick", currentTick);
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        compass = ItemStack.of(tag.getCompound("CompassItem"));
        currentTick = tag.getInt("currentTick");
    }

    //Sync
    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    public void notifyUpdate() {
        setChanged();
        if (level != null && !level.isClientSide) {
             level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }
}
