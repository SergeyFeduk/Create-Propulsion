#version 330 core

uniform vec4 uColor; 
uniform sampler2D uTex;

in vec2 vUV;
out vec4 FragColor;

void main() {
    vec4 texColor = texture(uTex, vUV);
    FragColor = texColor * uColor;
}