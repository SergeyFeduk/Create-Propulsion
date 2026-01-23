#version 330 core

in vec2 vUV;
in vec4 vColor;
uniform sampler2D uTex;

out vec4 FragColor;

void main() {
    vec4 texColor = texture(uTex, vUV);
    FragColor = texColor * vColor;
}