package com.deltasf.createpropulsion.heat.engine;

import java.util.ArrayList;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector4f;
import org.joml.Vector3f;

import com.deltasf.createpropulsion.PropulsionConfig;
import com.deltasf.createpropulsion.registries.PropulsionPartialModels;
import com.deltasf.createpropulsion.utility.math.MathUtility;
import com.jozufozu.flywheel.api.MaterialManager;
import com.jozufozu.flywheel.api.instance.DynamicInstance;
import com.jozufozu.flywheel.core.Materials;
import com.jozufozu.flywheel.core.materials.oriented.OrientedData;
import com.mojang.math.Axis;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityInstance;
import com.simibubi.create.content.kinetics.base.flwdata.RotatingData;
import com.simibubi.create.foundation.utility.AnimationTickHolder;

import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.valkyrienskies.mod.common.util.VectorConversionsMCKt;

public class StirlingEngineRenderInstance extends KineticBlockEntityInstance<StirlingEngineBlockEntity> implements DynamicInstance {
    protected final RotatingData shaft;
    protected final List<OrientedData> pistons = new ArrayList<>(4);
    private final static int[] offsetArray = {0, 7, 2, 9};

    public StirlingEngineRenderInstance(MaterialManager materialManager, StirlingEngineBlockEntity blockEntity) {
        super(materialManager, blockEntity);

        //Setuop shaft
        Direction direction = blockState.getValue(StirlingEngineBlock.FACING);
        shaft = getRotatingMaterial().getModel(AllPartialModels.SHAFT_HALF, blockState, direction).createInstance();
        setup(shaft);

        //Setup pistons
        var pistonModel = materialManager.defaultCutout().material(Materials.ORIENTED).getModel(PropulsionPartialModels.STIRLING_ENGINE_PISTON, blockState);

        for (int i = 0; i < 4; i++) {
            OrientedData data = pistonModel.createInstance();
            relight(pos, data);
            pistons.add(data);
        }
    }

    @Override
    public void beginFrame() {
        updateRotation(shaft);

        float time = AnimationTickHolder.getRenderTime(blockEntity.getLevel());
        float timeSeconds = time / 20.0f;
        float speed = blockEntity.getSpeed() / StirlingEngineBlockEntity.MAX_GENERATED_RPM;

        float effectiveRevolutionPeriod = Float.MAX_VALUE;
        if (Math.abs(speed) > MathUtility.epsilon) {
            double revolutionPeriod = PropulsionConfig.STIRLING_REVOLUTION_PERIOD.get();
            effectiveRevolutionPeriod = (float) revolutionPeriod / speed;
        }

        double crankRadius = PropulsionConfig.STIRLING_CRANK_RADIUS.get();
        double conrodLength = PropulsionConfig.STIRLING_CONROD_LENGTH.get();

        Vector4f normalizedExtensions = StirlingEngineRenderer.calculateExtensions(timeSeconds, (float) crankRadius, (float) conrodLength, effectiveRevolutionPeriod);
        Direction direction = blockState.getValue(StirlingEngineBlock.FACING);
        final float offsetDistance = 2 / 16.0f;

        //Update pistons
        for (int i = 0; i < 4; i++) {
            float normalized;
            if (i == 0) normalized = normalizedExtensions.x;
            else if (i == 1) normalized = normalizedExtensions.y;
            else if (i == 2) normalized = normalizedExtensions.z;
            else normalized = normalizedExtensions.w;
            float offset = Math.min(offsetDistance - 0.001f, normalized * offsetDistance);
            transformPiston(pistons.get(i), i, direction, offset);
        }
    }

    private void transformPiston(OrientedData instance, int index, Direction facing, float extensionOffset) {
        Quaternionf rotation = new Quaternionf();
        rotation.mul(facing.getRotation());
        
        if (index >= 2) {
            rotation.mul(Axis.ZP.rotationDegrees(180));
        }
        rotation.mul(Axis.XP.rotationDegrees(270));

        Vector3f localTranslation = new Vector3f(extensionOffset, 0, offsetArray[index] / 16.0f);
        Vector3f worldOffset = new Vector3f(localTranslation).rotate(rotation);
        Vector3f instancePos = VectorConversionsMCKt.toJOMLF((Vec3i) getInstancePosition());
        instancePos.add(worldOffset);

        instance.setPosition(instancePos).setRotation(rotation);
    }

    @Override
    public void updateLight() {
        super.updateLight();
        relight(pos, shaft);
        for (OrientedData piston : pistons) {
            relight(pos, piston);
        }
    }

    @Override
    public void remove() {
        shaft.delete();
        for (OrientedData piston : pistons) {
            piston.delete();
        }
        pistons.clear();
    }
}
