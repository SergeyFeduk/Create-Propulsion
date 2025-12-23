package com.deltasf.createpropulsion.impact_sensor;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.concurrent.atomic.AtomicReference;

@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class ImpactSensorData {
    private final AtomicReference<Double> impactSignal = new AtomicReference<>(0.0);

    public ImpactSensorData() {}

    public void setSignal(double val) {
        impactSignal.accumulateAndGet(val, Math::max);
    }

    public double getAndResetSignal() {
        return impactSignal.getAndSet(0.0);
    }
}