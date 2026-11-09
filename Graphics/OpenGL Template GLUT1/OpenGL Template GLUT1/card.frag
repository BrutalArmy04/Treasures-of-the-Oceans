#version 330

in vec2 tex_Coord;

out vec4 out_Color;

uniform sampler2D myTexture;

const vec4 deep = vec4(0.027, 0.165, 0.239, 1.0);
const vec4 deep2 = vec4(0.039, 0.227, 0.322, 1.0);

void main(void)
{
    vec4 texel = texture(myTexture, tex_Coord);
    if (texel.a < 0.5)
        discard;
    if (gl_FrontFacing)
        out_Color = texel;
    else
    {
        float u = tex_Coord.x;
        float v = tex_Coord.y;
        if (mod(floor((u + v) * 12.0), 2.0) == 0.0)
            out_Color = deep;
        else
            out_Color = deep2;
    }
}
