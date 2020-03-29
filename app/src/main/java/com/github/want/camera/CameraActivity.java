package com.github.want.camera;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.github.want.camera.utils.CameraUtil;
import com.github.want.camera.utils.Constans;
import com.github.want.camera.utils.OnClickListener;
import com.github.want.camera.utils.StatusBarUtil;
import com.yalantis.ucrop.UCrop;

import java.io.IOException;
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
                        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                        saveImage(getBaseContext(), bitmap);
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
        CameraUtil.init(this);
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
            setupCamera(camera);
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


    private void setupCamera(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();

        List<String> focusModes = parameters.getSupportedFocusModes();
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
        }

        Camera.Size previewSize = CameraUtil.findBestPreviewResolution(camera);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        Camera.Size pictrueSize = CameraUtil.getInstance().getPropPictureSize(parameters.getSupportedPictureSizes(), 1000);
        parameters.setPictureSize(pictrueSize.width, pictrueSize.height);
        camera.setParameters(parameters);
//
//        int picHeight = CameraUtil.screenWidth * previewSize.width / previewSize.height;
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(CameraUtil.screenWidth, picHeight);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
