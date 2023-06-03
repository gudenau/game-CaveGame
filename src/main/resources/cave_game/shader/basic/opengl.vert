#version 440 core

/*
vec2 positions[3] = vec2[](
    vec2(0.0, -0.5),
    vec2(0.5, 0.5),
    vec2(-0.5, 0.5)
);

vec3 colors[3] = vec3[](
    vec3(1.0, 0.0, 0.0),
    vec3(0.0, 1.0, 0.0),
    vec3(0.0, 0.0, 1.0)
);
*/

out vec3 fragColor;

void main() {
    if(gl_VertexID == 0) {
        gl_Position = vec4(0.0, -0.5, 0.0, 1.0);
        fragColor = vec3(1.0, 0.0, 0.0);
    } else if(gl_VertexID == 1) {
        gl_Position = vec4(0.5, 0.5, 0.0, 1.0);
        fragColor = vec3(0.0, 1.0, 0.0);
    } else if(gl_VertexID == 2) {
        gl_Position = vec4(-0.5, 0.5, 0.0, 1.0);
        fragColor = vec3(0.0, 0.0, 1.0);
    }

    gl_Position *= vec4(1, -1, 1, 1);
}
