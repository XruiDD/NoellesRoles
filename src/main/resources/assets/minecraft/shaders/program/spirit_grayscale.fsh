#version 150

uniform sampler2D DiffuseSampler;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    // ITU-R BT.709 亮度权重，标准灰度转换
    float luminance = dot(color.rgb, vec3(0.2126, 0.7152, 0.0722));
    // 略微压暗，营造灵界阴冷感
    luminance *= 0.85;
    fragColor = vec4(vec3(luminance), color.a);
}
