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

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.ListIterator;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class GameBoard implements OnInput{

    private String TAG = AndeanAbyssCBG.TAG;
    private final String vertexShaderCode =
        // This matrix member variable provides a hook to manipulate
        // the coordinates of the objects that use this vertex shader
        "uniform mat4 uMVPMatrix;" +
        "attribute vec3 aVerPos;" +
        "attribute vec3 aVerCol;" +
        "attribute vec2 aTexPos;" +
        "varying vec3 vVerCol;" +
        "varying vec2 vTexPos;" +
        "void main() {" +
        "  vTexPos = aTexPos;" +
        "  vVerCol = aVerCol;" +
        // the matrix must be included as a modifier of gl_Position
        "  gl_Position = uMVPMatrix*vec4(aVerPos, 1.0);" +
        "}";

    private final String fragmentShaderCode =
        "precision mediump float;" +
        "varying vec3 vVerCol;" +
        "varying vec2 vTexPos;" +
        "uniform sampler2D uSampler;" +
        "void main() {" +
        "  gl_FragColor = texture2D(uSampler, vTexPos);" +
        "}";

    private int mProgram;
    private ArrayList<Object2D> mObjectsList;
    private Object2D mActiveObject = null;
    private int mGameBoardIndex;
    private int mCardIndex;
    private int mGovBasesIndex;
    private int mGovTroopsIndex;
    private int mGovPoliceIndex;
    private int mFarcBasesIndex;
    private int mFarcGuerrillasIndex;
    private int mCartelBasesIndex;
    private int mCartelGuerrillasIndex;
    private int mAucBasesIndex;
    private int mAucGuerrillasIndex;

    // Declare as volatile because we are updating it from another thread
    public volatile float mZoom = 1.0f;
    public volatile float mPosX = 0.0f;
    public volatile float mPosY = 0.0f;

    private GLSurfaceView mView;

    public GameBoard(Context context, GLSurfaceView view) {
        long texStart = System.nanoTime();
        Object2D obj;
        mView = view;
        initShaders();
        mObjectsList = new ArrayList<Object2D>();
        obj = new GameBoardObject(context);
        obj.setShader(mProgram);
        mGameBoardIndex = mObjectsList.size();
        mObjectsList.add(obj);
        obj = new Object2D(context, R.raw.card01, 254, 356);
        obj.setShader(mProgram);
        obj.setPos(200, 800);
        mCardIndex = mObjectsList.size();
        mObjectsList.add(obj);
        obj = new Object2D(context, R.raw.disc_blue, 50, 51);
        obj.setShader(mProgram);
        obj.setPos(360, 461);
        mGovBasesIndex = mObjectsList.size();
        mObjectsList.add(obj);
        for (int i=1;i<3;i++) {
            Object2D tmp = new Object2D(obj);
            tmp.setPos(360 + i*76, 461);
            mObjectsList.add(tmp);
        }
        obj = new Object2D(context, R.raw.cube_dkblue, 34, 39);
        obj.setShader(mProgram);
        obj.setPos(100, 200);
        mGovTroopsIndex = mObjectsList.size();
        mObjectsList.add(obj);
        for (int i=1;i<10;i++) {
            Object2D tmp = new Object2D(obj);
            tmp.setPos(100, 200 + i*25);
            mObjectsList.add(tmp);
        }
        obj = new Object2D(context, R.raw.disc_red, 50, 51);
        obj.setShader(mProgram);
        obj.setPos(554, 2849);
        mFarcBasesIndex = mObjectsList.size();
        mObjectsList.add(obj);
        for (int i=1;i<9;i++) {
            Object2D tmp = new Object2D(obj);
            tmp.setPos(554 + i*77, 2849);
            mObjectsList.add(tmp);
        }
        obj = new Object2D(context, R.raw.cylinder_red, 30, 47);
        obj.setShader(mProgram);
        obj.setPos(1543, 154);
        mFarcGuerrillasIndex = mObjectsList.size();
        mObjectsList.add(obj);
        for (int i=0;i<10;i++) {
            Object2D tmp = new Object2D(obj);
            tmp.setPos(1543 + (i%5)*35, 154 + (i/5)*50);
            mObjectsList.add(tmp);
        }
        obj = new Object2D(context, R.raw.disc_green, 50, 51);
        obj.setShader(mProgram);
        obj.setPos(1705, 2629);
        mCartelBasesIndex = mObjectsList.size();
        mObjectsList.add(obj);
        for (int i=1;i<15;i++) {
            Object2D tmp = new Object2D(obj);
            tmp.setPos(1705 + (i/5)*80, 2629 + (i%5)*77);
            mObjectsList.add(tmp);
        }
        obj = new Object2D(context, R.raw.disc_yellow, 50, 51);
        obj.setShader(mProgram);
        obj.setPos(555, 2962);
        mAucBasesIndex = mObjectsList.size();
        mObjectsList.add(obj);
        for (int i=1;i<6;i++) {
            Object2D tmp = new Object2D(obj);
            tmp.setPos(555 + i*76, 2962);
            mObjectsList.add(tmp);
        }
        double texS = (System.nanoTime() - texStart) / 1e9;
        Log.i(TAG, texS + " total tex load");
        //mTextureID = MyGLRenderer.loadTexture(context, R.raw.map);
    }

    private void initShaders() {
        //shaderProgram = ShaderLoader.loadProgram(TextReader.readResource(context.getResources(), R.raw.vshader), TextReader.readResource(context.getResources(), R.raw.fshader));
        // prepare shaders and OpenGL program
        int vertexShader = MyGLRenderer.loadShader(GLES20.GL_VERTEX_SHADER,
                                                   vertexShaderCode);
        int fragmentShader = MyGLRenderer.loadShader(GLES20.GL_FRAGMENT_SHADER,
                                                     fragmentShaderCode);

        mProgram = GLES20.glCreateProgram();             // create empty OpenGL Program
        GLES20.glAttachShader(mProgram, vertexShader);   // add the vertex shader to program
        GLES20.glAttachShader(mProgram, fragmentShader); // add the fragment shader to program
        GLES20.glBindAttribLocation(mProgram, 0, "aVerPos");
        GLES20.glBindAttribLocation(mProgram, 1, "aTexPos");
        GLES20.glLinkProgram(mProgram);                  // create OpenGL program executables
        Log.v("Shader program", mProgram + " is the id of shader program. :)");

    }

    private void updateGBParams() {
        float max_ofs_x = mView.getWidth() - 2048*mZoom;
        float max_ofs_y = mView.getHeight() - (2048 + 1116)*mZoom;

        if (mPosX < max_ofs_x) {
            mPosX = max_ofs_x;
        }
        if (mPosY < max_ofs_y) {
            mPosY = max_ofs_y;
        }
        if (mPosX > 0) {
            mPosX = (max_ofs_x > 0)?max_ofs_x/2:0;
        }
        if (mPosY > 0) {
            mPosY = (max_ofs_y > 0)?max_ofs_y/2:0;
        }
        Log.v(TAG, "max_ofs_x=" + max_ofs_x + " max_ofs_y=" + max_ofs_y);
        Log.v(TAG, "mPosX=" + mPosX + " mPosY=" + mPosY);

    }

    public boolean onUp(int id, float x, float y) {
        mActiveObject = null;
        return false;
    }

    public boolean onDown(int id, float x, float y) {
        // Convert x,y to Gameboard coordinates
        x -= mPosX;
        y -= mPosY;
        Log.v(TAG, "onDown: x=" + x + " y=" + y);
        // Select object on screen (iterate backwards)
        for(ListIterator<Object2D> iterator = mObjectsList.listIterator(mObjectsList.size());
        		iterator.hasPrevious();) {
            Object2D obj = iterator.previous();
            if (obj != null) {
                if (obj.getAt(x, y, mZoom)) {
                    mActiveObject = obj;
                    Log.v(TAG, "onDown: obj index " + mObjectsList.lastIndexOf(obj));
                    break;
                }
            }
        }

        return false;
    }

    public boolean onMove(int id, float dx, float dy) {
        boolean update = false;
        if (mActiveObject == mObjectsList.get(mGameBoardIndex)) {
            mPosX += dx;
            mPosY += dy;
            updateGBParams();
            update = true; 
        } else if (mActiveObject != null) {
            mActiveObject.onMove(id, dx/mZoom, dy/mZoom);
            update = true;
        }
        return update;
    }

    public boolean onZoom(float scalefactor) {
    	mZoom *= scalefactor;
        // Don't let the object get too small or too large.
        mZoom = Math.max((float)mView.getHeight()/(2048 + 1116), Math.min(mZoom, 1.0f));
        Log.v(TAG, "mZoom=" + mZoom);
        updateGBParams();
        return true;
    }

    public void draw(float[] mvpMatrix) {
    	float[] mvpMatrixLocal = mvpMatrix.clone();
        // Add program to OpenGL environment
        GLES20.glUseProgram(mProgram);

        Matrix.translateM(mvpMatrixLocal, 0, mPosX, mPosY, 0.0f);

        Matrix.scaleM(mvpMatrixLocal, 0, -mZoom, -mZoom, 0.0f);

        for(Iterator<Object2D> iterator = mObjectsList.iterator(); iterator.hasNext();) {
            Object2D obj = iterator.next();
            if (obj != null) {
                obj.draw(mvpMatrixLocal);
            }
        }
    }

}

class GameBoardObject extends Object2D {

    Object2D mChild;

    public GameBoardObject(Context context) {
        super(context, "map.pkm", 2048, 2048, 2048, 2048);
        mChild = new Object2D(context, "map2.pkm", 2048, 1116, 2048, 2048);
        mH = 2048 + 1116;
    }

    @Override
    public void draw(float[] mvpMatrix) {
        float[] mvpMatrixLocal = mvpMatrix.clone();
        Matrix.translateM(mvpMatrixLocal, 0, -mX, -mY, 0.0f);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, mTextureID);
        int uSamp = GLES20.glGetAttribLocation(mProgram, "uSampler");
        GLES20.glUniform1i(uSamp, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vertexBufferPointer);
        GLES20.glEnableVertexAttribArray(0);
        GLES20.glVertexAttribPointer(0, 3, GLES20.GL_FLOAT, false, 0, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, textureBufferPointer);
        GLES20.glEnableVertexAttribArray(1);
        GLES20.glVertexAttribPointer(1, 2, GLES20.GL_FLOAT, false, 0, 0);

        // get handle to shape's transformation matrix
        mMVPMatrixHandle = GLES20.glGetUniformLocation(mProgram, "uMVPMatrix");
        MyGLRenderer.checkGlError("glGetUniformLocation");

        // Apply the projection and view transformation
        GLES20.glUniformMatrix4fv(mMVPMatrixHandle, 1, false, mvpMatrixLocal, 0);
        MyGLRenderer.checkGlError("glUniformMatrix4fv");

        // Draw the square
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
        /*GLES20.glDrawElements(GLES20.GL_TRIANGLES, drawOrder.length,
                              GLES20.GL_UNSIGNED_SHORT, drawListBuffer);*/
        Matrix.translateM(mvpMatrixLocal, 0, 0, -2048, 0.0f);
        mChild.draw(mvpMatrixLocal);
    }
    
    @Override
    public void setShader(int program) {
        mProgram = program;
        mChild.setShader(program);
    }
}