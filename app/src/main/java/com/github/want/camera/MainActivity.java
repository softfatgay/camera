package com.github.want.camera;

import android.Manifest;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.github.want.camera.utils.Constans;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.yalantis.ucrop.UCrop;

import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

public class MainActivity extends BasePictureActivity {
    RxPermissions rxPermissions;
    private RecyclerView recyclerView;
    private ImageView image;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        recyclerView = findViewById(R.id.recyclerView);

        rxPermissions = RxPermissions.getInstance(this);

        final List<PictureModel> photolist = new ArrayList<>();
        Button cameraBtn = findViewById(R.id.cameraBtn);
        Button abulyBtn = findViewById(R.id.abulyBtn);
        image = findViewById(R.id.image);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rxPermissions.request(Manifest.permission.CAMERA,
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        .subscribe(new Action1<Boolean>() {
                            @Override
                            public void call(Boolean aBoolean) {
                                if (aBoolean) {
                                    setPhotoList(photolist);
                                    PictureSelector.build(MainActivity.this)
                                            .cameraType(1)
                                            .openLight(true)
                                            .enableCrop(false)
                                            .takePhotoModel(1)
                                            .photolist(photolist)
                                            .enableAbum(true).builder(Constans.START_PHOTO_REQUEST_CODE);

                                }
                            }
                        });
            }
        });


        abulyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getGalleryIntent();
            }
        });
    }

    private void setPhotoList(List<PictureModel> photolist) {
        photolist.clear();

        for (int i = 0; i < 10; i++) {
            PictureModel pictureModel = new PictureModel();
            pictureModel.setName("第"+i+"张");
            pictureModel.setPath(null);
            photolist.add(pictureModel);

        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constans.START_PHOTO_REQUEST_CODE && resultCode == RESULT_OK) {
            List<PictureModel> photolist = (List<PictureModel>) data.getSerializableExtra(Constans.CALL_BACK_PHOTO_LIST);
            setView(photolist);
        }if (requestCode == Constans.START_ALBUM_REQUEST_CODE && resultCode == RESULT_OK) {
            Uri uri = data.getData();
            setCrop(uri,this);
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == RESULT_OK) {
            Uri uri = UCrop.getOutput(data);
            List<PictureModel> photolist = new ArrayList<>();
            PictureModel model = new PictureModel();
            String imgPath = getGalleryPhoto(uri);
            model.setPath(imgPath);
            photolist.add(model);
            setView(photolist);
        }
    }

    private void setView(List<PictureModel> photolist) {
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerView.setLayoutManager(linearLayoutManager);

        ImageAdapter imageAdapter = new ImageAdapter(this);
        imageAdapter.setDataList(photolist);
        recyclerView.setAdapter(imageAdapter);

        Glide.with(this).load(photolist.get(0).getPath()).into(image);
    }

}
