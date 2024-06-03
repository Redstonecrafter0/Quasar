#version 450

in vec2 aPos;
in vec3 aColor;

out vec3 fragColor;

void main() {
    gl_Position = vec4(aPos, 0.0, 1.0);
    fragColor = aColor;
}
