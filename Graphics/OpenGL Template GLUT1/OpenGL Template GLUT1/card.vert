#version 330

layout (location = 0) in vec4 in_Position;
layout (location = 2) in vec2 texCoord;

out vec2 tex_Coord;

uniform mat4 myMatrix;

void main(void)
{
    gl_Position = myMatrix * in_Position;
    tex_Coord = vec2(texCoord.x, 1 - texCoord.y);
}
