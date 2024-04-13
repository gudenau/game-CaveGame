#version 440 core

struct UniformBufferObject {
    mat4 model;
    mat4 view;
    mat4 proj;
};

layout(std140) uniform ubo {
    UniformBufferObject ubo2;
};

in vec3 inPosition;
in vec3 inColor;
in vec2 inTextureCoord;

out vec3 fragColor;
out vec2 fragTextureCoord;

void main() {
    gl_Position = ubo2.proj * ubo2.view * ubo2.model * vec4(inPosition, 1.0);
    fragColor = inColor;
    fragTextureCoord = inTextureCoord;
}
