#version 460 core

layout (location = 0) in vec2 aBasePos;
layout (location = 1) in int aInstanceData;

layout (std430, binding = 1) buffer ssbo {
    vec3 chunkPos[4096];
};

layout (location = 0) out vec4 fColor;

void main() {
    vec2 basePos = aBasePos;
    int z = (aInstanceData) & 15;
    int y = (aInstanceData >> 4) & 15;
    int x = (aInstanceData >> 8) & 15;
    int face = (aInstanceData >> 12) & 7;
}
