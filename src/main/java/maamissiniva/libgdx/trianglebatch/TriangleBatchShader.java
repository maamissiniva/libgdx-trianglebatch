package maamissiniva.libgdx.trianglebatch;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class TriangleBatchShader {

    public static String text(String... lines) {
        StringBuilder sb = new StringBuilder();
        for (String line : lines)
            sb.append(line).append('\n');
        return sb.toString();
    }

    public static final String vertexShader = text("" 
            , "attribute vec4 " + ShaderProgram.POSITION_ATTRIBUTE + ";"
            // format is ABGR
            , "attribute vec4 " + TriangleBatch.CONTROL_ATTRIBUTE + ";"
            , "attribute vec4 " + ShaderProgram.COLOR_ATTRIBUTE + ";"
            , "attribute vec2 " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;"
            , "uniform mat4 u_projTrans;"
            , "varying vec4 v_color;"
            , "varying vec2 v_texCoords;"
            , "varying vec4 v_ctrl;"
            , ""
            , "void main()"
            , "{"
            , "   v_color      = " + ShaderProgram.COLOR_ATTRIBUTE + ";"
            , "   v_color.a    = v_color.a * (255.0/254.0);"
            , "   v_texCoords  = " + ShaderProgram.TEXCOORD_ATTRIBUTE + "0;"
            , "   v_ctrl       = " + TriangleBatch.CONTROL_ATTRIBUTE + ";"
            , "   gl_Position  = u_projTrans * " + ShaderProgram.POSITION_ATTRIBUTE + ";"
            , "}"
        );
    
    public static String fragmentShader = text(""
            , "#ifdef GL_ES"
            , "#define LOWP lowp"
            , "precision mediump float;"
            , "#else"
            , "#define LOWP"
            , "#endif"
            , "varying LOWP vec4 v_color;"
            , "varying vec2 v_texCoords;"
            , "varying vec4 v_ctrl;"
            // Emulates a texture array which could be used instead if OpenGL supports it.
            , "uniform sampler2D u_texture_0;"
            , "uniform sampler2D u_texture_1;"
            , "uniform sampler2D u_texture_2;"
            , "uniform sampler2D u_texture_3;"
            , "uniform sampler2D u_texture_4;"
            , "uniform sampler2D u_texture_5;"
            , "uniform sampler2D u_texture_6;"
            , "uniform sampler2D u_texture_7;"
            , ""
            // constant 3 tests instead of varying 1-8
            , "vec4 getTexColor(int tidx, vec2 uv) {"
            , "  if (tidx < 4) {"
            , "     if (tidx < 2) {"
            , "       if (tidx == 0) {" 
            , "         return texture2D(u_texture_0, v_texCoords);"
            , "       } else {"
            , "         return texture2D(u_texture_1, v_texCoords);"
            , "       }"
            , "     } else {"
            , "       if (tidx == 2) {"
            , "         return texture2D(u_texture_2, v_texCoords);"
            , "       } else {"
            , "         return texture2D(u_texture_3, v_texCoords);"
            , "       }"
            , "     }"
            , "  } else {"
            , "    if (tidx < 6) {"
            , "       if (tidx == 4) {"
            , "          return texture2D(u_texture_4, v_texCoords);"
            , "       } else {"
            , "          return texture2D(u_texture_5, v_texCoords);"
            , "       }"
            , "    } else {"
            , "       if (tidx == 6) {"
            , "         return texture2D(u_texture_6, v_texCoords);"
            , "       } else {"
            , "         return texture2D(u_texture_7, v_texCoords);"
            , "       }"
            , "    }"
            , "  }"
            , "}"
            , ""
            , "const float spread    = 4.0;" // distance field spread
            , ""
            , "void main()"
            , "{"
            , "  int v_ctrl_draw = int(v_ctrl.a * (255.0 * 255.0 / 254.0)) / 2;"
            // -- draw colored
            , "  if (v_ctrl_draw == 0) {"
            , "    gl_FragColor = v_color;"
            // -- draw textured colored
            , "  } else if (v_ctrl_draw == 1) {"
            , "    int  v_ctrl_tex = int(v_ctrl.r * 255.0);"
            , "    vec4 tcolor     = getTexColor(v_ctrl_tex, v_texCoords) * v_color;"
            , "    gl_FragColor = v_color * tcolor;"
            // -- draw distance field
            , "  } else if (v_ctrl_draw == 2) {"
            , "    int   v_ctrl_tex   = int(v_ctrl.r * 255.0);"
            , "    float v_ctrl_scale = (v_ctrl.g * 255.0 + 1.0) / 16.0;"
            , "    float smoothing    = 1.0 / (4.0 * spread * v_ctrl_scale);"
            , "    float distance     = getTexColor(v_ctrl_tex, v_texCoords).a;"
            , "    float alpha        = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);"
            , "    gl_FragColor       = vec4(v_color.rgb, v_color.a * alpha);"
            // -- draw distance field outline 
            , "  } else if (v_ctrl_draw == 3) {"
            , "    int v_ctrl_tex      = int(v_ctrl.r * 255.0);"
            , "    float distance      = getTexColor(v_ctrl_tex, v_texCoords).a;"
            , "    float v_ctrl_scale  = (v_ctrl.g * 255.0 + 1.0)/ 16.0;"
            , "    v_ctrl_scale  = 1.0;"
            , "    float smoothing     = 1.0 / (4.0 * spread * v_ctrl_scale);"
            , "    float outline_width = v_ctrl.b / 5.0;"
            , "    float degde = 0.5 - abs(distance - 0.5);" 
            , "    float alpha = smoothstep(0.5 - outline_width - smoothing, 0.5 - outline_width + smoothing, degde);"
            , "    gl_FragColor = vec4(v_color.rgb, v_color.a * alpha);"
            // -- draw v controlled distance AA
            , "  } else if (v_ctrl_draw == 4) {"
            , "    float v_ctrl_scale = 1.0;"
            , "    float smoothing    = 1.0 / (4.0 * spread * v_ctrl_scale);"
            , "    float distance     = v_texCoords.y;"
            , "    float alpha        = smoothstep(0.5 - smoothing, 0.5 + smoothing, distance);"
            , "    gl_FragColor = vec4(v_color.rgb, v_color.a * alpha);"
            , "  } else {"
            , "    gl_FragColor = vec4(1.0, 0.0, 1.0, 1.0);"
            , "  }"
            , "}"
            );
    
    /**
     * Build shader. 
     * @return shader instance
     */
    static public ShaderProgram createShader () {
        ShaderProgram shader = new ShaderProgram(vertexShader, fragmentShader);
        if (!shader.isCompiled())
            throw new IllegalArgumentException("Error compiling shader: " + shader.getLog());
        return shader;
    }

}
