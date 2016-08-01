package com.revesoft.camerapreview;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {
    Button ButtonClick;
    int CAMERA_PIC_REQUEST = 1337;
    private Camera camera;
    SurfaceView surfaceView;
    SurfaceHolder surfaceholder;
    Preview mPreview;
    int genframecounter = 0;
    BlockingQueue<byte[]> dataQueue = new ArrayBlockingQueue<byte[]>(100);
    volatile boolean shouldCapture = true;


    SurfaceView mSurfaceView;
    SurfaceHolder mHolder;
    private int videoWidth = 320;
    private int videoHeight = 240;
    private int fps = 20;
    ImageView image;

    MediaCodec mMediaCodec;
    MediaFormat mediaFormat;
    ByteBuffer[] inputBuffers;
    ByteBuffer[] outputBuffers;



    Camera.PreviewCallback cb = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (shouldCapture) {
                genframecounter++;
                dataQueue.offer(data);
                try {
                    Log.d("generated frame", "preview frame count "
                            + genframecounter + " data length: " + data.length +" data :"+new String(data,"UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
                //  shouldCapture = false;
            }
            Log.i("Data :","Running..  onPreviewFrame ");
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
      //  mPreview = new Preview(this);
       // ButtonClick = (Button) findViewById(R.id.Camera);
        /*try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                mMediaCodec = MediaCodec.createEncoderByType("video/avc");
                mediaFormat = MediaFormat.createVideoFormat("video/avc",
                        320,
                        240);
                mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000);
                mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15);
                mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
                mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
                mMediaCodec.configure(mediaFormat,
                        null,
                        null,
                        MediaCodec.CONFIGURE_FLAG_ENCODE);
                mMediaCodec.start();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }*/


        lock_screen();
        FrameLayout outputImage = (FrameLayout) findViewById(R.id.image_output);
        image = new ImageView(MainActivity.this);
        float density = getResources().getDisplayMetrics().density;
        final FrameLayout.LayoutParams pms = new FrameLayout.LayoutParams(
                (int) (videoWidth * density), (int) (videoHeight * density));
        pms.gravity = Gravity.TOP;
        image.setLayoutParams(pms);
      //  outputImage.addView(image);
        FrameLayout preview = (FrameLayout) findViewById(R.id.camera_preview);
        surfaceView = new SurfaceView(this);
        surfaceView.setLayoutParams(pms);
        surfaceholder = surfaceView.getHolder();
        surfaceholder.addCallback(this);
        surfaceholder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        preview.addView(surfaceView);

    }


    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        Log.d("debug", "surface created called");
        try {
            camera = getCameraInstance();// Camera.open(camId);
            if (camera != null) {
                try {
                    parameters();
                    // initiate_ffmpeg();
                    // setCameraDisplayOrientation(this,camera,camId);
                    camera.setDisplayOrientation(setCameraDisplayOrientation(this, Camera.CameraInfo.CAMERA_FACING_FRONT));
                    camera.setPreviewDisplay(surfaceHolder);
                    camera.startPreview();
                    Log.i("Data :","Running..  created ");
                    camera.setPreviewCallback(cb);
                   /* running = true;
                    encoder.start();
                    decoder.start();*/
                } catch (Exception e) {
                    Log.d("Debug", "start preview " + e.getMessage());
                }
            }
        } catch (Exception e) {
            Log.d("Debug", "cant open camera" + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int i, int i1, int i2) {
        Log.d("debug", "surface changed called");
        try {
            // setCameraDisplayOrientation(this,camera,camId);
            camera.setPreviewDisplay(surfaceHolder);
            camera.startPreview();
            camera.setPreviewCallback(cb);
           // Log.i("Data :",cb.toString());
            Log.i("Data :","Running..  changed ");
        } catch (Exception e) {
            Log.d("DEBUG", "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null); // Preview callback of camera must
            // be set null when camera is
            // stopped
            camera.release();
            camera = null;
        }
    }

    public Camera getCameraInstance() {
        Camera c = null;
        try {
            c = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT); // attempt to get a Camera instance
        } catch (Exception e) {
            Log.d("debug", "cant open camera " + e.getMessage());
        }
        return c; // returns null if camera is unavailable
    }

    public int setCameraDisplayOrientation(Activity activity, int id) {
        int rotation = activity.getWindowManager().getDefaultDisplay()
                .getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(id, info);
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            result = (info.orientation - degrees + 360) % 360;
        }
        return result;
    }
    private void camerastop() {
        /*ffmpegInterface.closeencoder();
        ffmpegInterface.closedecoder();*/
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null); // Preview callback of camera must
            // be set null when camera is
            // stopped
            camera.release();
            camera = null;
        }
    }

    public void parameters() {
        Camera.Parameters parameters = camera.getParameters();
        try {
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                parameters.set("orientation", "portrait");
                parameters.set("rotation", 90);
            }
            if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                parameters.set("orientation", "landscape");
                parameters.set("rotation", 90);
            }
            parameters.setPreviewSize(videoWidth, videoHeight);
            parameters.setPreviewFormat(ImageFormat.YV12);
            parameters.setPreviewFrameRate(fps);
            // parameters.setPreviewFpsRange(10000, 15000);
            camera.setParameters(parameters);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    private void lock_screen() {
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        }
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        camerastop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        camerastop();
    }
}
