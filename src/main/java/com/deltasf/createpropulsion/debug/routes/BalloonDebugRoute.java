package com.deltasf.createpropulsion.debug.routes;

import com.deltasf.createpropulsion.debug.IDebugRoute;

public enum BalloonDebugRoute implements IDebugRoute {
    HAI_AABBS,
    AABB,
    VOLUME, 
    HOLES,
    FORCE_CHUNKS,
    EVENTS;
    
    private final IDebugRoute[] children;
    BalloonDebugRoute(IDebugRoute... children) { this.children = children; }
    BalloonDebugRoute() { this(new IDebugRoute[0]); }

    @Override public IDebugRoute[] getChildren() { return children; }
}
