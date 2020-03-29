package com.github.want.camera

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.github.want.camera.utils.CameraUtil
import com.github.want.camera.utils.Constans
import com.yalantis.ucrop.UCrop
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream

open class BasePictureActivity : AppCompatActivity() {
    fun setCrop(uri: Uri?, context: Activity?) {
        val options = UCrop.Options()
        val toolbarColor = resources.getColor(R.color.black)
        val statusColor = resources.getColor(R.color.black)
        val titleColor = resources.getColor(R.color.white)
        options.setToolbarColor(toolbarColor)
        options.setStatusBarColor(statusColor)
        options.setToolbarWidgetColor(titleColor)
        options.setCircleDimmedLayer(false)
        options.setShowCropFrame(true)
        options.setShowCropGrid(false)
        options.setDragFrameEnabled(true)
        options.setScaleEnabled(true)
        options.setRotateEnabled(true)
        options.setCompressionQuality(90)
        options.setHideBottomControls(true)
        options.setFreeStyleCropEnabled(true)
        val realFilePath = CameraUtil.getRealFilePath(this, uri)
        val imgType = CameraUtil.getLastImgType(realFilePath)
        UCrop.of(uri!!, Uri.fromFile(File(CameraUtil.getDiskCacheDir(context), System.currentTimeMillis().toString() + imgType)))
                .withAspectRatio(0f, 0f)
                .withMaxResultSize(100, 100)
                .withOptions(options)
                .start(context!!)
    }

    /**19之后的系统相册的图片都存在于MediaStore数据库中；19之前的系统相册中可能包含不存在与数据库中的图片，所以如果是19之上的系统
     * 跳转到19之前的系统相册选择了一张不存在与数据库中的图片，解析uri时就可能会出现null */
    val galleryIntent: Unit
        get() {
            var intent = Intent()
            /**19之后的系统相册的图片都存在于MediaStore数据库中；19之前的系统相册中可能包含不存在与数据库中的图片，所以如果是19之上的系统
             * 跳转到19之前的系统相册选择了一张不存在与数据库中的图片，解析uri时就可能会出现null */
            if (Build.VERSION.SDK_INT < 19) {
                intent.action = Intent.ACTION_GET_CONTENT
                intent.type = "image/*"
            } else {
                intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                intent.setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            }
            startActivityForResult(intent, Constans.START_ALBUM_REQUEST_CODE)
        }

    fun getGalleryPhoto(uri: Uri?): String? {
        var imaPath: String? = null
        if (Build.VERSION.SDK_INT > 19) {
            imaPath = CameraUtil.getRealFilePath(this, uri)
        } else {
            val cursor = contentResolver.query(uri, null, null, null, null)
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    imaPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                }
                cursor.close()
            }
        }
        return imaPath
    }

    //将bitmap保存在本地，然后通知图库更新
    fun saveImageToGallery(context: Context, bmp: Bitmap): Uri {
        // 首先保存图片
        val appDir = File(Environment.getExternalStorageDirectory(), "tdh")
        if (!appDir.exists()) {
            appDir.mkdir()
        }
        val fileName = System.currentTimeMillis().toString() + ".jpg"
        val file = File(appDir, fileName)
        try {
            val fos = FileOutputStream(file)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            fos.flush()
            fos.close()
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show()
        }

        // 其次把文件插入到系统图库
        try {
            MediaStore.Images.Media.insertImage(context.contentResolver, file.absolutePath, fileName, null)
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
            Toast.makeText(context, "保存图片失败", Toast.LENGTH_SHORT).show()
        }
        // 最后通知图库更新
        val uri = Uri.parse("file://$file")
        context.sendBroadcast(Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri))
        return uri
    }
}