package com.deltasf.createpropulsion.wing;

import java.util.Map;

public record CopycatWingProperties(double lift, double drag) {
    public static final Map<Integer, CopycatWingProperties> PROPERTIES_BY_WIDTH = Map.of(
        4, new CopycatWingProperties(150.0, 30.0),
        8, new CopycatWingProperties(170.0, 40.0),
        12, new CopycatWingProperties(200.0, 50.0)
    );
}