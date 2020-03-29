package com.github.want.camera.model;

import android.app.Activity;
import android.content.Intent;

import com.github.want.camera.CameraActivity;
import com.github.want.camera.utils.DoubleUtils;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.List;

public class PictureSelector implements Serializable {

    private WeakReference<Activity> mActivity;

    private PictureSelector(Activity activity) {
        mActivity = new WeakReference<>(activity);
    }

    public static PictureSelector build(Activity activity) {
        PictureConfig.getInstance().reset();
        return new PictureSelector(activity);
    }

    public PictureSelector takePhotoModel(int takePhotoModel) {
        PictureConfig.getInstance().setTakePhotoModel(takePhotoModel);
        return this;
    }

    public PictureSelector enableCrop(boolean enableCrop) {
        PictureConfig.getInstance().setEnableCrop(enableCrop);
        return this;
    }

    public PictureSelector openLight(boolean openLight) {
        PictureConfig.getInstance().setOpenLight(openLight);
        return this;
    }

    public PictureSelector cameraType(int cameraType) {
        PictureConfig.getInstance().setCameraType(cameraType);
        return this;
    }

    //多张连拍
    public PictureSelector photolist(List<PictureModel> photolist) {
        PictureConfig.getInstance().setPhotolist(photolist);
        return this;
    }

    public PictureSelector enableAbum(boolean enableAbum) {
        PictureConfig.getInstance().setEnableAbum(enableAbum);
        return this;
    }

    public void builder(int requestCode) {
        if (!DoubleUtils.isFastDoubleClick()) {
            if (mActivity.get() == null) {
                return;
            }
            Intent intent = new Intent(mActivity.get(), CameraActivity.class);
            if (mActivity.get() != null) {
                mActivity.get().startActivityForResult(intent, requestCode);
            }
        }
    }


}
