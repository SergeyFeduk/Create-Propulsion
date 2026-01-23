#version 330 core

//Per-vertex
layout(location = 0) in vec2 aQuadPos; // -0.5..0.5
layout(location = 1) in vec2 aUV;

//Per-instance
layout(location = 2) in vec3 aPos; //In ship space
layout(location = 3) in vec4 aColor;
layout(location = 4) in float aScale;

uniform mat4 uProjMat;
uniform mat4 uModelViewMat;
uniform vec3 uCamRight;
uniform vec3 uCamUp;

//Ship transforms
uniform mat4 uShipRotation; 
uniform vec3 uRelativeAnchor;

out vec2 vUV;
out vec4 vColor;

void main() {
    vUV = aUV;
    vColor = aColor;
    
    //Apply transform
    vec4 rotatedPos = uShipRotation * vec4(aPos, 1.0);
    vec3 worldPosRelativeToCamera = uRelativeAnchor + rotatedPos.xyz;
    
    //Billboarding
    float size = 0.0625 * aScale; //1/16
    vec3 billboardOffset = (uCamRight * aQuadPos.x * size) + (uCamUp * aQuadPos.y * size);
    
    vec3 finalPos = worldPosRelativeToCamera + billboardOffset;
    gl_Position = uProjMat * uModelViewMat * vec4(finalPos, 1.0);
}