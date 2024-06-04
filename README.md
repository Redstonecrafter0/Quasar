# VulkanHelper

This repo is intended for me to learn Vulkan and to build a helper library around it
which I will use extensively throughout my future projects.

When this repo becomes a fully functional library it will still not be 1.0 so that
there is enough time to figure out what parts can be set to common defaults and what needs more granular control.

## GLSL Naming Convention
This library can optionally infer the vertex attribute layout from the SPIR-V or GLSL shader code.
However, this requires a special naming for input variables of the vertex shader when using instancing.
Also, the vertex buffer must be at binding location 0 and the instance buffer must be at binding location 1.
The idea is to make the vertex shader code the single source of truth for the vertex layout and therefore reducing duplicate code that comes from the explicit nature of Vulkan.
When instancing is not used, the naming convention can be ignored.
```glsl
#version 460

in vec3 vPos; // input variables from the vertex buffer are prefixed with 'v'
in vec3 iPos; // input variables from the instance buffer are prefixed with 'i'

void main() {
    gl_Position = vec4(vPos + iPos, 0);
}
```
