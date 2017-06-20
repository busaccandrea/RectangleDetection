package pavel11.rectangledetection.views;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
//_______________________________________________________________________________________________
//_______________________________________________________________________________________________
//import pt2
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
//import android.support.annotation.RequiresApi;
//import android.support.annotation.Size;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.jar.Manifest;

@SuppressWarnings("deprecation")
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    private static final String TAG = CameraPreview.class.getSimpleName();
    private static final String CAMERA_PARAM_ORIENTATION = "orientation";
    private static final String CAMERA_PARAM_LANDSCAPE = "landscape";
    private static final String CAMERA_PARAM_PORTRAIT = "portrait";
    private SurfaceHolder mHolder;
    private Camera mCamera;
    private boolean mInProgress;
    private boolean mInFocus;
    private int mCameraId = 0;

    private LayoutMode mLayoutMode;
    private int mCenterPosX = -1;
    private int mCenterPosY;
    protected List<Camera.Size> mPreviewSizeList;
    protected List<Camera.Size> mPictureSizeList;
    private int mSurfaceChangedCallDepth = 0;
    protected boolean mSurfaceConfiguring = false;
    protected Camera.Size mPreviewSize;
    protected Camera.Size mPictureSize;
    private Camera.PreviewCallback callback;

    public void setCallback(Camera.PreviewCallback callback) {
        this.callback = callback;
    }

    public static enum LayoutMode {
        FitToParent, // Scale to the size that no side is larger than the parent
        NoBlank // Scale to the size that no side is smaller than the parent
    }

    public CameraPreview(Context context, AttributeSet attrs) {
        super(context, attrs);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mLayoutMode = LayoutMode.NoBlank;
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        int cameraId = 0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            if (Camera.getNumberOfCameras() > cameraId) {
                mCameraId = cameraId;
            } else {
                mCameraId = 0;
            }
        } else {
            mCameraId = 0;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
            mCamera = Camera.open(mCameraId);
        } else {
            mCamera = Camera.open();
        }
        Camera.Parameters cameraParams = mCamera.getParameters();
        mPreviewSizeList = cameraParams.getSupportedPreviewSizes();
        mPictureSizeList = cameraParams.getSupportedPictureSizes();
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(mHolder);
                mCamera.setPreviewCallback(callback);
            }
        } catch (IOException e) {
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mCamera != null) {
            mSurfaceChangedCallDepth++;
            doSurfaceChanged(width, height);
            mSurfaceChangedCallDepth--;
        }
    }

    private void doSurfaceChanged(int width, int height) {
        mCamera.stopPreview();

        Camera.Parameters cameraParams = mCamera.getParameters();
        boolean portrait = true;

        // The code in this if-statement is prevented from executed again when surfaceChanged is
        // called again due to the change of the layout size in this if-statement.
        if (!mSurfaceConfiguring) {
            Camera.Size previewSize = determinePreviewSize(portrait, width, height);
            Camera.Size pictureSize = determinePictureSize(previewSize);
            Log.v(TAG, "Desired Preview Size - w: " + width + ", h: " + height);
            mPreviewSize = previewSize;
            mPictureSize = pictureSize;
            mSurfaceConfiguring = adjustSurfaceLayoutSize(previewSize, portrait, width, height);
            // Continue executing this method if this method is called recursively.
            // Recursive call of surfaceChanged is very special case, which is a path from
            // the catch clause at the end of this method.
            // The later part of this method should be executed as well in the recursive
            // invocation of this method, because the layout change made in this recursive
            // call will not trigger another invocation of this method.
            if (mSurfaceConfiguring && (mSurfaceChangedCallDepth <= 1)) {
                return;
            }
        }

        configureCameraParameters(cameraParams, portrait);
        mSurfaceConfiguring = false;

        try {
            byte[] previewBuffer = new byte[(mPreviewSize.height * mPreviewSize.width * 3) / 2];
            mCamera.addCallbackBuffer(previewBuffer);
            mCamera.setPreviewCallback(callback);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.w(TAG, "Failed to start preview: " + e.getMessage());

            // Remove failed size
            mPreviewSizeList.remove(mPreviewSize);
            mPreviewSize = null;

            // Reconfigure
            if (mPreviewSizeList.size() > 0) { // prevent infinite loop
                surfaceChanged(null, 0, width, height);
            } else {
                Log.w(TAG, "Gave up starting preview");
            }
        }

    }

    protected Camera.Size determinePreviewSize(boolean portrait, int reqWidth, int reqHeight) {
        // Meaning of width and height is switched for preview when portrait,
        // while it is the same as user's view for surface and metrics.
        // That is, width must always be larger than height for setPreviewSize.
        int reqPreviewWidth; // requested width in terms of camera hardware
        int reqPreviewHeight; // requested height in terms of camera hardware
        if (portrait) {
            reqPreviewWidth = reqHeight;
            reqPreviewHeight = reqWidth;
        } else {
            reqPreviewWidth = reqWidth;
            reqPreviewHeight = reqHeight;
        }

        Log.v(TAG, "Listing all supported preview sizes");
        for (Camera.Size size : mPreviewSizeList) {
            Log.v(TAG, "  w: " + size.width + ", h: " + size.height);
        }
        Log.v(TAG, "Listing all supported picture sizes");
        for (Camera.Size size : mPictureSizeList) {
            Log.v(TAG, "  w: " + size.width + ", h: " + size.height);
        }

        // Adjust surface size with the closest aspect-ratio
        float reqRatio = ((float) reqPreviewWidth) / reqPreviewHeight;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Camera.Size retSize = null;
        for (Camera.Size size : mPreviewSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    protected Camera.Size determinePictureSize(Camera.Size previewSize) {
        Camera.Size retSize = null;
        for (Camera.Size size : mPictureSizeList) {
            if (size.equals(previewSize)) {
                return size;
            }
        }

        Log.v(TAG, "Same picture size not found.");

        // if the preview size is not supported as a picture size
        float reqRatio = ((float) previewSize.width) / previewSize.height;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        for (Camera.Size size : mPictureSizeList) {
            curRatio = ((float) size.width) / size.height;
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }

        return retSize;
    }

    protected boolean adjustSurfaceLayoutSize(Camera.Size previewSize, boolean portrait,
                                              int availableWidth, int availableHeight) {
        float tmpLayoutHeight, tmpLayoutWidth;
        if (portrait) {
            tmpLayoutHeight = previewSize.width;
            tmpLayoutWidth = previewSize.height;
        } else {
            tmpLayoutHeight = previewSize.height;
            tmpLayoutWidth = previewSize.width;
        }

        float factH, factW, fact;
        factH = availableHeight / tmpLayoutHeight;
        factW = availableWidth / tmpLayoutWidth;
        if (mLayoutMode == LayoutMode.FitToParent) {
            // Select smaller factor, because the surface cannot be set to the size larger than display metrics.
            if (factH < factW) {
                fact = factH;
            } else {
                fact = factW;
            }
        } else {
            if (factH < factW) {
                fact = factW;
            } else {
                fact = factH;
            }
        }

        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) this.getLayoutParams();

        int layoutHeight = (int) (tmpLayoutHeight * fact);
        int layoutWidth = (int) (tmpLayoutWidth * fact);
        Log.v(TAG, "Preview Layout Size - w: " + layoutWidth + ", h: " + layoutHeight);
        Log.v(TAG, "Scale factor: " + fact);

        boolean layoutChanged;
        if ((layoutWidth != this.getWidth()) || (layoutHeight != this.getHeight())) {
            layoutParams.height = layoutHeight;
            layoutParams.width = layoutWidth;
            if (mCenterPosX >= 0) {
                layoutParams.topMargin = mCenterPosY - (layoutHeight / 2);
                layoutParams.leftMargin = mCenterPosX - (layoutWidth / 2);
            }
            this.setLayoutParams(layoutParams); // this will trigger another surfaceChanged invocation.
            layoutChanged = true;
        } else {
            layoutChanged = false;
        }

        return layoutChanged;
    }

    /**
     * @param x X coordinate of center position on the screen. Set to negative value to unset.
     * @param y Y coordinate of center position on the screen.
     */
    public void setCenterPosition(int x, int y) {
        mCenterPosX = x;
        mCenterPosY = y;
    }

    protected void configureCameraParameters(Camera.Parameters cameraParams, boolean portrait) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.FROYO) { // for 2.1 and before
            if (portrait) {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_PORTRAIT);
            } else {
                cameraParams.set(CAMERA_PARAM_ORIENTATION, CAMERA_PARAM_LANDSCAPE);
            }
        } else { // for 2.2 and later
            int angle;
            Display display = ((Activity) getContext()).getWindowManager().getDefaultDisplay();
            switch (display.getRotation()) {
                case Surface.ROTATION_0: // This is display orientation
                    angle = 90; // This is camera orientation
                    break;
                case Surface.ROTATION_90:
                    angle = 0;
                    break;
                case Surface.ROTATION_180:
                    angle = 270;
                    break;
                case Surface.ROTATION_270:
                    angle = 180;
                    break;
                default:
                    angle = 90;
                    break;
            }
            Log.v(TAG, "angle: " + angle);
            mCamera.setDisplayOrientation(angle);
        }

        cameraParams.setPreviewSize(mPreviewSize.width, mPreviewSize.height);
        cameraParams.setPictureSize(mPictureSize.width, mPictureSize.height);
        Log.v(TAG, "Preview Actual Size - w: " + mPreviewSize.width + ", h: " + mPreviewSize.height);
        Log.v(TAG, "Picture Actual Size - w: " + mPictureSize.width + ", h: " + mPictureSize.height);

        mCamera.setParameters(cameraParams);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stop();
    }

    public void stop() {
        if (null == mCamera) {
            return;
        }
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    public void focus() {
        if (mCamera != null && !mInProgress) {
            mInFocus = true;
            mCamera.autoFocus((success, camera) -> {
                camera.autoFocus(null);
                mInFocus = false;
            });
        }
    }

    public Camera getCamera(){

        return  this.mCamera;
    }



//  ----------------------------------------------------------------------------------------------------------------------
//  ----------------------------------------------------------------------------------------------------------------------
    private static final int REQUEST_CAMERA_PERMISSION_RESULT=0;
    private static final int STATE_PREVIEW=0;
    private static final int STATE_WAIT_LOCK=1;

    private int mCaptureState=STATE_PREVIEW;
    private TextureView mTextureView;
    private TextureView.SurfaceTextureListener mSurfaceTextureListener= new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            Toast.makeText(getApplicationContext(), "TetureView is available", Toast.LENGTH_SHORT).show();
            setupCamera(width, height);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };

    private CameraDevice mCameraDevice;
    private CameraDevice.StateCallback mCameraDeviceStateCallback= new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice=camera;

            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
            mCameraDevice=null;
        }

        @Override
        public void onError(CameraDevice camera,  int error) {
            camera.close();
            mCameraDevice=null;
        }
    };

    private HandlerThread mBackgroundHandlerThread;
    private Handler mBackgroundHandler;
    private android.util.Size mPreviewSize2;
    private int mTotalRotation;
    private CameraCaptureSession mPreviewCaptureSession;
    public CameraCaptureSession.CaptureCallback mPreviewCaptureCallback= new CameraCaptureSession.CaptureCallback() {

        //      qui scatto la foto
        public void process(CaptureResult captureResult){
            switch (mCaptureState){
                case STATE_PREVIEW:
                    break;
                case STATE_WAIT_LOCK:
                    mCaptureState=STATE_PREVIEW;
                    Integer afState=captureResult.get(CaptureResult.CONTROL_AF_STATE);
                    if (afState==CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
                            || afState==CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED){
                        Toast.makeText(getContext(), "Autofocus locked", Toast.LENGTH_SHORT).show();
                        startStillCameraRequest();
                    }
                    break;
            }
        }
        public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
            super.onCaptureCompleted(session, request, result);

            process(result);
        }
    };



    private ImageReader.OnImageAvailableListener mOnImageAvailableListener= new
            ImageReader.OnImageAvailableListener(){
                @Override
                public void onImageAvailable(ImageReader reader) {
                    mBackgroundHandler.post(new ImageSaver(reader.acquireLatestImage()));
                }
            };
    private class ImageSaver implements Runnable{
        private final Image mImage;
        public ImageSaver(Image image) {
            mImage=image;
        }

        @Override
        public void run() {
            ByteBuffer byteBuffer=mImage.getPlanes()[0].getBuffer();
            byte[] bytes=new byte[byteBuffer.remaining()];
            byteBuffer.get(bytes);

            FileOutputStream fileOutputStream=null;
            try {
                fileOutputStream=new FileOutputStream(mImageFileName);
                fileOutputStream.write(bytes);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                mImage.close();
                if (fileOutputStream!=null){
                    try {
                        fileOutputStream.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    private CaptureRequest.Builder mCaptureRequestBuilder;
    private String mCameraId2;
    private ImageButton mStillImageButton;

    //variabili per cartella
    private File mImageFolder;
    private String mImageFileName;



    private static SparseIntArray ORIENTATION=new SparseIntArray();
    static {
        ORIENTATION.append(Surface.ROTATION_0, 0);
        ORIENTATION.append(Surface.ROTATION_90, 90);
        ORIENTATION.append(Surface.ROTATION_180, 180);
        ORIENTATION.append(Surface.ROTATION_270, 270);

    }

    private static class CompareSizeByArea implements Comparator<android.util.Size> {
        @Override
        public int compare(android.util.Size o1, android.util.Size o2) {
            return Long.signum((long)o1.getHeight()*o1.getWidth()/
                    (long)o2.getWidth()*o2.getHeight());
        }
    }

//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_camera2);
//
//        createImageFolder();
//        mTextureView=(TextureView)findViewById(R.id.textureView);
//        mStillImageButton=(ImageButton)findViewById(R.id.cameraImageButton);
//        mStillImageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                lockFocus();
//            }
//        });
//    }

//    @Override //metodo onresume
//    protected void onResume(){
//        super.onResume();
//
//        startBackgroundThread();
//        if(mTextureView.isAvailable()){
//            setupCamera(mTextureView.getWidth(), mTextureView.getHeight());
//            connectCamera();
//        }
//        else{
//            mTextureView.setSurfaceTextureListener(mSurfaceTextureListener);
//        }
//    }

//    @Override //vediamo se abbiamo i permessi
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
//        if(requestCode==REQUEST_CAMERA_PERMISSION_RESULT){
//            if (grantResults[0] != PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(getApplicationContext(),
//                        "L'app non verrà avviata senza il permesso di avviare la fotocamera", Toast.LENGTH_SHORT).show();
//            }
//        }
//        if (requestCode==REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT){
//            if (grantResults[0]==PackageManager.PERMISSION_GRANTED){
//                Toast.makeText(this, "Permesso di scrittura garantito!",
//                        Toast.LENGTH_SHORT).show();
//            }
//            else{
//                Toast.makeText(this, "L'applicazione non ha i permessi di scrittura necessari",
//                        Toast.LENGTH_SHORT).show();
//            }
//        }
//    }
//
//        @Override //metodo onpause
//        protected void onPause(){
//            closeCamera();
//            startBackgroundThread();
//            super.onPause();
//        }

//        @Override //con questo metodo setto l'app in fullscreen mode
//        public void onWindowFocusChanged(boolean hasFocas){
//            super.onWindowFocusChanged(hasFocas);
//            View decorView=getWindow().getDecorView();
//            if(hasFocas){
//                decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                        | View.SYSTEM_UI_FLAG_FULLSCREEN
//                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
//
//            }
//        }

    private android.util.Size mImageSize;
    private ImageReader mImageReader;

    private void setupCamera(int width, int height){
        CameraManager mCameraManager=(CameraManager) getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            //scorro l'array delle cameraID
            for (String cameraId : mCameraManager.getCameraIdList()){

                CameraCharacteristics cameraCharacteristics=mCameraManager.getCameraCharacteristics(cameraId);
                if (cameraCharacteristics.get(CameraCharacteristics.LENS_FACING)==CameraCharacteristics.LENS_FACING_FRONT){
                    continue;
                }

                int deviceOrientation=((Activity)getContext()).getWindowManager().getDefaultDisplay().getRotation();
                mTotalRotation=sensorDeviceRotation(cameraCharacteristics, deviceOrientation);
                boolean swapRotation=mTotalRotation==90 || mTotalRotation==270; //display in orizzontale o verticale?
                int rotatedWidth=width;
                int rotatedHeight=height;
                if (swapRotation){ //cambia orientamento
                    rotatedHeight=width;
                    rotatedWidth=height;
                }
                StreamConfigurationMap map=cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                mPreviewSize2=chooseOptimalSize(map.getOutputSizes(SurfaceTexture.class), rotatedWidth, rotatedHeight);
                mImageSize=chooseOptimalSize(map.getOutputSizes(ImageFormat.JPEG), rotatedWidth, rotatedHeight);
                mImageReader=ImageReader.newInstance(mImageSize.getWidth(), mImageSize.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(mOnImageAvailableListener, mBackgroundHandler);

                mCameraId2=cameraId;
                return;
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //metodo per fare una connessione con la camera
    private void connectCamera(){
        CameraManager cameraManager=(CameraManager)getContext().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
                if (ContextCompat.checkSelfPermission(getContext(), android.Manifest.permission.CAMERA)==
                        PackageManager.PERMISSION_GRANTED){
                    cameraManager.openCamera(mCameraId2, mCameraDeviceStateCallback, mBackgroundHandler);
                }
//                else{
//                    if (shouldShowRequestPermissionRationale(android.Manifest.permission.CAMERA)){
//                        Toast.makeText(this, "App required access to camera", Toast.LENGTH_SHORT).show();
//                    }
//                    requestPermissions(new String[] {android.Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_RESULT);
//                }
            }
            else{
                cameraManager.openCamera(mCameraId2, mCameraDeviceStateCallback, mBackgroundHandler);
            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void startPreview(){
        SurfaceTexture surfaceTexture = mTextureView.getSurfaceTexture();
        surfaceTexture.setDefaultBufferSize(mPreviewSize2.getWidth(), mPreviewSize2.getHeight());
        Surface previewSurface=new Surface(surfaceTexture);
        try {
            mCaptureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mCaptureRequestBuilder.addTarget(previewSurface);

            mCameraDevice.createCaptureSession(Arrays.asList(previewSurface, mImageReader.getSurface()), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    mPreviewCaptureSession=session;
                    try {
                        mPreviewCaptureSession.setRepeatingRequest(mCaptureRequestBuilder.build(),
                                null, mBackgroundHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(getContext(), "impossibile settare la camera preview",
                            Toast.LENGTH_SHORT).show();

                }
            }, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    //    questo metodo scatta la foto
    private void startStillCameraRequest(){
        try {
            mCaptureRequestBuilder=mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            mCaptureRequestBuilder.addTarget(mImageReader.getSurface());
            mCaptureRequestBuilder.set(CaptureRequest.JPEG_ORIENTATION,mTotalRotation);
            CameraCaptureSession.CaptureCallback stillCaptureCallback=new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureStarted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, long timestamp, long frameNumber) {
                    super.onCaptureStarted(session, request, timestamp, frameNumber);
                    try {
                        createImageFileName();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), stillCaptureCallback, null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void closeCamera(){
        if (mCameraDevice!=null){
            mCameraDevice.close();
            mCameraDevice=null;
        }
    }

    private void startBackgroundThread(){
        mBackgroundHandlerThread=new HandlerThread("Camera2VideoImage");
        mBackgroundHandlerThread.start();
        mBackgroundHandler=new Handler(mBackgroundHandlerThread.getLooper());

    }

    private void stopBackgroundThread(){
        mBackgroundHandlerThread.quitSafely();
        try {
            mBackgroundHandlerThread.join();
            mBackgroundHandlerThread=null;
            mBackgroundHandler=null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private static int sensorDeviceRotation(CameraCharacteristics cameraCharacteristics, int deviceOrientation){
        int sensorOrientation =cameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
        deviceOrientation=ORIENTATION.get(deviceOrientation);
        return (sensorOrientation + deviceOrientation + 360)%360;
    }

    private static android.util.Size chooseOptimalSize(android.util.Size[] choises, int width, int height){
        List<android.util.Size> bigEnough=new ArrayList<Size>();
        for (android.util.Size option: choises){
            if (option.getHeight()==option.getWidth()*option.getHeight()/width &&
                    option.getWidth()>=width && option.getHeight()>=height){
                bigEnough.add(option);
            }
        }
        if (bigEnough.size()>0){
            return Collections.min(bigEnough, new CompareSizeByArea());
        }
        else return choises[0];
    }

    private void createImageFolder(){
        File imageFile= Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        mImageFolder= new File(imageFile, "Camera2VideoImage" );

        if (!mImageFolder.exists()){
            mImageFolder.mkdirs();
        }
    }

    //ritorna un file.jpg
    private File createImageFileName() throws IOException{
        String timestamp= new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String prepend="IMAGE_"+ timestamp + "_";
        File imageFile = File.createTempFile(prepend, ".jpg", mImageFolder);
        mImageFileName=imageFile.getAbsolutePath();
        return imageFile;
    }

//    private static final int REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT=1;
//    //metodo controllato
//    private void checkWriteStoragePermission(){
//        if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
//            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
//                    == PackageManager.PERMISSION_GRANTED){
//            } else{
//                if (shouldShowRequestPermissionRationale(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)){
//                    Toast.makeText(getApplicationContext(),
//                            "L'app ha bisogno dei permessi per salvare video.", Toast.LENGTH_SHORT).show();
//                    requestPermissions(new String[] {android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
//                            REQUEST_WRITE_EXTERNAL_STORAGE_PERMISSION_RESULT);
//                }
//            }
//        }
//    }
    public void lockFocus(){
        mCaptureState=STATE_WAIT_LOCK;
        mCaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CaptureRequest.CONTROL_AF_TRIGGER_START);
        try {
            mPreviewCaptureSession.capture(mCaptureRequestBuilder.build(), mPreviewCaptureCallback, mBackgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

}