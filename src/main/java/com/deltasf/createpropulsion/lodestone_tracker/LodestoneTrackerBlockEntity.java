package com.deltasf.createpropulsion.lodestone_tracker;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import javax.annotation.Nullable;

import org.jetbrains.annotations.NotNull;
import org.joml.Quaterniondc;
import org.joml.Vector2f;
import org.joml.Vector3d;
import org.joml.Vector4i;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import com.deltasf.createpropulsion.utility.MathUtility;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;

@SuppressWarnings("null")
public class LodestoneTrackerBlockEntity extends SmartBlockEntity {

    private ItemStack compass = ItemStack.EMPTY;
    private int currentTick = 0; //TODO: Save
    private float targetAngle;
    private float previousAngle;
    private final LodestoneTrackerItemHandler itemHandler = new LodestoneTrackerItemHandler(this);
    private final LazyOptional<IItemHandler> itemHandlerCap = LazyOptional.of(() -> itemHandler);

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
        if (compass.isEmpty()) return;
        targetAngle = getAngleFromCompass(compass);
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
        }

        return angle;
    }

    private void updateRedstoneOutput(float angle) {
        //Angle of 0/360 is pointing south
        if (isAngleWithingTolerance(previousAngle, angle, 1.0f / 32.0f * 360.0f / 2.0f)) return;
        previousAngle = targetAngle;
        Vector4i outputs = calculateRedstoneOutput(angle);

        BlockState updatedState = getBlockState()
            .setValue(LodestoneTrackerBlock.POWER_NORTH, outputs.x)
            .setValue(LodestoneTrackerBlock.POWER_EAST, outputs.y)
            .setValue(LodestoneTrackerBlock.POWER_SOUTH, outputs.z)
            .setValue(LodestoneTrackerBlock.POWER_WEST, outputs.w);
        getLevel().setBlock(worldPosition, updatedState, 3);
    }

    private boolean isAngleWithingTolerance(float value, float target, float tolerance) {
        return target > value - tolerance && target < value + tolerance;
    }

    private Vector4i calculateRedstoneOutput(float angle) {
        float normalizedAngle = angle % 360.0f;
        if (normalizedAngle < 0) {
            normalizedAngle += 360.0f;
        }

        double angleRad = Math.toRadians(normalizedAngle);
        double cosA = Math.cos(angleRad);
        double sinA = Math.sin(angleRad);

        double weightN = Math.max(0, -cosA);
        double weightE = Math.max(0, -sinA);
        double weightS = Math.max(0, cosA);
        double weightW = Math.max(0, sinA);

        double sumWeights = weightN + weightE + weightS + weightW;

        if (sumWeights == 0) { // Should ideally not happen with unit vector logic, but as a fallback
            sumWeights = 1.0; // Prevent division by zero; distribute evenly or default (e.g. to North)
             // For this specific problem, sumWeights (|cosA|+|sinA|) is always >= 1.0
        }

        double[] fPowers = new double[4];
        fPowers[0] = (weightN / sumWeights) * 15.0; // North
        fPowers[1] = (weightE / sumWeights) * 15.0; // East
        fPowers[2] = (weightS / sumWeights) * 15.0; // South
        fPowers[3] = (weightW / sumWeights) * 15.0; // West

        int[] intPowers = new int[4];
        int currentSumInt = 0;

        for (int i = 0; i < 4; i++) {
            intPowers[i] = (int) Math.floor(fPowers[i]);
            currentSumInt += intPowers[i];
        }

        int remainderToDistribute = 15 - currentSumInt;

        Integer[] indices = {0, 1, 2, 3};
        Arrays.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer idx1, Integer idx2) {
                double rem1 = fPowers[idx1] - intPowers[idx1];
                double rem2 = fPowers[idx2] - intPowers[idx2];
                if (rem1 != rem2) {
                    return Double.compare(rem2, rem1); // Sort descending by remainder
                }
                return Integer.compare(idx1, idx2); // Tie-break by original index
            }
        });

        for (int k = 0; k < remainderToDistribute; k++) {
            intPowers[indices[k]]++;
        }
        return new Vector4i(intPowers[0], intPowers[1], intPowers[2], intPowers[3]);
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
	public void addBehaviours(List<BlockEntityBehaviour> behaviours) {}

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
    }

    @Override
    public void read(CompoundTag tag, boolean clientPacket) {
        super.read(tag, clientPacket);
        compass = ItemStack.of(tag.getCompound("CompassItem"));
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
