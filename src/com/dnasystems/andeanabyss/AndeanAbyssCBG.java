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

import android.app.Activity;
import android.content.Context;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

public class AndeanAbyssCBG extends Activity {

	public static final String TAG = "AndeanAbyssCBG";
    private GLSurfaceView mGLView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity
        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // The following call pauses the rendering thread.
        // If your OpenGL application is memory intensive,
        // you should consider de-allocating objects that
        // consume significant memory here.
        mGLView.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // The following call resumes a paused rendering thread.
        // If you de-allocated graphic objects for onPause()
        // this is a good place to re-allocate them.
        mGLView.onResume();
    }
}

class MyGLSurfaceView extends GLSurfaceView {

	private static final String TAG = AndeanAbyssCBG.TAG;
    private final MyGLRenderer mRenderer;
    private ScaleGestureDetector mScaleDetector;

    public MyGLSurfaceView(Context context) {
        super(context);

        // Create an OpenGL ES 2.0 context.
        setEGLContextClientVersion(2);

        // Set the Renderer for drawing on the GLSurfaceView
        mRenderer = new MyGLRenderer(context, this);
        setRenderer(mRenderer);

        // Create our ScaleGestureDetector
        mScaleDetector = new ScaleGestureDetector(context, new ScaleListener());

        // Render the view only when there is a change in the drawing data
        setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
    }

    //private final float TOUCH_SCALE_FACTOR = 180.0f / 320;
    private float mLastTouchX;
    private float mLastTouchY;
    private int mActivePointerId;

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        // Let the ScaleGestureDetector inspect all events.
        mScaleDetector.onTouchEvent(ev);
        
        final int action = ev.getAction();
        switch (action & MotionEvent.ACTION_MASK) {
        case MotionEvent.ACTION_DOWN: {
            final float x = ev.getX();
            final float y = ev.getY();
            
            mLastTouchX = x;
            mLastTouchY = y;
            mActivePointerId = ev.getPointerId(0);
            mRenderer.onDown(mActivePointerId, x,  y);
            break;
        }

        case MotionEvent.ACTION_MOVE: {
            final int pointerIndex = ev.findPointerIndex(mActivePointerId);
            final float x = ev.getX(pointerIndex);
            final float y = ev.getY(pointerIndex);

            // Only move if the ScaleGestureDetector isn't processing a gesture.
            if (!mScaleDetector.isInProgress()) {
                final float dx = x - mLastTouchX;
                final float dy = y - mLastTouchY;

                if (mRenderer.onMove(mActivePointerId, dx,  dy)) {
                    requestRender();
                }
            }

            mLastTouchX = x;
            mLastTouchY = y;

            break;
        }

        case MotionEvent.ACTION_UP: {
            if (mRenderer.onUp(mActivePointerId, ev.getX(),  ev.getY())) {
                requestRender();
            }
            mActivePointerId = MotionEvent.INVALID_POINTER_ID;
            break;
        }

        case MotionEvent.ACTION_CANCEL: {
            mActivePointerId = MotionEvent.INVALID_POINTER_ID;
            break;
        }

        case MotionEvent.ACTION_POINTER_UP: {
            final int pointerIndex = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK) 
                    >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
            final int pointerId = ev.getPointerId(pointerIndex);
            if (pointerId == mActivePointerId) {
                if (mRenderer.onUp(mActivePointerId, ev.getX(pointerIndex),
                        ev.getY(pointerIndex))) {
                    requestRender();
                }
                // This was our active pointer going up. Choose a new
                // active pointer and adjust accordingly.
                final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
                mLastTouchX = ev.getX(newPointerIndex);
                mLastTouchY = ev.getY(newPointerIndex);
                mActivePointerId = ev.getPointerId(newPointerIndex);
            }
            break;
        }
        }

        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {

            if (mRenderer.onZoom(detector.getScaleFactor())) {
                requestRender();
            }

            return true;
        }
    }
}
