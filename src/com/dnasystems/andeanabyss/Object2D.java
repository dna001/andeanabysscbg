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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ListIterator;

import android.opengl.ETC1Util;
import android.opengl.GLES20;
import android.opengl.GLUtils;
import android.opengl.Matrix;
import android.util.Log;
import android.content.Context;
import android.content.res.Resources.NotFoundException;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class Object2D implements OnInput {

	protected String TAG = AndeanAbyssCBG.TAG;
    /**
     * FloatBuffer containing vertex data (x, y, z)
     */
    protected float[] mVertices = {
            0.0f, 0.0f, 0.0f,
            0.0f, 1.0f, 0.0f,
            1.0f, 0.0f, 0.0f,
            1.0f, 1.0f, 0.0f};
    /**
     * FloatBuffer containing texture coordinates (u, v)
     */
    protected float[] mTextureCoords = {
            0.0f, 0.0f,
            0.0f, 1.0f,
            1.0f, 0.0f,
            1.0f, 1.0f
            };

    protected int mTextureID;
    protected float mX = 0;
    protected float mY = 0;
    protected float mW;
    protected float mH;
    protected int mProgram;
    protected int mMVPMatrixHandle;

    protected int vertexBufferPointer;
    protected int textureBufferPointer;

    public Object2D(Context context, String image, float w, float h, int img_w, int img_h) {
        // initialize vertex byte buffer for shape coordinates
        mW = w;
        mH = h;
        mVertices[4] = -h;
        mVertices[6] = -w;
        mVertices[9] = -w;
        mVertices[10] = -h;
        mTextureID = loadTexture(context, 0, image, img_w, img_h);
        initBuffers();
    }

    public Object2D(Context context, int resource, float w, float h) {
        // initialize vertex byte buffer for shape coordinates
        mW = w;
        mH = h;
        mVertices[4] = -h;
        mVertices[6] = -w;
        mVertices[9] = -w;
        mVertices[10] = -h;
        mTextureID = loadTexture(context, resource, null, 0, 0);
        initBuffers();
    }

    public Object2D(Object2D obj) {
        // Copy object (reuse texture)
        for (int i=0;i<12;i++) {
            this.mVertices[i] = obj.mVertices[i];
        }
        for (int i=0;i<8;i++) {
            this.mTextureCoords[i] = obj.mTextureCoords[i];
        }
        this.mW = obj.mW;
        this.mH = obj.mH;
        this.mProgram = obj.mProgram;
        this.mTextureID = obj.mTextureID;
        initBuffers();
    }

    public void setPos(float x, float y) {
        mX = x;
        mY = y;
    }

    public void setShader(int program) {
        mProgram = program;
    }

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
    }

    public boolean getAt(float x, float y, float scale) {
        if (((x > mX*scale) && (x < (mX + mW)*scale)) &&
            ((y > mY*scale) && (y < (mY + mH)*scale))) {
            return true;
        }
        return false;
    }

    public boolean onUp(int id, float x, float y) {
        return false;
    }

    public boolean onDown(int id, float x, float y) {
        return false;
    }

    public boolean onMove(int id, float dx, float dy) {
        mX += dx;
        mY += dy;
        return true;
    }

    public boolean onZoom(float scalefactor) {
        return false;
    }

    // Will load a texture out of a drawable resource file, and return an OpenGL texture ID:
    protected int loadTexture(Context context, int resource, String name,
            int img_w, int img_h) {
        int[] temp = new int[1];
        // In which ID will we be storing this texture?
        GLES20.glGenTextures(1, temp, 0);
        int id = temp[0];

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, id);

        // Set all of our texture parameters:
        GLES20.glPixelStorei(GLES20.GL_UNPACK_ALIGNMENT, GLES20.GL_TRUE);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_REPEAT);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_REPEAT);

        if (name != null) {
            Log.v(TAG, "ETC info: ");
            try {
                Log.i(TAG, "supports etc: " + ETC1Util.isETC1Supported());
                // In a real app, you should read the texture on a bg thread with createTexture()
                // and then only do the upload of the result on the render thread. You probably
                // also want to put all mipmaps into a single file.
                //String name = String.format("map.pkm");
                ETC1Util.loadTexture(GLES20.GL_TEXTURE_2D, 0, 0,
                        GLES20.GL_RGB, GLES20.GL_UNSIGNED_SHORT_5_6_5,
                        context.getAssets().open(name));
            } catch (NotFoundException e) {
                Log.e(TAG, "" + e);
            } catch (IOException e) {
                Log.e(TAG, "" + e);
            }
            mTextureCoords[3] = mH/(float)img_h;
            mTextureCoords[4] = mW/(float)img_w;
            mTextureCoords[6] = mW/(float)img_w;
            mTextureCoords[7] = mH/(float)img_h;
        } else {
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inScaled = false;
            opts.inPreferredConfig = Bitmap.Config.ARGB_4444;

            // Load up, and flip the texture:
            Bitmap bitmap = BitmapFactory.decodeResource(context.getResources(), resource, opts);
            Log.v(TAG, "Bitmap info: " + bitmap.getWidth() + ", " + bitmap.getHeight() +
                    ", " + bitmap.hasAlpha());

            mTextureCoords[3] = mH/(float)bitmap.getHeight();
            mTextureCoords[4] = mW/(float)bitmap.getWidth();
            mTextureCoords[6] = mW/(float)bitmap.getWidth();
            mTextureCoords[7] = mH/(float)bitmap.getHeight();

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0);
            MyGLRenderer.checkGlError("texImage2D");
            bitmap.recycle();
        } 
 
        return id;
    }

    protected void initBuffers() {
        vertexBufferPointer = initFloatBuffer(mVertices);
        textureBufferPointer = initFloatBuffer(mTextureCoords);
    }

    protected int initFloatBuffer(float[] data) {
        int[] buffer = new int[1];
        GLES20.glGenBuffers(1, buffer, 0);
        int pointer = buffer[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, pointer);
        ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length * 4); //one float size is 4 bytes
        byteBuffer.order(ByteOrder.nativeOrder()); //byte order must be native
        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
        floatBuffer.put(data);
        floatBuffer.flip();
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, data.length * 4, floatBuffer, GLES20.GL_STATIC_DRAW);
        return pointer;
    }
}
