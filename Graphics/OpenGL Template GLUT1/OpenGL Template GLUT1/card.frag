#version 330

in vec2 tex_Coord;

out vec4 out_Color;

uniform sampler2D myTexture;

void main(void)
{
    vec4 texel = texture(myTexture, tex_Coord);
    if (texel.a < 0.5)
        discard;
    out_Color = texel;
}
