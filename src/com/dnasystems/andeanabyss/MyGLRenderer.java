/*
 * Copyright (C) 2012 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dnasystems.andeanabyss;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;
import android.content.Context;

public class MyGLRenderer implements GLSurfaceView.Renderer, OnInput {

    private static final String TAG = AndeanAbyssCBG.TAG;
    private GameBoard mGameBoard;

    private final float[] mMVPMatrix = new float[16];
    private final float[] mProjMatrix = new float[16];
    private final float[] mVMatrix = new float[16];
    //private final float[] mRotationMatrix = new float[16];
    
    private Context mContext;
    private GLSurfaceView mView;

    private float mRatio;

    public MyGLRenderer(Context context, GLSurfaceView view) {
        mContext = context;
        mView = view;
        /*mTriangleVertices = ByteBuffer.allocateDirect(mTriangleVerticesData.length
                * FLOAT_SIZE_BYTES).order(ByteOrder.nativeOrder()).asFloatBuffer();
        mTriangleVertices.put(mTriangleVerticesData).position(0);*/
    }

    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {

        // Set the background frame color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA);
        GLES20.glEnable(GLES20.GL_BLEND);

        mGameBoard = new GameBoard(mContext, mView);
    }

    @Override
    public void onDrawFrame(GL10 unused) {

        // Draw background color
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);

        // Set the camera position (View matrix)
        //Matrix.setLookAtM(mVMatrix, 0, 0, 0, -3, 0f, 0f, 0f, 0f, 1.0f, 0.0f);
        Matrix.setIdentityM(mVMatrix, 0);

        // Calculate the projection and view transformation
        Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        //Matrix.multiplyMM(mMVPMatrix, 0, mProjMatrix, 0, mVMatrix, 0);

        //Matrix.setRotateM(mRotationMatrix, 0, mAngle, 0, 0, -1.0f);

        // Combine the rotation matrix with the projection and camera view
        //Matrix.multiplyMM(mMVPMatrix, 0, mRotationMatrix, 0, mMVPMatrix, 0);

        // Draw GameBoard
        mGameBoard.draw(mMVPMatrix);
    }

    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        // Adjust the viewport based on geometry changes,
        // such as screen rotation
        GLES20.glViewport(0, 0, width, height);

        mRatio = (float) width / height;

        // this projection matrix is applied to object coordinates
        // in the onDrawFrame() method
        //Matrix.frustumM(mProjMatrix, 0, -mRatio, mRatio, -1, 1, 3, 7);
        Matrix.orthoM(mProjMatrix, 0, 0, (float)width, (float)height, 0,
                      0, 0.1f);

    }

    public static int loadShader(int type, String shaderCode){

        // create a vertex shader type (GLES20.GL_VERTEX_SHADER)
        // or a fragment shader type (GLES20.GL_FRAGMENT_SHADER)
        int shader = GLES20.glCreateShader(type);

        // add the source code to the shader and compile it
        GLES20.glShaderSource(shader, shaderCode);
        GLES20.glCompileShader(shader);
        int[] buffer = new int[1];
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, buffer, 0);
        if(buffer[0] == GLES20.GL_FALSE) {
            Log.e(TAG, "ShaderHelper " + GLES20.glGetShaderInfoLog(shader));
            GLES20.glDeleteShader(shader);
            return -1;
        }
        return shader;
    }

    public boolean onUp(int id, float x, float y) {
        return mGameBoard.onUp(id, x, y);
    }

    public boolean onDown(int id, float x, float y) {
        return mGameBoard.onDown(id, x, y);
    }

    public boolean onMove(int id, float dx, float dy) {
        return mGameBoard.onMove(id, dx, dy);
    }

    public boolean onZoom(float scalefactor) {
        return mGameBoard.onZoom(scalefactor);
    }

    /**
     * Utility method for debugging OpenGL calls. Provide the name of the call
     * just after making it:
     *
     * <pre>
     * mColorHandle = GLES20.glGetUniformLocation(mProgram, "vColor");
     * MyGLRenderer.checkGlError("glGetUniformLocation");</pre>
     *
     * If the operation is not successful, the check throws an error.
     *
     * @param glOperation - Name of the OpenGL call to check.
     */
    public static void checkGlError(String glOperation) {
        int error;
        while ((error = GLES20.glGetError()) != GLES20.GL_NO_ERROR) {
            Log.e(TAG, glOperation + ": glError " + error);
            throw new RuntimeException(glOperation + ": glError " + error);
        }
    }
}
