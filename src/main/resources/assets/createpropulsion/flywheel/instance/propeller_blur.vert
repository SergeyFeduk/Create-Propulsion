#include "flywheel:util/quaternion.glsl"

void flw_instanceVertex(in FlwInstance instance) {
    vec3 centeredPos = flw_vertexPos.xyz - 0.5;
    vec3 rotated = rotateByQuaternion(centeredPos, instance.rotation);
    flw_vertexPos.xyz = rotated + instance.pos + 0.5;
    flw_vertexNormal = rotateByQuaternion(flw_vertexNormal, instance.rotation);
    flw_vertexColor *= instance.color;
    flw_vertexLight = max(vec2(instance.light) / 256.0, flw_vertexLight);
    flw_vertexOverlay = instance.overlay;
}