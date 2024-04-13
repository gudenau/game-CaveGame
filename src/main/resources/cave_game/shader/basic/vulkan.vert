#version 450

layout(binding = 0) uniform mat4 uboModel;
layout(binding = 1) uniform mat4 uboView;
layout(binding = 2) uniform mat4 uboProj;

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec3 inColor;
layout(location = 2) in vec2 inTextureCoord;

layout(location = 0) out vec3 fragColor;
layout(location = 1) out vec2 fragTextureCoord;

void main() {
    gl_Position = uboProj * uboView * uboModel * vec4(inPosition, 1.0);
    fragColor = inColor;
    fragTextureCoord = inTextureCoord;
}
