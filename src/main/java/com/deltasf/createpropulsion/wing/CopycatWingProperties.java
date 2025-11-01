package com.deltasf.createpropulsion.wing;

import java.util.Map;

public record CopycatWingProperties(double lift, double drag) {
    public static final Map<Integer, CopycatWingProperties> PROPERTIES_BY_WIDTH = Map.of(
        4, new CopycatWingProperties(0.0, 0.0),
        8, new CopycatWingProperties(20.0, 10.0),
        12, new CopycatWingProperties(40.0, 20.0)
    );
}