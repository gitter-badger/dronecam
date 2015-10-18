package me.flavors.cnheider.dronecam.ui.ar;

import static me.flavors.cnheider.dronecam.ui.ar.GLUtils.checkGLError;

import android.content.Context;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import com.google.vrtoolkit.cardboard.CardboardView;
import com.google.vrtoolkit.cardboard.Eye;
import com.google.vrtoolkit.cardboard.HeadTransform;
import com.google.vrtoolkit.cardboard.Viewport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.egl.EGLConfig;

import me.flavors.cnheider.dronecam.R;
import me.flavors.cnheider.dronecam.ui.widget.CardboardOverlayView;
import timber.log.Timber;

public class Renderer implements CardboardView.StereoRenderer {

    // We keep the light always position just above the user.
    private final float[] mLightPosInWorldSpace = new float[] {0.0f, 2.0f, 0.0f, 1.0f};
    private final float[] mLightPosInEyeSpace = new float[4];
    private final float[] mEulerAngles = new float[3];

    private static final int COORDS_PER_VERTEX = 3;

    private static final float CAMERA_Z = 0.01f;

    private static final float Z_NEAR = 0.1f;
    private static final float Z_FAR = 100.0f;

    private final WorldLayoutData DATA = new WorldLayoutData();

    private Context mContext;

    private int[] mGlPrograms;

    private FloatBuffer mWallVertices;
    private FloatBuffer mWallColors;
    private FloatBuffer mWallNormals;

    // Head Position
    private float[] mHeadView;

    private float[] mView;
    private float[] mCamera;
    private float[] mModelWall;
    private float[] mModelViewProjectionWall;
    private float[] mModelViewWall;

    private int[] mTextures;

    private int mPositionParam;
    private int mTextureCoordParam;
    private int mTextureParam;

    private int mNormalParam;
    private int mColorParam;
    private int mModelViewProjectionParam;
    private int mLightPosParam;
    private int mModelViewParam;
    private int mModelParam;
    private int mIsFloorParam;


    private FloatBuffer pVertex;
    private FloatBuffer pTexCoord;

    private CardboardOverlayView mOverlayView;

    private Camera mRealCamera;
    private SurfaceTexture mRealCameraTexture;

    public Renderer(Context ctx) {
        mContext = ctx;

        mCamera = new float[16];
        mView = new float[16];
        mHeadView = new float[16];

        mGlPrograms = new int[2];

        mModelWall = new float[16];

        mModelViewProjectionWall = new float[16];
        mModelViewWall = new float[16];

        float[] vtmp = { 1.0f, -1.0f, -1.0f, -1.0f, 1.0f, 1.0f, -1.0f, 1.0f };
        float[] ttmp = { 1.0f, 1.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f };
        pVertex = ByteBuffer.allocateDirect(8 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pVertex.put ( vtmp );
        pVertex.position(0);
        pTexCoord = ByteBuffer.allocateDirect(8*4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        pTexCoord.put ( ttmp );
        pTexCoord.position(0);
    }

    @Override
    public void onNewFrame(HeadTransform headTransform) {
        // Build the camera matrix and apply it to the ModelView.
        Matrix.setLookAtM(mCamera, 0, 0.0f, 0.0f, CAMERA_Z, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f);

        headTransform.getHeadView(mHeadView, 0);

        mRealCameraTexture.updateTexImage();

        headTransform.getEulerAngles(mEulerAngles, 0);

        Log.i("d","mEulerAngles: " + mEulerAngles[0] + ", " + mEulerAngles[1] + ", " + mEulerAngles[2]);


                //= (CardboardOverlayView)findViewById(R.id.overlay);

        checkGLError("onReadyToDraw");
    }

    @Override
    public void onDrawEye(Eye eyeTransform) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Apply the eye transformation to the camera.
        Matrix.multiplyMM(mView, 0, eyeTransform.getEyeView(), 0, mCamera, 0);

        GLES20.glUseProgram(mGlPrograms[1]);

        mTextureParam = GLES20.glGetAttribLocation(mGlPrograms[1], "v_Texture");
        mTextureCoordParam = GLES20.glGetAttribLocation(mGlPrograms[1], "v_TextCoord");
        mModelViewProjectionParam = GLES20.glGetUniformLocation(mGlPrograms[1], "u_MVP");
        mLightPosParam = GLES20.glGetUniformLocation(mGlPrograms[1], "u_LightPos");
        mModelViewParam = GLES20.glGetUniformLocation(mGlPrograms[1], "u_MVMatrix");
        mModelParam = GLES20.glGetUniformLocation(mGlPrograms[1], "u_Model");
        mIsFloorParam = GLES20.glGetUniformLocation(mGlPrograms[1], "u_IsFloor");
        mPositionParam = GLES20.glGetAttribLocation(mGlPrograms[1], "a_Position");
        mNormalParam = GLES20.glGetAttribLocation(mGlPrograms[1], "a_Normal");
        mColorParam = GLES20.glGetAttribLocation(mGlPrograms[1], "a_Color");

        GLES20.glVertexAttribPointer(mTextureCoordParam, 2, GLES20.GL_FLOAT, false, 4 * 2, pTexCoord);
        GLES20.glEnableVertexAttribArray(mTextureCoordParam);

        GLES20.glEnableVertexAttribArray(mPositionParam);
        GLES20.glEnableVertexAttribArray(mNormalParam);
        GLES20.glEnableVertexAttribArray(mColorParam);
        checkGLError("mColorParam");

        // Set the position of the light
        Matrix.multiplyMV(mLightPosInEyeSpace, 0, mView, 0, mLightPosInWorldSpace, 0);
        GLES20.glUniform3f(mLightPosParam, mLightPosInEyeSpace[0], mLightPosInEyeSpace[1],
                mLightPosInEyeSpace[2]);


        //Matrix.multiplyMM(mModelView, 0, mView, 0, mModelWall, 0);
        Matrix.setIdentityM(mModelViewWall, 0);
        Matrix.translateM(mModelViewWall, 0, 0, -5f, -78f);
        Matrix.multiplyMM(mModelViewProjectionWall, 0, eyeTransform.getPerspective(Z_NEAR, Z_FAR), 0, mModelViewWall, 0);
        drawWall();
    }

    @Override
    public void onFinishFrame(Viewport viewport) {

    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        Timber.e("onSurfaceChanged(%d, %d)", width, height);
    }

    @Override
    public void onSurfaceCreated(EGLConfig eglConfig) {
        Timber.e("onSurfaceCreated");

        GLES20.glClearColor(0.1f, 0.1f, 0.1f, 0.5f); // Dark background so text shows up well

        // Init Objects here
        initTextures();

        initRealWorldCamera();

        initModels();

        initWorldShader();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        Matrix.setIdentityM(mModelWall, 0);
        Matrix.translateM(mModelWall, 0, 0, 0, -16f);

        checkGLError("onSurfaceCreated");
    }

    @Override
    public void onRendererShutdown() {
        Timber.e("onRendererShutdown");

        GLES20.glDeleteTextures(1, mTextures, 0);

        mRealCamera.stopPreview();
        mRealCamera.setPreviewCallbackWithBuffer(null);
        mRealCamera.release();
    }

    private void initTextures() {
        mTextures = new int[1];
        GLES20.glGenTextures(1, mTextures, 0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
    }
    private void initRealWorldCamera() {
        mRealCamera = Camera.open(0);

        //Set the camera parameters
        Camera.Parameters params = mRealCamera.getParameters();

        int fps = 0;
        for(Integer nfps : params.getSupportedPreviewFrameRates()) {
            if(nfps > fps)
                fps = nfps;
        }
        params.setPreviewFrameRate(fps);

        params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        params.setPreviewSize(1920, 1080);
        mRealCamera.setParameters(params);

        mRealCameraTexture = new SurfaceTexture(mTextures[0]);
        try {
            mRealCamera.setPreviewTexture(mRealCameraTexture);
        } catch (IOException t) {
            Timber.e("Cannot set preview texture target!");
        }

        //Start the preview
        mRealCamera.startPreview();
    }
    private void initModels() {

        // make wall
        ByteBuffer bbWallVertices = ByteBuffer.allocateDirect(DATA.WALL_COORDS.length * 4);
        bbWallVertices.order(ByteOrder.nativeOrder());
        mWallVertices = bbWallVertices.asFloatBuffer();
        mWallVertices.put(DATA.WALL_COORDS);
        mWallVertices.position(0);

        ByteBuffer bbWallNormals = ByteBuffer.allocateDirect(DATA.WALL_NORMALS.length * 4);
        bbWallNormals.order(ByteOrder.nativeOrder());
        mWallNormals = bbWallNormals.asFloatBuffer();
        mWallNormals.put(DATA.WALL_NORMALS);
        mWallNormals.position(0);

        ByteBuffer bbWallColors = ByteBuffer.allocateDirect(DATA.WALL_COLORS.length * 4);
        bbWallColors.order(ByteOrder.nativeOrder());
        mWallColors = bbWallColors.asFloatBuffer();
        mWallColors.put(DATA.WALL_COLORS);
        mWallColors.position(0);
    }
    private int initWorldShader() {
        int vertexShader = GLUtils.loadGLShader(mContext, GLES20.GL_VERTEX_SHADER, R.raw.light_vertex);
        int gridShader = GLUtils.loadGLShader(mContext, GLES20.GL_FRAGMENT_SHADER, R.raw.grid_fragment);

        mGlPrograms[1] = GLES20.glCreateProgram();
        GLES20.glAttachShader(mGlPrograms[1], vertexShader);
        GLES20.glAttachShader(mGlPrograms[1], gridShader);
        GLES20.glLinkProgram(mGlPrograms[1]);

        return mGlPrograms[1];
    }
    public void drawWall() {

        GLES20.glUniform1f(mIsFloorParam, 0.3f);

        // Set the Model in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(mModelParam, 1, false, mModelViewWall, 0);

        // Set the ModelView in the shader, used to calculate lighting
        GLES20.glUniformMatrix4fv(mModelViewParam, 1, false, mModelViewWall, 0);

        // Set the position of the cube
        GLES20.glVertexAttribPointer(mPositionParam, COORDS_PER_VERTEX, GLES20.GL_FLOAT,
                false, 0, mWallVertices);

        // Set the ModelViewProjection matrix in the shader.
        GLES20.glUniformMatrix4fv(mModelViewProjectionParam, 1, false, mModelViewProjectionWall, 0);

        // Set the normal positions of the cube, again for shading
        GLES20.glVertexAttribPointer(mNormalParam, 3, GLES20.GL_FLOAT,
                false, 0, mWallNormals);

        GLES20.glVertexAttribPointer(mColorParam, 4, GLES20.GL_FLOAT, false,
                0, mWallColors);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mTextures[0]);
        GLES20.glUniform1i(mTextureParam, 0);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);

        checkGLError("drawing wall");
    }
}
