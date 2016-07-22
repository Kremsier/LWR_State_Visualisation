/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/
/*===============================================================================
Copyright (c) 2016 PTC Inc. All Rights Reserved.

Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other
countries.
===============================================================================*/

package com.vuforia.samples.VuforiaSamples.app.VirtualButtons;

import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.util.Log;

import com.vuforia.Area;
import com.vuforia.ImageTargetResult;
import com.vuforia.Rectangle;
import com.vuforia.Renderer;
import com.vuforia.State;
import com.vuforia.Tool;
import com.vuforia.TrackableResult;
import com.vuforia.VIDEO_BACKGROUND_REFLECTION;
import com.vuforia.VirtualButton;
import com.vuforia.VirtualButtonResult;
import com.vuforia.Vuforia;
import com.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.vuforia.samples.SampleApplication.utils.CubeShaders;
import com.vuforia.samples.SampleApplication.utils.FifthSegment;
import com.vuforia.samples.SampleApplication.utils.FirstSegment;
import com.vuforia.samples.SampleApplication.utils.FourthSegment;
import com.vuforia.samples.SampleApplication.utils.LineShaders;
import com.vuforia.samples.SampleApplication.utils.SampleUtils;
import com.vuforia.samples.SampleApplication.utils.SecondSegment;
import com.vuforia.samples.SampleApplication.utils.SeventhSegment;
import com.vuforia.samples.SampleApplication.utils.SixthSegment;
import com.vuforia.samples.SampleApplication.utils.Teapot;
import com.vuforia.samples.SampleApplication.utils.Texture;
import com.vuforia.samples.SampleApplication.utils.ThirdSegment;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Vector;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;


public class VirtualButtonRenderer implements GLSurfaceView.Renderer
{
    private static final String LOGTAG = "VirtualButtonRenderer";

    private SampleApplicationSession vuforiaAppSession;

    public boolean mIsActive = false;

    private VirtualButtons mActivity;

    private Vector<Texture> mTextures;

    private Teapot mTeapot = new Teapot();
    private FirstSegment mFirstSegment = new FirstSegment();
    private SecondSegment mSecondSegment = new SecondSegment();
    private ThirdSegment mThirdSegment = new ThirdSegment();
    private FourthSegment mFourthSegment = new FourthSegment();
    private FifthSegment mFifthSegment = new FifthSegment();
    private SixthSegment mSixthSegment = new SixthSegment();
    private SeventhSegment mSeventhSegment = new SeventhSegment();
    private Tool mTool = new Tool();


    // OpenGL ES 2.0 specific (3D model):
    private int shaderProgramID = 0;
    private int vertexHandle = 0;
    private int normalHandle = 0;
    private int textureCoordHandle = 0;
    private int mvpMatrixHandle = 0;
    private int texSampler2DHandle = 0;

    private int lineOpacityHandle = 0;
    private int lineColorHandle = 0;
    private int mvpMatrixButtonsHandle = 0;

    // OpenGL ES 2.0 specific (Virtual Buttons):
    private int vbShaderProgramID = 0;
    private int vbVertexHandle = 0;

    // Constants:
    static private float kTeapotScale = 0.5f;
    //static private float kThirdSegmentScale = 1.05f;
    static private float kSeventhSegmentScale = 0.6f;

    private double prevTime;
    private float rotateBallAngle;
    private boolean button1Set;
    private boolean button1Hold;
    private boolean button2Set;
    private boolean button2Hold;
    private boolean button3Set;
    private boolean button3Hold;
    private boolean button4Set;
    private boolean button4Hold;
    private boolean buttonColorSet = false;
    private boolean buttonColorHold = false;
    private int showNextObject = 0;
    private int textureIndex = 0;


    public VirtualButtonRenderer(VirtualButtons activity,
                                 SampleApplicationSession session)
    {
        mActivity = activity;
        vuforiaAppSession = session;
    }


    // Called when the surface is created or recreated.
    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceCreated");

        // Call function to initialize rendering:
        initRendering();

        // Call Vuforia function to (re)initialize rendering after first use
        // or after OpenGL ES context was lost (e.g. after onPause/onResume):
        vuforiaAppSession.onSurfaceCreated();
    }


    // Called when the surface changed size.
    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height)
    {
        Log.d(LOGTAG, "GLRenderer.onSurfaceChanged");

        // Call Vuforia function to handle render surface size changes:
        vuforiaAppSession.onSurfaceChanged(width, height);
    }


    // Called to draw the current frame.
    @Override
    public void onDrawFrame(GL10 gl)
    {
        if (!mIsActive)
            return;

        // Call our function to render content
        renderFrame();
    }


    private void initRendering()
    {
        Log.d(LOGTAG, "VirtualButtonsRenderer.initRendering");

        // Define clear color
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, Vuforia.requiresAlpha() ? 0.0f
                : 1.0f);

        // Now generate the OpenGL texture objects and add settings
        for (Texture t : mTextures)
        {
            GLES20.glGenTextures(1, t.mTextureID, 0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t.mTextureID[0]);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA,
                    t.mWidth, t.mHeight, 0, GLES20.GL_RGBA,
                    GLES20.GL_UNSIGNED_BYTE, t.mData);
        }

        shaderProgramID = SampleUtils.createProgramFromShaderSrc(
                CubeShaders.CUBE_MESH_VERTEX_SHADER,
                CubeShaders.CUBE_MESH_FRAGMENT_SHADER);

        vertexHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexPosition");
        normalHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexNormal");
        textureCoordHandle = GLES20.glGetAttribLocation(shaderProgramID,
                "vertexTexCoord");
        mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "modelViewProjectionMatrix");
        texSampler2DHandle = GLES20.glGetUniformLocation(shaderProgramID,
                "texSampler2D");

        // OpenGL setup for Virtual Buttons
        vbShaderProgramID = SampleUtils.createProgramFromShaderSrc(
                LineShaders.LINE_VERTEX_SHADER, LineShaders.LINE_FRAGMENT_SHADER);

        mvpMatrixButtonsHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "modelViewProjectionMatrix");
        vbVertexHandle = GLES20.glGetAttribLocation(vbShaderProgramID,
                "vertexPosition");
        lineOpacityHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "opacity");
        lineColorHandle = GLES20.glGetUniformLocation(vbShaderProgramID,
                "color");
    }


    private void renderFrame()
    {
        // Clear color and depth buffer
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

        // Get the state from Vuforia and mark the beginning of a rendering
        // section
        State state = Renderer.getInstance().begin();

        // Explicitly render the Video Background
        Renderer.getInstance().drawVideoBackground();

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        // We must detect if background reflection is active and adjust the
        // culling direction.
        // If the reflection is active, this means the post matrix has been
        // reflected as well,
        // therefore counter standard clockwise face culling will result in
        // "inside out" models.
        GLES20.glEnable(GLES20.GL_CULL_FACE);
        GLES20.glCullFace(GLES20.GL_BACK);
        if (Renderer.getInstance().getVideoBackgroundConfig().getReflection() == VIDEO_BACKGROUND_REFLECTION.VIDEO_BACKGROUND_REFLECTION_ON)
            GLES20.glFrontFace(GLES20.GL_CW); // Front camera
        else
            GLES20.glFrontFace(GLES20.GL_CCW); // Back camera

        // Set the viewport
        int[] viewport = vuforiaAppSession.getViewport();
        GLES20.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);

        // Did we find any trackables this frame?
        if (state.getNumTrackableResults() > 0)
        {
            // Get the trackable:
            TrackableResult trackableResult = state.getTrackableResult(0);
            float[] modelViewMatrix = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();

            // CHANGE
            /*
            Matrix.translateM(modelViewMatrix, 0, -105.0f, 278.0f,
                    3.0f);
            Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);*/

            // The image target specific result:
            assert (trackableResult.getType() == ImageTargetResult
                    .getClassType());
            ImageTargetResult imageTargetResult = (ImageTargetResult) trackableResult;

            // Set transformations:
            float[] modelViewProjection = new float[16];
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            // END OF CHANGE


            //modelViewMatrix for FirstSegment
            /*float[] modelViewMatrix2 = Tool.convertPose2GLMatrix(
                    trackableResult.getPose()).getData();

            Matrix.translateM(modelViewMatrix2, 0, 0.0f, 0.0f,
                    3.0f);
            Matrix.rotateM(modelViewMatrix2, 0, 0.0f, 1.0f, 0.0f, 0.0f);

            float[] modelViewProjection2 = new float[16];
            Matrix.multiplyMM(modelViewProjection2, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewMatrix2, 0);*/
            //end FirstSegment


            // Set the texture used for the teapot model:

            float vbVertices[] = new float[imageTargetResult
                    .getNumVirtualButtons() * 24];
            short vbCounter = 0;

            // Iterate through this targets virtual buttons:
            for (int i = 0; i < imageTargetResult.getNumVirtualButtons(); ++i)
            {
                VirtualButtonResult buttonResult = imageTargetResult
                        .getVirtualButtonResult(i);
                VirtualButton button = buttonResult.getVirtualButton();

                if(buttonColorSet == false && buttonColorHold == false){
                    if(i==2 && buttonResult.isPressed()){
                        buttonColorSet = true;
                        textureIndex = 1;
                    }
                }

                if(buttonColorSet == true && buttonColorHold == false){
                    if(i==2 && !buttonResult.isPressed()){
                        buttonColorSet = false;
                        buttonColorHold = true;
                    }
                }

                if(buttonColorSet == false && buttonColorHold == true && textureIndex == 1)
                {
                    if(i==2 && buttonResult.isPressed()){
                        buttonColorSet = true;
                        buttonColorHold = false;
                        textureIndex = 2;
                    }
                }

                if(buttonColorSet == false && buttonColorHold == true && textureIndex == 2) {
                    if (i == 2 && buttonResult.isPressed()) {
                        buttonColorSet = true;
                        buttonColorHold = false;
                        textureIndex = 3;
                    }
                }

                if(buttonColorSet == false && buttonColorHold == true && textureIndex == 3) {
                    if (i == 2 && buttonResult.isPressed()) {
                        buttonColorSet = true;
                        buttonColorHold = false;
                        textureIndex = 4;
                    }
                }

                if(buttonColorSet == false && buttonColorHold == true && textureIndex == 4) {
                    if (i == 2 && buttonResult.isPressed()) {
                        buttonColorSet = true;
                        buttonColorHold = false;
                        textureIndex = 1;
                    }
                }

                int buttonIndex = 0;
                // Run through button name array to find button index
                for (int j = 0; j < VirtualButtons.NUM_BUTTONS; ++j)
                {
                    if (button.getName().compareTo(
                            mActivity.virtualButtonColors[j]) == 0)
                    {
                        buttonIndex = j;
                        break;
                    }
                }

                Area vbArea = button.getArea();
                assert (vbArea.getType() == Area.TYPE.RECTANGLE);
                Rectangle vbRectangle[] = new Rectangle[4];
                vbRectangle[0] = new Rectangle(-108.68f, -53.52f, -75.75f,
                        -65.87f);
                vbRectangle[1] = new Rectangle(-45.28f, -53.52f, -12.35f,
                        -65.87f);
                vbRectangle[2] = new Rectangle(14.82f, -53.52f, 47.75f, -65.87f);
                vbRectangle[3] = new Rectangle(76.57f, -53.52f, 109.50f,
                        -65.87f);

                // We add the vertices to a common array in order to have one
                // single
                // draw call. This is more efficient than having multiple
                // glDrawArray calls
                vbVertices[vbCounter] = vbRectangle[buttonIndex].getLeftTopX();
                vbVertices[vbCounter + 1] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 2] = 0.0f;
                vbVertices[vbCounter + 3] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 4] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 5] = 0.0f;
                vbVertices[vbCounter + 6] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 7] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 8] = 0.0f;
                vbVertices[vbCounter + 9] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 10] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 11] = 0.0f;
                vbVertices[vbCounter + 12] = vbRectangle[buttonIndex]
                        .getRightBottomX();
                vbVertices[vbCounter + 13] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 14] = 0.0f;
                vbVertices[vbCounter + 15] = vbRectangle[buttonIndex]
                        .getLeftTopX();
                vbVertices[vbCounter + 16] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 17] = 0.0f;
                vbVertices[vbCounter + 18] = vbRectangle[buttonIndex]
                        .getLeftTopX();
                vbVertices[vbCounter + 19] = vbRectangle[buttonIndex]
                        .getRightBottomY();
                vbVertices[vbCounter + 20] = 0.0f;
                vbVertices[vbCounter + 21] = vbRectangle[buttonIndex]
                        .getLeftTopX();
                vbVertices[vbCounter + 22] = vbRectangle[buttonIndex]
                        .getLeftTopY();
                vbVertices[vbCounter + 23] = 0.0f;
                vbCounter += 24;

            }

            // We only render if there is something on the array
            if (vbCounter > 0)
            {
                // Render frame around button
                GLES20.glUseProgram(vbShaderProgramID);

                GLES20.glVertexAttribPointer(vbVertexHandle, 3,
                        GLES20.GL_FLOAT, false, 0, fillBuffer(vbVertices));

                GLES20.glEnableVertexAttribArray(vbVertexHandle);

                GLES20.glUniform1f(lineOpacityHandle, 1.0f);
                GLES20.glUniform3f(lineColorHandle, 1.0f, 1.0f, 1.0f);

                GLES20.glUniformMatrix4fv(mvpMatrixButtonsHandle, 1, false,
                        modelViewProjection, 0);

                // We multiply by 8 because that's the number of vertices per
                // button
                // The reason is that GL_LINES considers only pairs. So some
                // vertices
                // must be repeated.
                GLES20.glDrawArrays(GLES20.GL_LINES, 0,
                        imageTargetResult.getNumVirtualButtons() * 8);

                SampleUtils.checkGLError("VirtualButtons drawButton");

                GLES20.glDisableVertexAttribArray(vbVertexHandle);
            }


            // CHANGE
            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f, 1.0f); // -105.0f, 278.0f, 3.0f
            Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f); //

            // The image target specific result:
            assert (trackableResult.getType() == ImageTargetResult
                    .getClassType());
            imageTargetResult = (ImageTargetResult) trackableResult;

            // Set transformations:
            modelViewProjection = new float[16];
            Matrix.multiplyMM(modelViewProjection, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewMatrix, 0);
            // END OF CHANGE

            // Assumptions:
            assert (textureIndex < mTextures.size());
            Texture thisTexture = mTextures.get(textureIndex);

            // Scale 3D model
            float[] modelViewScaled = modelViewMatrix;
            Matrix.scaleM(modelViewScaled, 0, kTeapotScale, kTeapotScale,
                    kTeapotScale);

            float[] modelViewProjectionScaled = new float[16];
            Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

            // Render 3D model
            GLES20.glUseProgram(shaderProgramID);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mTeapot.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mTeapot.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, mTeapot.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    thisTexture.mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjectionScaled, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                    mTeapot.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                    mTeapot.getIndices());

            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

            //Button 1 and 2 will add and remove object to current robot
            VirtualButtonResult buttonResult1 = imageTargetResult.getVirtualButtonResult(0);
            VirtualButtonResult buttonResult2 = imageTargetResult.getVirtualButtonResult(1);


            //Neues Segment hinzufuegen
            if(button2Set == false && button2Hold == false){
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    showNextObject = 0;
                }
            }

            if(button2Set == true && button2Hold == false){
                if (!buttonResult2.isPressed()) {
                    button2Set = false;
                    button2Hold = true;
                }
            }


            if(button2Set == false && button2Hold == true && showNextObject == 0)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 1;
                }
            }

            if(button2Set == false && button2Hold == true && showNextObject == 1)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 2;
                }
            }

            if(button2Set == false && button2Hold == true && showNextObject == 2)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 3;
                }
            }
            if(button2Set == false && button2Hold == true && showNextObject == 3)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 4;
                }
            }

            if(button2Set == false && button2Hold == true && showNextObject == 4)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 5;
                }
            }

            if(button2Set == false && button2Hold == true && showNextObject == 5)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 6;
                }
            }

            if(button2Set == false && button2Hold == true && showNextObject == 6)
            {
                if (buttonResult2.isPressed()) {
                    button2Set = true;
                    button2Hold = false;
                    showNextObject = 7;
                }
            }

            //Letztes Segment entfernen
            if(button1Set == false && button1Hold == false){
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                }
            }

            if(button1Set == true && button1Hold == false){
                if (!buttonResult1.isPressed()) {
                    button1Set = false;
                    button1Hold = true;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 1)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 0;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 2)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 1;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 3)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 2;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 4)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 3;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 5)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 4;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 6)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 5;
                }
            }

            if(button1Set == false && button1Hold == true && showNextObject == 7)
            {
                if (buttonResult1.isPressed()) {
                    button1Set = true;
                    button1Hold = false;
                    showNextObject = 6;
                }
            }

            if(showNextObject == 1) {
            assert (textureIndex < mTextures.size());
            thisTexture = mTextures.get(textureIndex);

            Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                    100.0f);

            Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f); // Achtung


            VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

            if (buttonResult4.isPressed() == true){
                animateObject(modelViewMatrix);
                    /*if (buttonResult4.isPressed() == false) {
                        animateStopObject(modelViewMatrix);
                    }*/
            }

                /*if (buttonResult4.isPressed() == false) {
                    animateStopObject(modelViewMatrix);
                }*/


            // First Segment
            //Matrix.scaleM(modelViewScaled, 0, kFirstSegmentScale, kFirstSegmentScale, kFirstSegmentScale);
            Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                    .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

            GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mFirstSegment.getVertices());
            GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                    false, 0, mFirstSegment.getNormals());
            GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                    GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

            GLES20.glEnableVertexAttribArray(vertexHandle);
            GLES20.glEnableVertexAttribArray(normalHandle);
            GLES20.glEnableVertexAttribArray(textureCoordHandle);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                    thisTexture.mTextureID[0]);
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                    modelViewProjectionScaled, 0);
            GLES20.glUniform1i(texSampler2DHandle, 0);
            GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                    mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                    mFirstSegment.getIndices());

            GLES20.glDisableVertexAttribArray(vertexHandle);
            GLES20.glDisableVertexAttribArray(normalHandle);
            GLES20.glDisableVertexAttribArray(textureCoordHandle);

        }

            if(showNextObject == 2) {
                assert (textureIndex < mTextures.size());
                thisTexture = mTextures.get(textureIndex);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        100.0f);

                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

                //animateStopObject(modelViewMatrix);

                if (buttonResult4.isPressed() == true){
                    animateObject(modelViewMatrix);
                }

                /*if (buttonResult4.isPressed() == false) {
                    animateStopObject(modelViewMatrix);
                }*/



                // First Segment --------------------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFirstSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);



                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        90.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Second Object---------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSecondSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSecondSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSecondSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);


            }


            if(showNextObject == 3) {
                assert (textureIndex < mTextures.size());
                thisTexture = mTextures.get(textureIndex);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        100.0f);

                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

                if (buttonResult4.isPressed() == true){
                    animateObject(modelViewMatrix);
                }

                // First Segment--------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFirstSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        90.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Second Object---------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSecondSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSecondSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSecondSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Third Object---------------------------------------------------------------------------
                //Matrix.scaleM(modelViewScaled, 0, kThirdSegmentScale, kThirdSegmentScale, kThirdSegmentScale);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mThirdSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mThirdSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mThirdSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            }

            if(showNextObject == 4) {
                assert (textureIndex < mTextures.size());
                thisTexture = mTextures.get(textureIndex);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        100.0f);

                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

                if (buttonResult4.isPressed() == true){
                    animateObject(modelViewMatrix);
                }

                // First Segment--------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFirstSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        90.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Second Object---------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSecondSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSecondSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSecondSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Third Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mThirdSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mThirdSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mThirdSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fourth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFourthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFourthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFourthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            }
            if(showNextObject == 5) {
                assert (textureIndex < mTextures.size());
                thisTexture = mTextures.get(textureIndex);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        100.0f);

                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

                if (buttonResult4.isPressed() == true){
                    animateObject(modelViewMatrix);
                }

                // First Segment--------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFirstSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        90.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Second Object---------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSecondSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSecondSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSecondSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Third Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mThirdSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mThirdSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mThirdSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fourth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFourthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFourthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFourthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fifth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFifthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFifthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFifthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFifthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFifthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            }
            if(showNextObject == 6) {
                assert (textureIndex < mTextures.size());
                thisTexture = mTextures.get(textureIndex);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        100.0f);

                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

                if (buttonResult4.isPressed() == true){
                    animateObject(modelViewMatrix);
                }

                // First Segment--------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFirstSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        90.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Second Object---------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSecondSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSecondSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSecondSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Third Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mThirdSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mThirdSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mThirdSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fourth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFourthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFourthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFourthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fifth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFifthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFifthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFifthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFifthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFifthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 12.0f,
                        100.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Sixth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSixthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSixthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSixthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSixthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSixthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);
            }

            if(showNextObject == 7) {
                assert (textureIndex < mTextures.size());
                thisTexture = mTextures.get(textureIndex);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        110.0f);

                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                VirtualButtonResult buttonResult4 = imageTargetResult.getVirtualButtonResult(3);

                if (buttonResult4.isPressed() == true){
                    animateObject(modelViewMatrix);
                }

                // First Segment--------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFirstSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFirstSegment.getTexCoords());

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);

                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFirstSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFirstSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        90.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Second Object---------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSecondSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSecondSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSecondSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSecondSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Third Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mThirdSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mThirdSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mThirdSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mThirdSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fourth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFourthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFourthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFourthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFourthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        95.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Fifth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFifthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mFifthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mFifthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mFifthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mFifthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 12.0f,
                        100.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Sixth Object---------------------------------------------------------------------------
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSixthSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSixthSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSixthSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSixthSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSixthSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

                Matrix.translateM(modelViewMatrix, 0, 0.0f, 0.0f,
                        50.0f);
                Matrix.rotateM(modelViewMatrix, 0, 0.0f, 1.0f, 0.0f, 0.0f);

                // Seventh Object---------------------------------------------------------------------------
                Matrix.scaleM(modelViewScaled, 0, kSeventhSegmentScale, kSeventhSegmentScale,
                        kSeventhSegmentScale);
                Matrix.multiplyMM(modelViewProjectionScaled, 0, vuforiaAppSession
                        .getProjectionMatrix().getData(), 0, modelViewScaled, 0);

                GLES20.glEnableVertexAttribArray(vertexHandle);
                GLES20.glEnableVertexAttribArray(normalHandle);
                GLES20.glEnableVertexAttribArray(textureCoordHandle);


                GLES20.glVertexAttribPointer(vertexHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSeventhSegment.getVertices());
                GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT,
                        false, 0, mSeventhSegment.getNormals());
                GLES20.glVertexAttribPointer(textureCoordHandle, 2,
                        GLES20.GL_FLOAT, false, 0, mSeventhSegment.getTexCoords());


                GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
                GLES20.glBindTexture(GLES20.GL_TEXTURE_2D,
                        thisTexture.mTextureID[0]);
                GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false,
                        modelViewProjectionScaled, 0);
                GLES20.glUniform1i(texSampler2DHandle, 0);
                GLES20.glDrawElements(GLES20.GL_TRIANGLES,
                        mSeventhSegment.getNumObjectIndex(), GLES20.GL_UNSIGNED_SHORT,
                        mSeventhSegment.getIndices());

                GLES20.glDisableVertexAttribArray(vertexHandle);
                GLES20.glDisableVertexAttribArray(normalHandle);
                GLES20.glDisableVertexAttribArray(textureCoordHandle);

            }

            SampleUtils.checkGLError("VirtualButtons renderFrame");

        }

        GLES20.glDisable(GLES20.GL_DEPTH_TEST);

        Renderer.getInstance().end();

    }

    private void animateObject(float[] modelViewMatrix)
    {
        //double time = System.currentTimeMillis();             // Get real time difference
        //float dt = (float) (time - prevTime) / 1000;          // from frame to frame

        rotateBallAngle += 18.0f / 3.1415f;     // Animate angle based on time
        rotateBallAngle %= 360;

        Matrix.rotateM(modelViewMatrix, 0, rotateBallAngle, 0.0f, 0.0f, 1.0f);

        //prevTime = time;
    }

    private Buffer fillBuffer(float[] array)
    {
        // Convert to floats because OpenGL doesnt work on doubles, and manually
        // casting each input value would take too much time.
        ByteBuffer bb = ByteBuffer.allocateDirect(4 * array.length); // each
        // float
        // takes 4
        // bytes
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (float d : array)
            bb.putFloat(d);
        bb.rewind();

        return bb;

    }


    public void setTextures(Vector<Texture> textures)
    {
        mTextures = textures;

    }

}