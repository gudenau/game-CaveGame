#version 440 core

uniform sampler2D texSampler;

in vec3 fragColor;
in vec2 fragTextureCoord;

out vec4 outColor;

void main() {
    outColor = texture(texSampler, fragTextureCoord) * vec4(fragColor, 1.0);
}
