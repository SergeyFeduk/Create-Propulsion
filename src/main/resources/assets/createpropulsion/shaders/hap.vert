#version 330 core

//Per-vertex
layout(location = 0) in vec2 aQuadPos; //-0.5..0.5
layout(location = 1) in vec2 aUV;

//Per-instance
layout(location = 2) in float inPx;
layout(location = 3) in float inPy;
layout(location = 4) in float inPz;
layout(location = 5) in float inCx;
layout(location = 6) in float inCy;
layout(location = 7) in float inCz;
layout(location = 8) in float inLife;
layout(location = 9) in float inScale;

uniform mat4 uProjMat;
uniform mat4 uModelViewMat;
uniform vec3 uCamRight;
uniform vec3 uCamUp;

//Ship transforms
uniform mat4 uShipRotation; 
uniform vec3 uRelativeAnchor;

//Interpolation
uniform float uPartialTick;

out vec2 vUV;

void main() {
    vUV = aUV;

    float x = mix(inPx, inCx, uPartialTick);
    float y = mix(inPy, inCy, uPartialTick);
    float z = mix(inPz, inCz, uPartialTick);
    vec3 localPos = vec3(x, y, z);

    float lifeFactor = min(inLife / 0.2, 1.0); 
    float finalScale = inScale * lifeFactor;

    //Apply transform
    vec4 rotatedPos = uShipRotation * vec4(localPos, 1.0);
    vec3 worldPosRelativeToCamera = uRelativeAnchor + rotatedPos.xyz;

    //Billboarding
    float size = 0.0625 * finalScale; //1/16
    vec3 billboardOffset = (uCamRight * aQuadPos.x * size) + (uCamUp * aQuadPos.y * size);

    vec3 finalPos = worldPosRelativeToCamera + billboardOffset;
    gl_Position = uProjMat * uModelViewMat * vec4(finalPos, 1.0);
}