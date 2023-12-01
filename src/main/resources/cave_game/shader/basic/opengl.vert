#version 440 core

in vec3 inPosition;
in vec3 inColor;
in vec2 inTextureCoord;

out vec3 fragColor;
out vec2 fragTextureCoord;

void main() {
    gl_Position = vec4(inPosition, 1.0);
    fragColor = inColor;
    fragTextureCoord = inTextureCoord;
}
