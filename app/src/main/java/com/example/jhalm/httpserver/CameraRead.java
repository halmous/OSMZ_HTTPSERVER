package com.example.jhalm.httpserver;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceView;
import android.widget.FrameLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.Semaphore;

import static android.provider.MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE;

public class CameraRead
{
    private Context context;
    private Camera camera = null;
    private boolean run = false;
    //private boolean takePicture = false;
    private SurfaceTexture texture;
    private Semaphore jpegChange;
    private Semaphore takePicture;

    private Camera.PictureCallback pictureCallbackToFile = new Camera.PictureCallback()
    {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            //texture.release();
            File pictureFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/public_html/img/snap.jpg");
            if (pictureFile == null){
                Log.d("SERVER", "Error creating media file, check storage permissions");
                return;
            }

            try {
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Log.d("SERVER", "File created");
            } catch (FileNotFoundException e) {
                Log.d("SERVER", "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d("SERVER", "Error accessing file: " + e.getMessage());
            }
        }
    };

    private Camera.PictureCallback pictureCallbackToArray = new Camera.PictureCallback()
    {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            try
            {
                jpegChange.acquire();
                jpegData = data;
                jpegChange.release();
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    };

    private byte[] jpegData;

    private Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] bytes, Camera camera)
        {
            takePicture.release();
        }
    };

    private Camera.ErrorCallback errorCallback = new Camera.ErrorCallback() {
        @Override
        public void onError(int i, Camera camera) {
            //Log.d("SERVER", "Cam error: " + i);
        }
    };

    public CameraRead(Context context)
    {
        this.context = context;
    }

    public byte[] getJpegData()
    {
        byte[] ret = null;

        try
        {
            jpegChange.acquire();
            ret = jpegData;
            jpegChange.release();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        return ret;
    }

    public boolean getCamera()
    {
        if(context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA) && camera == null)
        {
            try
            {
                camera = Camera.open(0);
                camera.release();
                camera = Camera.open(0);
                camera.stopPreview();

                if(camera == null)
                    return false;

                Camera.Parameters parameters = camera.getParameters();
                parameters.setPictureSize(1920, 1080);
                //parameters.setPreviewFpsRange(10, 16);
                parameters.setRotation(90);
                parameters.setJpegQuality(100);
                parameters.setVideoStabilization(false);

                parameters.setColorEffect(Camera.Parameters.EFFECT_NONE);

                if ( parameters.getAntibanding() != null) {
                    parameters.setAntibanding(Camera.Parameters.ANTIBANDING_OFF);
                }

                if ( parameters.getFlashMode() != null) {
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                }

                //set focus mode
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);

                camera.setParameters(parameters);
                camera.setDisplayOrientation(90);
                texture = new SurfaceTexture(100);
                camera.setPreviewTexture(texture);
                camera.setPreviewCallback(previewCallback);
                camera.setErrorCallback(errorCallback);
                takePicture = new Semaphore(1);
                camera.startPreview();

                return true;
            }
            catch (Exception e)
            {
                e.printStackTrace();
                return false;
            }
        }
        else
        {
            return false;
        }
    }

    public void releaseCamera()
    {
        run = false;
        takePicture.release();

        if(this.camera != null)
            this.camera.release();
    }

    public void startTakeFileSnapshots()
    {
        run = true;
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                while(run) {
                    try {
                        takePicture.tryAcquire();
                        camera.startPreview();
                        //camera.startPreview();
                        takePicture.acquire();
                        camera.takePicture(null, null, pictureCallbackToFile);
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        new Thread(runnable).start();
    }

    public void startTakeArraySnapshots()
    {
        run = true;
        Runnable runnable = new Runnable() {
            @Override
            public void run()
            {
                if(jpegChange == null)
                    jpegChange = new Semaphore(1);

                while(run) {
                    try {
                        takePicture.tryAcquire();
                        camera.startPreview();
                        takePicture.acquire();
                        //camera.startPreview();
                        camera.takePicture(null, null, pictureCallbackToArray);
                        Thread.sleep(50);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                jpegChange.release();
                jpegChange = null;
            }
        };
        new Thread(runnable).start();
    }
}
