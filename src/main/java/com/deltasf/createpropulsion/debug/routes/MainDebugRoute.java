package com.deltasf.createpropulsion.debug.routes;

import com.deltasf.createpropulsion.debug.IDebugRoute;

public enum MainDebugRoute implements IDebugRoute {
    THRUSTER,
    MAGNET,                                  
    BALLOON(BalloonDebugRoute.values());

    private final IDebugRoute[] children;
    MainDebugRoute(IDebugRoute... children) { this.children = children; }
    MainDebugRoute() { this(new IDebugRoute[0]); }

    @Override public IDebugRoute[] getChildren() { return children; }
}
