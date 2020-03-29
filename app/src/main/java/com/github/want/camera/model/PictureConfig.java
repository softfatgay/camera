package com.github.want.camera.model;

import java.util.ArrayList;
import java.util.List;


public class PictureConfig {

    private List<PictureModel> photolist;
    private int cameraType;//1后置摄像头，2前置，3，后置+前置
    private boolean openLight;//是否显示闪光灯按钮
    private boolean enableCrop;//是否剪裁，默认不剪裁
    private int takePhotoModel;//0，单独拍照，1多张连拍，默认单独拍照
    private boolean enableAbum;//是否显示相册按钮，默认不显示


    public void reset() {

        photolist = new ArrayList<>();
        cameraType = 1;
        openLight = false;
        enableCrop = false;
        takePhotoModel = 0;
        enableAbum = false;
    }

    private volatile static PictureConfig instance = null;

    public static PictureConfig getInstance() {
        if (instance == null) {
//            synchronized (PictureConfig.class) {
//                if (instance == null) {
//                    instance = new PictureConfig();
//                }
//            }
            instance = new PictureConfig();
        }
        return instance;
    }

    private PictureConfig() {

    }

    public List<PictureModel> getPhotolist() {
        return photolist;
    }

    public PictureConfig setPhotolist(List<PictureModel> photolist) {
        this.photolist = photolist;
        return this;
    }

    public int getCameraType() {
        return cameraType;
    }

    public PictureConfig setCameraType(int cameraType) {
        this.cameraType = cameraType;
        return this;
    }

    public boolean isOpenLight() {
        return openLight;
    }

    public PictureConfig setOpenLight(boolean openLight) {
        this.openLight = openLight;
        return this;
    }

    public boolean isEnableCrop() {
        return enableCrop;
    }

    public PictureConfig setEnableCrop(boolean enableCrop) {
        this.enableCrop = enableCrop;
        return this;
    }

    public int getTakePhotoModel() {
        return takePhotoModel;
    }

    public PictureConfig setTakePhotoModel(int takePhotoModel) {
        this.takePhotoModel = takePhotoModel;
        return this;
    }

    public boolean isEnableAbum() {
        return enableAbum;
    }

    public PictureConfig setEnableAbum(boolean enableAbum) {
        this.enableAbum = enableAbum;
        return this;
    }
}
