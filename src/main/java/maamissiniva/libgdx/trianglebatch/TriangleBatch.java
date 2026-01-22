package maamissiniva.libgdx.trianglebatch;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Mesh;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.VertexAttribute;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.math.Matrix4;

/**
 */
public class TriangleBatch {
    
    public enum DrawAlgorithm {

        ColoredNoTexture     (0),
        ColoredTexture       (1),
        DistanceField        (2),
        DistanceFieldOutline (3),
        DistanceFieldV       (4);
        
        public int value;
        
        private DrawAlgorithm(int value) {
            this.value = value;
        }
        
    }

    /**
     * Good enough number.
     */
    public static int MAX_TEXTURES = 8;
    
    /**
     * Shader program algorithm control attribute.
     */
    static final String CONTROL_ATTRIBUTE = "a_control"; 

    static final int ATT_X     = 0;
    static final int ATT_Y     = 1;
    static final int ATT_CTRL  = 2;
    static final int ATT_COLOR = 3;
    static final int ATT_U     = 4;
    static final int ATT_V     = 5;
    
    private static final int VERTEX_SIZE = 
            1   // x
            + 1 // y
            + 1 // control
            + 1 // color
            + 1 // u
            + 1 // v
            ;
    /**
     * Array of textures to bind. Texture arrays may not exist in OpenGL
     * so we use some shader texture selection from {@link #MAX_TEXTURES} textures. 
     */
    private Texture[] textures;

    /**
     * Currently used texture slots.
     */
    private int textureCount;
    
    /**
     * Shader.
     */
    private ShaderProgram program;
    
    /**
     * Vertex drawing GDX abstraction.
     */
    private Mesh mesh;

    /**
     * Vertex data that will be draw using the {@link #mesh}. 
     */
    private float[] vertexData;
    
    /**
     * Currently allocated triangles.
     */
    private int triangleCount;

    /**
     * Maximum number of triangles, same as
     * <p>
     * {@code vertexData.length / (3 * vertexSize) } 
     */
    private int maxTriangles;
    
    private final Matrix4 transformMatrix;
    private final Matrix4 projectionMatrix;
    private final Matrix4 combinedMatrix;
    
    public TriangleBatch(int size) {
        maxTriangles     = size; 
        vertexData       = new float[size * VERTEX_SIZE * 3];
        triangleCount    = 0;
        textures         = new Texture[MAX_TEXTURES];
        textureCount     = 0;
        transformMatrix  = new Matrix4();
        projectionMatrix = new Matrix4();
        combinedMatrix   = new Matrix4();
        program          = TriangleBatchShader.createShader();
        
        // This should be configured as either "natural" or fixed.
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        
        mesh = new Mesh(false, size * 3, 0,
                new VertexAttribute(Usage.Position,           2, ShaderProgram.POSITION_ATTRIBUTE),
                new VertexAttribute(Usage.ColorPacked,        4, CONTROL_ATTRIBUTE),
                new VertexAttribute(Usage.ColorPacked,        4, ShaderProgram.COLOR_ATTRIBUTE),
                new VertexAttribute(Usage.TextureCoordinates, 2, ShaderProgram.TEXCOORD_ATTRIBUTE + "0"));
    }
    
    public void flush() {
        mesh.setVertices(vertexData, 0, triangleCount * 3 * VERTEX_SIZE);
        
        Gdx.gl.glEnable(GL20.GL_BLEND); 
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        // Gdx.gl.glBlendFuncSeparate(blendSrcFunc, blendDstFunc, blendSrcFuncAlpha, blendDstFuncAlpha);
        // bind textures
        for (int i=0; i<textureCount; i++)
            textures[i].bind(i);
        mesh.render(program, GL20.GL_TRIANGLES, 0, triangleCount * 3);
        // reset data
        triangleCount = 0;
        textureCount  = 0;
    }

    // -------------------------------------------------
    // Raw
    
    /**
     * Full vertex information.
     * @param vertexData array to put data in
     * @param idx        index to put data at 
     * @param x          vertex x
     * @param y          vertex y
     * @param ctrl       control
     * @param color      vertex color
     * @param u          vertex u
     * @param v          vertex v
     */
    public static void vertex(float[] vertexData, int idx, float x, float y, float ctrl, float color, float u, float v) {
        vertexData[idx + ATT_X    ] = x;
        vertexData[idx + ATT_Y    ] = y;
        vertexData[idx + ATT_CTRL ] = ctrl;
        vertexData[idx + ATT_COLOR] = color;
        vertexData[idx + ATT_U    ] = u;
        vertexData[idx + ATT_V    ] = v;        
    }

    /**
     * Partial vertex information, no texture coordinates.
     * @param vertexData array to put data in
     * @param idx        index to put data at
     * @param x          vertex x
     * @param y          vertex y
     * @param ctrl       control
     * @param color      vertex color
     */
    public static void vertex(float[] vertexData, int idx, float x, float y, float ctrl, float color) {
        vertexData[idx + ATT_X    ] = x;
        vertexData[idx + ATT_Y    ] = y;
        vertexData[idx + ATT_CTRL ] = ctrl;
        vertexData[idx + ATT_COLOR] = color;
    }
    
    /**
     * Raw triangle draw, use specialized versions instead.
     * @param ctrl algorithm control
     * @param x0   first vertex x
     * @param y0   first vertex x
     * @param c0   first vertex color
     * @param u0   first vertex u
     * @param v0   first vertex v
     * @param x1   second vertex x
     * @param y1   second vertex y
     * @param c1   second vertex color
     * @param u1   second vertex u
     * @param v1   second vertex v
     * @param x2   third vertex x
     * @param y2   third vertex y
     * @param c2   third vertex color
     * @param u2   third vertex u
     * @param v2   third vertex v
     */
    public void drawTriangle(
            float ctrl,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2) {
        if (triangleCount == maxTriangles)
            flush();
        int   idx   = triangleCount * VERTEX_SIZE * 3;
        vertex(vertexData, idx, x0, y0, ctrl, c0, u0, v0);
        idx += VERTEX_SIZE;
        vertex(vertexData, idx, x1, y1, ctrl, c1, u1, v1);
        idx += VERTEX_SIZE;
        vertex(vertexData, idx, x2, y2, ctrl, c2, u2, v2);
        triangleCount += 1;
    }

    /**
     * Raw triangle draw, use specialized versions instead.
     * @param ctrl algorithm control
     * @param x0   first vertex x
     * @param y0   first vertex x
     * @param c0   first vertex color
     * @param x1   second vertex x
     * @param y1   second vertex y
     * @param c1   second vertex color
     * @param x2   third vertex x
     * @param y2   third vertex y
     * @param c2   third vertex color
     */
    public void drawTriangle(
            float ctrl,
            float x0, float y0, float c0, 
            float x1, float y1, float c1,
            float x2, float y2, float c2) {
        if (triangleCount == maxTriangles)
            flush();
        int   idx   = triangleCount * VERTEX_SIZE * 3;
        vertex(vertexData, idx, x0, y0, ctrl, c0);
        idx += VERTEX_SIZE;
        vertex(vertexData, idx, x1, y1, ctrl, c1);
        idx += VERTEX_SIZE;
        vertex(vertexData, idx, x2, y2, ctrl, c2);
        triangleCount += 1;
    }

    // -------------------------------------------------------------
    // ColoredNoTexture

    /**
     * Colored triangle, does not use any texture.
     * @param x0 first vertex x
     * @param y0 first vertex y
     * @param c0 first vertex color
     * @param x1 second vertex x
     * @param y1 second vertex y
     * @param c1 second vertex color
     * @param x2 third vertex x
     * @param y2 third vertex y
     * @param c2 third vertex color
     */
    public void drawColoredTriangle(
            float x0, float y0, Color c0,
            float x1, float y1, Color c1,
            float x2, float y2, Color c2) {
        drawTriangle(
                ctrlFloat(DrawAlgorithm.ColoredNoTexture),
                x0, y0, c0.toFloatBits(),
                x1, y1, c1.toFloatBits(),
                x2, y2, c2.toFloatBits()
                );
    }
    
    /**
     * Colored quad, does not use any texture.
     * @param x0 first vertex x
     * @param y0 first vertex y
     * @param c0 first vertex color
     * @param x1 second vertex x
     * @param y1 second vertex y
     * @param c1 second vertex color
     * @param x2 third vertex x
     * @param y2 third vertex y
     * @param c2 third vertex color
     * @param x3 fourth vertex x
     * @param y3 fourth vertex y
     * @param c3 fourth vertex color
     */
    public void drawColoredQuad(
            float x0, float y0, Color c0,
            float x1, float y1, Color c1, 
            float x2, float y2, Color c2, 
            float x3, float y3, Color c3) {
        float ctrl = ctrlFloat(DrawAlgorithm.ColoredNoTexture);
        float fc0 = c0.toFloatBits();
        float fc1 = c1.toFloatBits();
        float fc2 = c2.toFloatBits();
        float fc3 = c3.toFloatBits();
        drawTriangle(
                ctrl,
                x0, y0, fc0,
                x1, y1, fc1,
                x2, y2, fc2);
        drawTriangle(
                ctrl,
                x2, y2, fc2,
                x3, y3, fc3,
                x0, y0, fc0);
    }

    // ----------------------------------------------------------------------------
    // ColoredTexure
    
    public void drawTexturedTriangle(
            Texture texture,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2) {
        int   tid  = prepareTriangleRendering(texture);
        float ctrl = ctrlFloat(DrawAlgorithm.ColoredTexture, tid);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
    }

    public void drawTexturedQuad(
            Texture texture,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2,
            float x3, float y3, float c3, float u3, float v3) {
        int   tid  = prepareTriangleRendering(texture);
        float ctrl = ctrlFloat(DrawAlgorithm.ColoredTexture, tid);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
        drawTriangle(ctrl,
                x2, y2, c2, u2, v2,
                x3, y3, c3, u3, v3,
                x0, y0, c0, u0, v0);
    }

    // ----------------------------------------------------
    // DistanceField
    
    public void drawDistanceFieldTriangle(
            Texture texture,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2) {
        int tid = prepareTriangleRendering(texture);
        float ctrl = ctrlFloat(DrawAlgorithm.DistanceField, tid);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
    }

    // map [16, 1/16] -> [0,1]
    static float sixteenth(float value) {
        float v = ((value * 16f) - 1f) / 255f;
        v = Math.min(1f,  v);
        v = Math.max(0f, v);
        return v;
    }
    
    public void drawDistanceFieldQuad(
            Texture texture, float scale,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2,
            float x3, float y3, float c3, float u3, float v3) {
        final int   tid  = prepareTriangleRendering(texture);
        final float ctrl = ctrlFloat(DrawAlgorithm.DistanceField, tid, sixteenth(scale));
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
        drawTriangle(ctrl,
                x2, y2, c2, u2, v2,
                x3, y3, c3, u3, v3,
                x0, y0, c0, u0, v0);
    }

    // ----------------------------------------------------
    // DistanceFieldOutline
    
    public void drawDistanceFieldOutlineTriangle(
            Texture texture, float scale, float outline,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2) {
        final int   tid  = prepareTriangleRendering(texture);
        final float ctrl = ctrlFloat(DrawAlgorithm.DistanceFieldOutline, tid, sixteenth(scale), outline);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
    }

    public void drawDistanceFieldOutlineQuad(
            Texture texture, float scale, float outline,
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2,
            float x3, float y3, float c3, float u3, float v3) {
        final int   tid  = prepareTriangleRendering(texture);
        final float ctrl = ctrlFloat(DrawAlgorithm.DistanceFieldOutline, tid, sixteenth(scale), outline);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
        drawTriangle(ctrl,
                x2, y2, c2, u2, v2,
                x3, y3, c3, u3, v3,
                x0, y0, c0, u0, v0);
    }
   

    // ----------------------------------------------------
    // DistanceField
    
    public void drawDistanceFieldVTriangle(
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2) {
        final float ctrl = ctrlFloat(DrawAlgorithm.DistanceFieldV);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
    }

    public void drawDistanceFieldVQuad(
            float x0, float y0, float c0, float u0, float v0,
            float x1, float y1, float c1, float u1, float v1,
            float x2, float y2, float c2, float u2, float v2,
            float x3, float y3, float c3, float u3, float v3) {
        final float ctrl = ctrlFloat(DrawAlgorithm.DistanceFieldV);
        drawTriangle(ctrl,
                x0, y0, c0, u0, v0,
                x1, y1, c1, u1, v1,
                x2, y2, c2, u2, v2);
        drawTriangle(ctrl,
                x2, y2, c2, u2, v2,
                x3, y3, c3, u3, v3,
                x0, y0, c0, u0, v0);
    }
     
    

    public void begin() {
        Gdx.gl.glDepthMask(false);
        program.bind();
        setup();
    }

    // Should update the transformation matrix here.
    protected void setup () {
        projectionMatrix.setToOrtho2D(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        combinedMatrix.set(projectionMatrix).mul(transformMatrix);
        program.setUniformMatrix("u_projTrans", combinedMatrix);
        for (int i=0; i<MAX_TEXTURES; i++)
            program.setUniformi("u_texture_" + i, i);
    }

    static final float algoFactor = 255f;
    
    public static float ctrlFloat(DrawAlgorithm algo) {
        return Color.toFloatBits(0f, 0f, 0f, algo.value * 2 / algoFactor);
    }
    
    public static float ctrlFloat(DrawAlgorithm algo, int texture) {
        return Color.toFloatBits(texture / 255f, 0f, 0f, algo.value * 2 / algoFactor);
    }

    public static float ctrlFloat(DrawAlgorithm algo, int texture, float arg0) {
        return Color.toFloatBits(texture / 255f, arg0, 0f, algo.value * 2 / algoFactor);
    }
    
    public static float ctrlFloat(DrawAlgorithm algo, int texture, float arg0, float arg1) {
        return Color.toFloatBits(texture / 255f, arg0, arg1, algo.value * 2 / algoFactor);
    }

    public void end() {
        flush();
        GL20 gl = Gdx.gl;
        gl.glDepthMask(true);
    }
    
    // Should take a color or vertex colors arguments.
    public void draw (TextureRegion region, float x, float y, float width, float height) {
        Texture texture = region.getTexture();
        final float x1    = x + width;
        final float y1    = y + height;
        final float u0    = region.getU();
        final float v0    = region.getV2();
        final float u1    = region.getU2();
        final float v1    = region.getV();
        final float color = Color.WHITE_FLOAT_BITS; 
        drawTexturedQuad(
                texture, 
                x,   y, color, u0, v0,
                x1,  y, color, u1, v0,
                x1, y1, color, u1, v1,
                x,  y1, color, u0, v1);
    }
 
    public int getTextureId(Texture t) {
        for (int i=0; i<textureCount; i++)
            if (textures[i] == t)
                return i;
        if (textureCount < textures.length) {
            textures[textureCount] = t;
            return textureCount ++;
        }
        return -1;
    }
    
    public int prepareTriangleRendering(Texture t) {
        if (triangleCount >= maxTriangles)
            flush();
        int tid = getTextureId(t);
        if (tid < 0) {
            flush();
            tid = getTextureId(t);
        }
        return tid;
    }

    public void draw (Texture texture, float x, float y) {
        draw(texture, x, y, texture.getWidth(), texture.getHeight());
    }

    public void draw (Texture texture, float x, float y, float width, float height) {
        final float color = Color.WHITE_FLOAT_BITS; 
        final float x1 = x + width;
        final float y1 = y + height;
        final float u0 = 0;
        final float v0 = 1;
        final float u1 = 1;
        final float v1 = 0;
        drawTexturedQuad(
                texture, 
                x,  y,  color, u0, v0,
                x1, y,  color, u1, v0,
                x1, y1, color, u1, v1,
                x,  y1, color, u0, v1);
    }
    
}
