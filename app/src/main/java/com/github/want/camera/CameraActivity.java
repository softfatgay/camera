package com.github.want.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.cjt2325.cameralibrary.util.ScreenUtils;
import com.github.want.camera.utils.CameraParamUtil;
import com.github.want.camera.utils.CameraUtil;
import com.github.want.camera.utils.Constans;
import com.github.want.camera.utils.OnClickListener;
import com.github.want.camera.utils.StatusBarUtil;
import com.yalantis.ucrop.UCrop;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class CameraActivity extends BasePictureActivity implements SurfaceHolder.Callback, View.OnClickListener {
    private Camera mCamera;
    private int cameraPosition = 0;//0代表前置摄像头,1代表后置摄像头,默认打开前置摄像头
    private SurfaceHolder holder;

    private SurfaceView mSurfaceView;
    private ImageButton openLight;
    private View bootomRly;
    private RecyclerView recyclerView;
    private TextView title_tv;

    //放大缩小
    private Camera.Parameters parameters;
    private Handler handler = new Handler();
    boolean safeToTakePicture = true;
    private double screenProp = 0f;

    /* 图像数据处理完成后的回调函数 */
    private Camera.PictureCallback mJpeg = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(final byte[] data, Camera camera) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        //将照片改为竖直方向
                        Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                        Matrix matrix = new Matrix();
                        switch (cameraPosition) {
                            case 0://前
                                matrix.preRotate(90);
                                break;
                            case 1:
                                matrix.preRotate(270);
                                break;
                        }
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, false);
                        saveImage(CameraActivity.this, bitmap);
                        mCamera.stopPreview();
                        mCamera.startPreview();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    safeToTakePicture = true;
                }
            }).start();
        }
    };


    //是否开启闪关灯按钮
    private boolean isOpenLight;
    //完成按钮
    private ImageView lookPictureIv;
    private ImageView back;
    private Button takePhoto;
    private ImageButton cameraSwitch;
    private ImageView album;
    private int cameraType;
    private PictureConfig pictureSelector;

    @SuppressLint("ResourceAsColor")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        initView();
        initStatuBar();
        initData();

        initRecyclerView();
        initListenner();
    }

    private void initListenner() {
        album.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGalleryIntent();
            }
        });
    }

    private void initStatuBar() {
        StatusBarUtil.transparencyBar(this);
    }

    CopyOnWriteArrayList<PictureModel> photolist = new CopyOnWriteArrayList<>();
    ImageAdapter imageAdapter;

    private void initRecyclerView() {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);
        imageAdapter = new ImageAdapter(this);
        imageAdapter.setDataList(photolist);
        recyclerView.setAdapter(imageAdapter);


        imageAdapter.setDeleteClickListenner(new OnClickListener() {
            @Override
            public void onClick(int position) {
                PictureModel pictureModel = photolist.get(position);
                pictureModel.setPath("");
                photolist.set(position, pictureModel);
                imageAdapter.setDataList(photolist);
                lookPictureIv.setVisibility(View.GONE);

                for (int i = 0; i < photolist.size(); i++) {
                    if (TextUtils.isEmpty(photolist.get(i).getPath())) {
                        title_tv.setText(photolist.get(i).getName());
                        imageAdapter.setPhotoSelect(i);
                        break;
                    }
                }
            }
        });
    }

    private void initView() {

        mSurfaceView = findViewById(R.id.my_surfaceView);
        openLight = findViewById(R.id.openLight);
        bootomRly = findViewById(R.id.bootomRly);
        recyclerView = findViewById(R.id.recyclerView);
        title_tv = findViewById(R.id.title_tv);

        back = findViewById(R.id.back);
        lookPictureIv = findViewById(R.id.lookPictureIv);
        takePhoto = findViewById(R.id.takePhoto);
        cameraSwitch = findViewById(R.id.cameraSwitch);
        album = findViewById(R.id.album);

        back.setOnClickListener(this);
        lookPictureIv.setOnClickListener(this);
        takePhoto.setOnClickListener(this);
        openLight.setOnClickListener(this);
        cameraSwitch.setOnClickListener(this);
        mSurfaceView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    //TODO 兼容不支持自动聚焦功能，点击聚焦，可以删除
                    autoFocus();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });

        if (screenProp == 0) {
            int screenHeight = ScreenUtils.getScreenHeight(this);
            int screenWidth = ScreenUtils.getScreenWidth(this);
            Log.e(">>>>>>>>>>>>>>>", String.valueOf(screenHeight));
            Log.e(">>>>>>>>>>>>>>>", String.valueOf(screenWidth));
            double screenM = screenHeight /screenWidth;
            screenProp =  screenM;
            Log.e(">>>>>>>>>>>>>>>", String.valueOf(this.screenProp));
        }
    }

    @SuppressWarnings("deprecation")
    protected void initData() {
        pictureSelector = PictureConfig.getInstance();
        isOpenLight = pictureSelector.isOpenLight();
        cameraType = pictureSelector.getCameraType();

        List<PictureModel> list = pictureSelector.getPhotolist();
        photolist.addAll(list);
        if (photolist.size() > 0) {
            title_tv.setText(list.get(0).getName());
        }
        if (isOpenLight) {
            openLight.setVisibility(View.VISIBLE);
        } else {
            openLight.setVisibility(View.GONE);
        }

        if (pictureSelector.getTakePhotoModel() == 1) {

        } else {
            recyclerView.setVisibility(View.GONE);
            if (pictureSelector.isEnableAbum()) {
                album.setVisibility(View.VISIBLE);
            }
//            lookPictureIv.setVisibility(View.VISIBLE);
        }

        holder = mSurfaceView.getHolder();
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holder.addCallback(this); // 回调接口

        bootomRly.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            }
        });
    }

    //自动对焦
    private void autoFocus() {
        if (mCamera == null) {
            return;
        }
        parameters = mCamera.getParameters();
        parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);//连续对焦
        mCamera.setParameters(parameters);
        mCamera.startPreview();
        mCamera.cancelAutoFocus();
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();
        startPreview(mCamera, holder);
        autoFocus();
        mCamera.cancelAutoFocus();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startPreview(mCamera, holder);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        releaseCamera();
    }

    public void onResume() {
        super.onResume();
        if (mCamera == null) {
            mCamera = getCamera(cameraPosition);
            if (holder != null) {
                startPreview(mCamera, holder);
            }
            if (cameraType == 2) {
                cameraSwitch.performClick();
            }
        }

    }

    @Override
    public void onPause() {
        super.onPause();
        releaseCamera();
    }

    /**
     * 闪光灯
     *
     * @param mCamera
     */
    private void turnLight(Camera mCamera) {
        if (mCamera == null || mCamera.getParameters() == null
                || mCamera.getParameters().getSupportedFlashModes() == null) {
            return;
        }
        Camera.Parameters parameters = mCamera.getParameters();
        String flashMode = mCamera.getParameters().getFlashMode();
        List<String> supportedModes = mCamera.getParameters().getSupportedFlashModes();
        if (Camera.Parameters.FLASH_MODE_OFF.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_ON)) {//关闭状态
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON);
            mCamera.setParameters(parameters);
            openLight.setImageResource(R.drawable.camera_flash_on);
        } else if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {//开启状态
            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
                openLight.setImageResource(R.drawable.camera_flash_auto);
                mCamera.setParameters(parameters);
            } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                openLight.setImageResource(R.drawable.camera_flash_off);
                mCamera.setParameters(parameters);
            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO.equals(flashMode)
                && supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            mCamera.setParameters(parameters);
            openLight.setImageResource(R.drawable.camera_flash_off);
        }
    }

    private Camera getCamera(int id) {
        Camera camera = null;
        try {
            camera = Camera.open(id);
        } catch (Exception e) {

        }
        return camera;
    }

    /**
     * 预览
     */
    private void startPreview(Camera camera, SurfaceHolder holder) {
        try {
            setupCamera(camera, holder);
            camera.setPreviewDisplay(holder);
            //亲测的一个方法 基本覆盖所有手机 将预览矫正
            CameraUtil.getInstance().setCameraDisplayOrientation(this, cameraPosition, camera);
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 释放相机资源
     */
    private void releaseCamera() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        releaseCamera();
        pictureSelector.reset();
        pictureSelector = null;
        super.onDestroy();
    }


    private void setupCamera(Camera camera, SurfaceHolder holder) {
        setStartPreview(camera, holder);
    }

    private int width;
    private int height;

    //启动相机浏览
    private void setStartPreview(Camera camera, SurfaceHolder holder) {
        try {
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPictureFormat(ImageFormat.JPEG);
            List<Camera.Size> sizeList = parameters.getSupportedPreviewSizes();//获取所有支持的camera尺寸
            Iterator<Camera.Size> itor = sizeList.iterator();
            while (itor.hasNext()) {
                Log.e("//////////////////", "////////////////");
                Camera.Size cur = itor.next();
                Log.i("CJT", "所有的  width = " + cur.width + " height = " + cur.height);
                if (cur.width >= width) {
                    Log.e("=================", "=================");
                    Log.i("=================", "width = " + cur.width + " height = " + cur.height);
                    width = cur.width;
                    height = (int) (width * screenProp);
                    height = cur.height;
                }
            }


            parameters.setPreviewSize(width, height);//把camera.size赋值到parameters
            parameters.setPictureSize(width, height);
            if (cameraPosition == 0) {
                parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
            }

            camera.setParameters(parameters);

            camera.setPreviewDisplay(holder);
            camera.setDisplayOrientation(90);
            camera.startPreview();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private int cameraAngle = 90;//摄像头角度   默认为90度


    private void setParameter() {
        Camera.Parameters parameters = mCamera.getParameters(); // 获取各项参数
        parameters.setPictureFormat(PixelFormat.JPEG); // 设置图片格式
        parameters.setJpegQuality(70); // 设置照片质量
        //获得相机支持的照片尺寸,选择合适的尺寸
        List<Camera.Size> sizes = parameters.getSupportedPictureSizes();
        int maxSize = Math.max(CameraUtil.screenWidth, CameraUtil.screenHeight);
        int length = sizes.size();
//        if (maxSize > 0) {
//            for (int i = 0; i < length; i++) {
//                if (maxSize <= Math.max(sizes.get(i).width, sizes.get(i).height)) {
//                    parameters.setPictureSize(sizes.get(i).width, sizes.get(i).height);
//                    break;
//                }
//            }
//        }
        List<Camera.Size> ShowSizes = parameters.getSupportedPreviewSizes();
        int showLength = ShowSizes.size();
        if (maxSize > 0) {
            for (int i = 0; i < showLength; i++) {
                if (maxSize <= Math.max(ShowSizes.get(i).width, ShowSizes.get(i).height)) {
                    parameters.setPictureSize(ShowSizes.get(i).width, ShowSizes.get(i).height);
                    parameters.setPreviewSize(ShowSizes.get(i).width, ShowSizes.get(i).height);
                    break;
                }
            }
        }
        mCamera.setParameters(parameters);
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(CameraUtil.screenWidth, CameraUtil.screenWidth*4/3);
//        mSurfaceView.setLayoutParams(params);
    }

    //将bitmap保存，然后通知图库更新
    public void saveImage(Context context, Bitmap bmp) {
        final Uri uri = saveImageToGallery(context, bmp);
        final String realFilePath = CameraUtil.getRealFilePath(CameraActivity.this, uri);
        isNullPath = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pictureSelector.getTakePhotoModel() == 1) {
                    if (photolist.size() > 0) {
                        for (int i = 0; i < photolist.size(); i++) {
                            if (TextUtils.isEmpty(photolist.get(i).getPath())) {
                                PictureModel pictureModel = photolist.get(i);
                                pictureModel.setPath(realFilePath);
                                photolist.set(i, pictureModel);
                                isNullPath = true;
                                imageAdapter.setDataList(photolist);
                                break;
                            }
                        }

                        for (int i = 0; i < photolist.size(); i++) {
                            if (TextUtils.isEmpty(photolist.get(i).getPath())) {
                                imageAdapter.setPhotoSelect(i);
                                lookPictureIv.setVisibility(View.GONE);
                                break;
                            }
                            lookPictureIv.setVisibility(View.VISIBLE);
                        }

                    }

                    for (PictureModel pictureModel : photolist) {
                        if (TextUtils.isEmpty(pictureModel.getPath())) {
                            title_tv.setText(pictureModel.getName());
                            break;
                        }
                    }
                } else {
                    if (pictureSelector.isEnableCrop()) {
                        setCrop(uri, CameraActivity.this);
                    } else {
                        photolist.clear();
                        PictureModel model = new PictureModel();
                        model.setPath(realFilePath);
                        photolist.add(model);
                        setResult(RESULT_OK, new Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist));
                        finish();
                    }

                }
            }
        });
    }

    boolean isNullPath = false;

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.back:
                finish();
                break;
            case R.id.lookPictureIv:
                //指定照片

                for (PictureModel pictureModel : photolist) {
                    if (TextUtils.isEmpty(pictureModel.getPath())) {
                        Toast.makeText(this, "请完成拍照", Toast.LENGTH_SHORT).show();
                        return;
                    }
                }
                setResult(RESULT_OK, new Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist));
                finish();
                break;

            case R.id.takePhoto:
                if (safeToTakePicture) {
                    safeToTakePicture = false;
                    mCamera.takePicture(null, null, mJpeg);
                }
                break;
            case R.id.openLight:
                turnLight(mCamera);
                break;
            case R.id.cameraSwitch:
                releaseCamera();
                cameraPosition = (cameraPosition + 1) % mCamera.getNumberOfCameras();
                mCamera = getCamera(cameraPosition);
                if (holder != null) {
                    startPreview(mCamera, holder);
                }
                break;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case UCrop.REQUEST_CROP:
                    singleCropHandleResult(data);
                    break;
                case Constans.START_ALBUM_REQUEST_CODE:
                    if (pictureSelector.isEnableCrop()) {
                        Uri uri = data.getData();
                        setCrop(uri, this);
                    } else {
                        setResult(data);
                    }
                    break;
            }
        } else if (resultCode == RESULT_CANCELED) {
            finish();
        } else if (resultCode == UCrop.RESULT_ERROR) {
            finish();
        }
    }

    /**
     * 相册不剪裁直接返回
     *
     * @param data
     */
    private void setResult(Intent data) {
        photolist.clear();
        PictureModel model = new PictureModel();
        String imgPath = getGalleryPhoto(data.getData());
        model.setPath(imgPath);
        photolist.add(model);
        setResult(RESULT_OK, new Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist));
        finish();
    }

    /**
     * 单张图片裁剪
     *
     * @param data
     */
    private void singleCropHandleResult(Intent data) {
        Uri resultUri = UCrop.getOutput(data);
        String cutPath = resultUri.getPath();
        photolist.clear();
        PictureModel model = new PictureModel();
        model.setPath(cutPath);
        photolist.add(model);
        setResult(RESULT_OK, new Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist));
        finish();
    }

}
