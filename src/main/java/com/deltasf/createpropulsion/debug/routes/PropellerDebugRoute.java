package com.deltasf.createpropulsion.debug.routes;

import com.deltasf.createpropulsion.debug.IDebugRoute;

public enum PropellerDebugRoute implements IDebugRoute {
    DAMAGE,
    SAMPLE_POINTS;
    
    private final IDebugRoute[] children;
    PropellerDebugRoute(IDebugRoute... children) { this.children = children; }
    PropellerDebugRoute() { this(new IDebugRoute[0]); }

    @Override public IDebugRoute[] getChildren() { return children; }
}
