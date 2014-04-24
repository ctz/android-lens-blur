package io.jbp.depthimagetest;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.opengl.GLES20;
import android.opengl.GLU;
import android.opengl.GLUtils;
import android.opengl.GLSurfaceView.Renderer;
import android.opengl.Matrix;
import android.util.Log;

public class DepthRenderer implements Renderer
{
  private final String TAG = "DepthRenderer";

  private boolean useBlur;
  private boolean showDepth;
  
  private int program;
  private int programBlur;
  private int vertexPositionAttributeLocation;
  private int MVPMatrixUniformLocation;
  private int skewUniformLocation;
  private int colourTextureLocation;
  private int depthTextureLocation;
  private int textureCoordinateLocation;

  private float[] modelMatrix = new float[16];
  private float[] projMatrix = new float[16];
  private float[] viewMatrix = new float[16];
  
  private float[] skew = new float[] { 0f, 0f };
  
  private float pad = 0.f;

  private float[] vertices =
  {
   1.0f, 1.0f, 0.0f,
   -1.0f, 1.0f, 0.0f,
   1.0f, -1.0f, 0.0f,
   -1.0f, -1.0f, 0.0f,
  };
  private FloatBuffer vertexBuffer;
  
  private float[] uvs =
  {
    1.0f, 0.0f, 0.0f,
   0.0f, 0.0f, 0.0f,
   1.0f, 1.0f, 0.0f,
   0.0f, 1.0f, 0.0f,
  };
  private FloatBuffer texBuffer;
  
  private int[] textureHandles = new int[2];
  private static final int TEXTURE_COLOUR = 0;
  private static final int TEXTURE_DEPTH = 1;

  private final Bitmap colourBitmap;
  private final Bitmap depthBitmap;
  private final Resources resources;

  DepthRenderer(final Bitmap colour, final Bitmap depth, final Resources res)
  {
    colourBitmap = colour;
    depthBitmap = depth;
    resources = res;
  }

  @Override
  public void onDrawFrame(GL10 gl)
  {
    GLES20.glClearColor(0.1f, 0.1f, 0.1f, 1);
    GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

    GLES20.glUseProgram(useBlur ? programBlur : program);
    checkGlError("glUseProgram");
    
    Matrix.multiplyMM(modelMatrix, 0, projMatrix, 0, viewMatrix, 0);

    GLES20.glUniformMatrix4fv(MVPMatrixUniformLocation, 1, false, modelMatrix, 0);
    checkGlError("glUniform4fv");
    
    GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
    checkGlError("glActiveTexture");
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[showDepth ? TEXTURE_DEPTH : TEXTURE_COLOUR]);
    checkGlError("glBindTexture");
    GLES20.glUniform1i(colourTextureLocation, 0);
    checkGlError("glUniform1i");
    
    GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
    checkGlError("glActiveTexture (depth)");
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[TEXTURE_DEPTH]);
    checkGlError("glBindTexture (depth)");
    GLES20.glUniform1i(depthTextureLocation, 1);
    checkGlError("glUniform1i (depth)");

    GLES20.glEnableVertexAttribArray(textureCoordinateLocation);
    texBuffer.position(0);
    GLES20.glVertexAttribPointer(textureCoordinateLocation, 4, GLES20.GL_FLOAT, false, 3 * 4, texBuffer);
    checkGlError("glVertexAttribPointer (texture)");

    GLES20.glEnableVertexAttribArray(vertexPositionAttributeLocation);
    checkGlError("glEnableVertexAttribArray (vertexPositionAttributeLocation)");
    vertexBuffer.position(0);
    GLES20.glVertexAttribPointer(vertexPositionAttributeLocation, 4, GLES20.GL_FLOAT, false, 3 * 4, vertexBuffer);
    checkGlError("glVertexAttribPointer (vertices)");
    
    GLES20.glUniform2f(skewUniformLocation, skew[0], skew[1]);
    checkGlError("glUniform2f (skews)");    
    
    GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    checkGlError("glDrawArrays");
  }

  @Override
  public void onSurfaceChanged(GL10 gl, int width, int height)
  {
    Log.v(TAG, "onSurfaceChanged " + width + "x" + height);
    Log.v(TAG, "bitmap is " + colourBitmap.getWidth() + "x" + colourBitmap.getHeight());
    float imageRatio = (float) colourBitmap.getWidth() / colourBitmap.getHeight();
    
    if (colourBitmap.getWidth() > width)
    {
      int newHeight = (int) (width / imageRatio);
      Log.v(TAG, "too wide, viewport is " + width + "x" + newHeight);
      GLES20.glViewport(0, (height - newHeight) / 2, width, newHeight);
    } else if (colourBitmap.getHeight() > height) {
      int newWidth = (int) (height * imageRatio);
      Log.v(TAG, "too high, viewport is " + newWidth + "x" + height);
      GLES20.glViewport((width - newWidth) / 2, 0, newWidth, height);
    } else {
      Log.v(TAG, "image size");
      GLES20.glViewport((width - colourBitmap.getWidth()) / 2,
                        (height - colourBitmap.getHeight()) / 2,
                        colourBitmap.getWidth(),
                        colourBitmap.getHeight());
    }
    
    Matrix.orthoM(projMatrix, 0, -1 - pad, 1 + pad, -1 - pad, 1 + pad, 0f, 1f);
    Matrix.setIdentityM(viewMatrix, 0);
  }

  @Override
  public void onSurfaceCreated(GL10 gl10, EGLConfig config)
  {
    vertexBuffer = ByteBuffer.allocateDirect(vertices.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    vertexBuffer.put(vertices);
    vertexBuffer.position(0);
    
    texBuffer = ByteBuffer.allocateDirect(uvs.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
    texBuffer.put(uvs);
    texBuffer.position(0);

    program = createProgram(false);
    programBlur = createProgram(true);

    textureCoordinateLocation = GLES20.glGetAttribLocation(program, "aTexCoordinate");
    checkGlError("glGetUniformLocation (aTexCoordinate)");
    skewUniformLocation = GLES20.glGetUniformLocation(program, "uSkew");
    checkGlError("glGetUniformLocation (uSkew)");
    vertexPositionAttributeLocation = GLES20.glGetAttribLocation(program, "aVertexPosition");
    checkGlError("glGetAttribLocation (aVertexPosition)");
    MVPMatrixUniformLocation = GLES20.glGetUniformLocation(program, "uMVPMatrix");
    checkGlError("glGetUniformLocation (uMVPMatrix)");

    colourTextureLocation = GLES20.glGetUniformLocation(program, "uColourTexture");
    checkGlError("glGetUniformLocation (colour)");
    depthTextureLocation = GLES20.glGetUniformLocation(program, "uDepthTexture");
    checkGlError("glGetUniformLocation (depth)");
    
    GLES20.glGenTextures(2, textureHandles, 0);
    
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[TEXTURE_COLOUR]);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, colourBitmap, 0);
    
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandles[TEXTURE_DEPTH]);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
    GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
    GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, depthBitmap, 0);
  }

  private void checkGlError(String op)
  {
    int error;
    while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR)
    {
      final String msg = op + ": opengl error " + GLU.gluErrorString(error) + " (" + error + ")";
      Log.e(TAG, msg);
      throw new RuntimeException(msg);
    }
  }

  private int createProgram(boolean withBlur)
  {
    int program = GLES20.glCreateProgram();

    int vertexShader = getShader(GLES20.GL_VERTEX_SHADER, R.string.vertex_shader);
    int fragmentShader = getShader(GLES20.GL_FRAGMENT_SHADER, withBlur ? R.string.fragment_shader_blur : R.string.fragment_shader);

    GLES20.glAttachShader(program, vertexShader);
    GLES20.glAttachShader(program, fragmentShader);
    GLES20.glLinkProgram(program);
    GLES20.glUseProgram(program);

    return program;
  }

  private int getShader(int type, int source)
  {
    int shader = GLES20.glCreateShader(type);
    String shaderSource = resources.getString(source);
    GLES20.glShaderSource(shader, shaderSource);
    GLES20.glCompileShader(shader);

    int[] compiled = new int[1];
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0);
    if (compiled[0] == 0)
    {
      Log.e(TAG, "Could not compile shader");
      Log.e(TAG, GLES20.glGetShaderInfoLog(shader));
      Log.e(TAG, shaderSource);
    }

    return shader;
  }
  
  public void setSkew(float x, float y)
  {
    skew[0] = x;
    skew[1] = y;
  }
  
  public boolean toggleBlur()
  {
    useBlur = !useBlur;
    return useBlur;
  }
  
  public boolean toggleShowDepth()
  {
    showDepth = !showDepth;
    return showDepth;
  }
}
