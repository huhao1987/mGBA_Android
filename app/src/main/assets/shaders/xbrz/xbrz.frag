precision highp float;

uniform sampler2D tex;
uniform vec2 texSize;
varying vec2 texCoord;

const float coef = 2.0;
const vec3 yuv = vec3(0.2126, 0.7152, 0.0722);

float df(vec3 c1, vec3 c2) {
    vec3 diff = abs(c1 - c2);
    return dot(diff, yuv);
}

void main() {
    vec2 ps = 1.0 / texSize;

    vec3 A = texture2D(tex, texCoord + vec2(-ps.x, -ps.y)).xyz;
    vec3 B = texture2D(tex, texCoord + vec2(0.0, -ps.y)).xyz;
    vec3 C = texture2D(tex, texCoord + vec2(ps.x, -ps.y)).xyz;
    vec3 D = texture2D(tex, texCoord + vec2(-ps.x, 0.0)).xyz;
    vec3 E = texture2D(tex, texCoord).xyz;
    vec3 F = texture2D(tex, texCoord + vec2(ps.x, 0.0)).xyz;
    vec3 G = texture2D(tex, texCoord + vec2(-ps.x, ps.y)).xyz;
    vec3 H = texture2D(tex, texCoord + vec2(0.0, ps.y)).xyz;
    vec3 I = texture2D(tex, texCoord + vec2(ps.x, ps.y)).xyz;

    vec4 b = 2.0 * abs(vec4(df(D, B), df(D, H), df(F, B), df(F, H)));
    vec4 d = abs(vec4(df(A, E), df(G, E), df(C, E), df(I, E)));
    vec4 e = abs(vec4(df(D, E), df(D, E), df(F, E), df(F, E)));
    vec4 f = abs(vec4(df(B, E), df(H, E), df(B, E), df(H, E)));

    b = (b + d + e + f) / (3.0 * (b + d + e + f) + 0.5);

    vec2 f_tex = fract(texCoord * texSize);

    if (f_tex.x < 0.5 && f_tex.y < 0.5) {
        if (df(E, A) <= df(E, E) && df(E, A) <= df(E, B) && df(E, A) <= df(E, D)) {
             gl_FragColor = vec4(mix(E, A, b.x), 1.0);
             return;
        }
    }
    
    // Very simplified logic to just demonstrate a visual change (smearing/smoothing)
    // Real xBR/xBRZ is significantly larger. 
    // This serves to prove the shader pipeline works and highp is accepted.
    
    gl_FragColor = vec4(E, 1.0);
}
