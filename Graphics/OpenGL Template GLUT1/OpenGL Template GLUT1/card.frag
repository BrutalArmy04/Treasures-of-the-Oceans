#version 330

in vec2 tex_Coord;

out vec4 out_Color;

uniform sampler2D myTexture;
uniform int highlightRow;
uniform int isWinner;

const vec4 deep = vec4(0.027, 0.165, 0.239, 1.0);
const vec4 deep2 = vec4(0.039, 0.227, 0.322, 1.0);
const vec3 teal = vec3(0.071, 0.475, 0.561);
const vec3 tealBright = vec3(0.184, 0.714, 0.769);
const vec3 gold = vec3(1.0, 0.784, 0.341);
const float rowTop[3] = float[3](0.7254, 0.8080, 0.8906);
const float rowBottom[3] = float[3](0.8080, 0.8906, 0.9732);

void main(void)
{
    vec4 texel = texture(myTexture, tex_Coord);
    if (texel.a < 0.5)
        discard;
    if (gl_FrontFacing)
    {
        out_Color = texel;
        float u = tex_Coord.x;
        float v = tex_Coord.y;
        if (highlightRow >= 0 && v >= rowTop[highlightRow] && v <= rowBottom[highlightRow])
            out_Color.rgb = mix(out_Color.rgb, teal, 0.45);
        if (isWinner == 1)
        {
            if (u < 0.03 || u > 0.97 || v < 0.022 || v > 0.978)
                out_Color.rgb = gold;
            else
                out_Color.rgb = mix(out_Color.rgb, gold, 0.12);
        }
    }
    else
    {
        float u = tex_Coord.x;
        float v = tex_Coord.y;
        if (mod(floor((u + v) * 12.0), 2.0) == 0.0)
            out_Color = deep;
        else
            out_Color = deep2;
        vec2 p = abs(vec2(u * 320.0, v * 448.0) - vec2(160.0, 224.0)) - vec2(130.5, 194.5);
        float d = length(max(p, 0.0)) + min(max(p.x, p.y), 0.0) - 16.5;
        if (abs(d) < 1.5)
            out_Color.rgb = mix(out_Color.rgb, tealBright, 0.353);
        if (isWinner == 1)
        {
            if (u < 0.04 || u > 0.96 || v < 0.03 || v > 0.97)
                out_Color.rgb = gold;
            else
                out_Color.rgb = mix(out_Color.rgb, gold, 0.25);
        }
    }
}
