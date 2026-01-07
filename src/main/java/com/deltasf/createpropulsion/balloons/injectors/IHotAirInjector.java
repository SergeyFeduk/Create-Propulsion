package com.deltasf.createpropulsion.balloons.injectors;

import java.util.UUID;

public interface IHotAirInjector {
    public UUID getId();
    public double getInjectionAmount();
    void attemptScan();
    default void onBalloonLoaded() {}
}
