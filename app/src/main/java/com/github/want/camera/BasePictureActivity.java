package com.github.want.camera;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.github.want.camera.utils.CameraUtil;
import com.github.want.camera.utils.Constans;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

public class BasePictureActivity extends AppCompatActivity {

    public void setCrop(Uri uri, Activity context) {
        UCrop.Options options = new UCrop.Options();
        int toolbarColor = getResources().getColor(R.color.black);
        int statusColor = getResources().getColor(R.color.black);
        int titleColor = getResources().getColor(R.color.white);
        options.setToolbarColor(toolbarColor);
        options.setStatusBarColor(statusColor);
        options.setToolbarWidgetColor(titleColor);
        options.setCircleDimmedLayer(false);
        options.setShowCropFrame(true);
        options.setShowCropGrid(false);
        options.setDragFrameEnabled(true);
        options.setScaleEnabled(true);
        options.setRotateEnabled(true);
        options.setCompressionQuality(90);
        options.setHideBottomControls(true);
        options.setFreeStyleCropEnabled(true);


        String realFilePath = CameraUtil.getRealFilePath(this, uri);
        String imgType = CameraUtil.getLastImgType(realFilePath);

        UCrop.of(uri, Uri.fromFile(new File(CameraUtil.getDiskCacheDir(context),
                System.currentTimeMillis() + imgType)))
                .withAspectRatio(0, 0)
                .withMaxResultSize(100, 100)
                .withOptions(options)
                .start(context);
    }


    public void getGalleryIntent() {

        Intent intent = new Intent();
        /**19之后的系统相册的图片都存在于MediaStore数据库中；19之前的系统相册中可能包含不存在与数据库中的图片，所以如果是19之上的系统
         * 跳转到19之前的系统相册选择了一张不存在与数据库中的图片，解析uri时就可能会出现null*/
        if (Build.VERSION.SDK_INT < 19) {
            intent.setAction(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*");
        }

        startActivityForResult(intent, Constans.START_ALBUM_REQUEST_CODE);
    }

    public String getGalleryPhoto(Uri uri) {
        String imaPath = null;
        if (Build.VERSION.SDK_INT > 19) {
            imaPath = CameraUtil.getRealFilePath(this, uri);
        } else {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    imaPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
                }
                cursor.close();
            }
        }

        return imaPath;
    }


    //将bitmap保存在本地，然后通知图库更新
    public Uri saveImageToGallery(Context context, Bitmap bmp) {
        // 首先保存图片
        File appDir = new File(Environment.getExternalStorageDirectory(), "tdh");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        String fileName = System.currentTimeMillis() + ".jpg";
        File file = new File(appDir, fileName);
        try {
            FileOutputStream fos = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show();
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.getContentResolver(), file.getAbsolutePath(), fileName, null);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show();
        }
        // 最后通知图库更新
        final Uri uri = Uri.parse("file://" + file);
        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));

        return uri;
    }

}
