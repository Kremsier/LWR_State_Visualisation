package com.vuforia.samples.VuforiaSamples.app;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Bundle;

import com.vuforia.samples.VuforiaSamples.MyGLSurfaceView;

/**
 * Created by julianahlers on 10.05.16.
 */
public class OpenGLESActivity extends Activity {
    private GLSurfaceView mGLView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Create a GLSurfaceView instance and set it
        // as the ContentView for this Activity.
        mGLView = new MyGLSurfaceView(this);
        setContentView(mGLView);
    }
}
