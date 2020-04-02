package com.github.want.camera

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.hardware.Camera.PictureCallback
import android.os.Bundle
import android.os.Handler
import android.support.v7.widget.LinearLayoutManager
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import com.github.want.camera.model.PictureConfig
import com.github.want.camera.model.PictureModel
import com.github.want.camera.utils.CameraUtil
import com.github.want.camera.utils.Constans
import com.github.want.camera.utils.OnClickListener
import com.github.want.camera.utils.StatusBarUtil
import com.luck.picture.lib.compress.Luban
import com.luck.picture.lib.config.PictureMimeType
import com.luck.picture.lib.entity.LocalMedia
import com.yalantis.ucrop.UCrop
import io.reactivex.Flowable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_camera.*
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

@Suppress("DEPRECATION")
class CameraActivity : BasePictureActivity(), SurfaceHolder.Callback, View.OnClickListener {
    private var mCamera: Camera? = null
    private var cameraPosition = 0 //0代表前置摄像头,1代表后置摄像头,默认打开前置摄像头
    private lateinit var holder: SurfaceHolder

    //放大缩小
    private lateinit var parameters: Camera.Parameters
    private val handler = Handler()
    var safeToTakePicture = true

    /* 图像数据处理完成后的回调函数 */
    private val mJpeg = PictureCallback { data, camera ->
        Thread(Runnable {
            try {
                //将照片改为竖直方向
                var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                val matrix = Matrix()
                when (cameraPosition) {
                    0 -> matrix.preRotate(90f)
                    1 -> matrix.preRotate(270f)
                }
                bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                saveImage(baseContext, bitmap)
                mCamera?.run {
                    stopPreview()
                    startPreview()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
            safeToTakePicture = true
        }).start()
    }

    //是否开启闪关灯按钮
    private var isOpenLight = false

    private var cameraType = 0
    private lateinit var pictureSelector: PictureConfig

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)
        CameraUtil.init(this)
        initView()
        initStatuBar()
        initData()
        initRecyclerView()
        initListenner()
    }

    private fun initListenner() {
        album!!.setOnClickListener { galleryIntent }
    }

    private fun initStatuBar() {
        StatusBarUtil.transparencyBar(this)
    }

    var photolist = CopyOnWriteArrayList<PictureModel>()
    lateinit var imageAdapter: ImageAdapter
    private fun initRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView.layoutManager = linearLayoutManager
        imageAdapter = ImageAdapter(this)
        imageAdapter.setDataList(photolist)
        recyclerView.adapter = imageAdapter

        imageAdapter.setDeleteClickListenner(OnClickListener { position ->
            val pictureModel = photolist[position]
            pictureModel.path = ""
            photolist[position] = pictureModel
            imageAdapter.setDataList(photolist)
            lookPictureIv.visibility = View.GONE
            for (i in photolist.indices) {
                if (TextUtils.isEmpty(photolist[i].path)) {
                    title_tv.text = photolist[i].name
                    imageAdapter.setPhotoSelect(i)
                    break
                }
            }
        })
    }

    private fun initView() {
        back.setOnClickListener(this)
        lookPictureIv.setOnClickListener(this)
        takePhoto.setOnClickListener(this)
        openLight.setOnClickListener(this)
        cameraSwitch.setOnClickListener(this)
        my_surfaceView.setOnClickListener(View.OnClickListener {
            try {
                //TODO 兼容不支持自动聚焦功能，点击聚焦，可以删除
                autoFocus()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        })
    }

    protected fun initData() {
        pictureSelector = PictureConfig.getInstance()
        isOpenLight = pictureSelector.isOpenLight()
        cameraType = pictureSelector.getCameraType()
        val list = pictureSelector.getPhotolist()
        photolist.addAll(list)
        if (photolist.size > 0) {
            title_tv!!.text = list[0].name
        }
        if (isOpenLight) {
            openLight!!.visibility = View.VISIBLE
        } else {
            openLight!!.visibility = View.GONE
        }
        if (pictureSelector.getTakePhotoModel() == 1) {
        } else {
            recyclerView!!.visibility = View.GONE
            if (pictureSelector.isEnableAbum()) {
                album!!.visibility = View.VISIBLE
            }
            //            lookPictureIv.setVisibility(View.VISIBLE);
        }
        holder = my_surfaceView.holder
        holder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        holder.addCallback(this) // 回调接口
        bootomRly!!.setOnClickListener { }
    }

    //自动对焦
    private fun autoFocus() {
        mCamera?.run {
            this@CameraActivity.parameters = parameters
            this@CameraActivity.parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE) //连续对焦
            parameters = this@CameraActivity.parameters
            startPreview()
            cancelAutoFocus()
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        mCamera?.run {
            stopPreview()
            startPreview(this, holder)
            autoFocus()
            cancelAutoFocus()
        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        mCamera?.run {
            startPreview(this, holder)
        }

    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        releaseCamera()
    }

    public override fun onResume() {
        super.onResume()
        if (mCamera == null) {
            mCamera = getCamera(cameraPosition)
        }
        mCamera?.run {
            if (holder != null) {
                startPreview(this, holder)
            }
            if (cameraType == 2) {
                cameraSwitch!!.performClick()
            }
        }
    }

    public override fun onPause() {
        super.onPause()
        releaseCamera()
    }

    /**
     * 闪光灯
     *
     * @param mCamera
     */
    private fun turnLight(mCamera: Camera?) {
        if (mCamera == null || mCamera.parameters == null || mCamera.parameters.supportedFlashModes == null) {
            return
        }
        val parameters = mCamera.parameters
        val flashMode = mCamera.parameters.flashMode
        val supportedModes = mCamera.parameters.supportedFlashModes
        if (Camera.Parameters.FLASH_MODE_OFF == flashMode && supportedModes.contains(Camera.Parameters.FLASH_MODE_ON)) { //关闭状态
            parameters.flashMode = Camera.Parameters.FLASH_MODE_ON
            mCamera.parameters = parameters
            openLight!!.setImageResource(R.drawable.camera_flash_on)
        } else if (Camera.Parameters.FLASH_MODE_ON == flashMode) { //开启状态
            if (supportedModes.contains(Camera.Parameters.FLASH_MODE_AUTO)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_AUTO
                openLight!!.setImageResource(R.drawable.camera_flash_auto)
                mCamera.parameters = parameters
            } else if (supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
                openLight!!.setImageResource(R.drawable.camera_flash_off)
                mCamera.parameters = parameters
            }
        } else if (Camera.Parameters.FLASH_MODE_AUTO == flashMode && supportedModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
            parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
            mCamera.parameters = parameters
            openLight!!.setImageResource(R.drawable.camera_flash_off)
        }
    }

    private fun getCamera(id: Int): Camera? {
        var camera: Camera? = null
        try {
            camera = Camera.open(id)
        } catch (e: Exception) {
        }
        return camera!!
    }

    /**
     * 预览
     */
    private fun startPreview(camera: Camera?, holder: SurfaceHolder) {
        try {
            setupCamera(camera)
            camera!!.setPreviewDisplay(holder)
            //亲测的一个方法 基本覆盖所有手机 将预览矫正
            CameraUtil.getInstance().setCameraDisplayOrientation(this, cameraPosition, camera)
            camera.startPreview()
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * 释放相机资源
     */
    private fun releaseCamera() {
        mCamera?.run {
            setPreviewCallback(null)
            stopPreview()
//            release()
        }
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
//        releaseCamera()
        pictureSelector.reset()
        mCamera?.run {
            release()
        }
        super.onDestroy()
    }

    private fun setupCamera(camera: Camera?) {
        val parameters = camera!!.parameters
        val focusModes = parameters.supportedFocusModes
        if (focusModes.contains(Camera.Parameters.FOCUS_MODE_AUTO)) {
            parameters.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
        val previewSize = CameraUtil.findBestPreviewResolution(camera)
        parameters.setPreviewSize(previewSize.width, previewSize.height)
        val pictrueSize = CameraUtil.getInstance().getPropPictureSize(parameters.supportedPictureSizes, 1000)
        parameters.setPictureSize(pictrueSize.width, pictrueSize.height)
        camera.parameters = parameters
        //
//        int picHeight = CameraUtil.screenWidth * previewSize.width / previewSize.height;
//        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(CameraUtil.screenWidth, picHeight);
//        my_surfaceView.setLayoutParams(params);
    }

    //将bitmap保存，然后通知图库更新
    fun saveImage(context: Context?, bmp: Bitmap?) {
        val uri = saveImageToGallery(context!!, bmp!!)
        val realFilePath = CameraUtil.getRealFilePath(this@CameraActivity, uri)
        isNullPath = false
        runOnUiThread {
            if (pictureSelector!!.takePhotoModel == 1) {
                if (photolist.size > 0) {
                    for (i in photolist.indices) {
                        if (TextUtils.isEmpty(photolist[i].path)) {
                            val pictureModel = photolist[i]
                            pictureModel.path = realFilePath
                            photolist[i] = pictureModel
                            isNullPath = true
                            imageAdapter!!.setDataList(photolist)
                            break
                        }
                    }
                    for (i in photolist.indices) {
                        if (TextUtils.isEmpty(photolist[i].path)) {
                            imageAdapter!!.setPhotoSelect(i)
                            lookPictureIv!!.visibility = View.GONE
                            break
                        }
                        lookPictureIv!!.visibility = View.VISIBLE
                    }
                }
                for (pictureModel in photolist) {
                    if (TextUtils.isEmpty(pictureModel.path)) {
                        title_tv!!.text = pictureModel.name
                        break
                    }
                }
            } else {
                if (pictureSelector.isEnableCrop) {
                    setCrop(uri, this@CameraActivity)
                } else {
                    photolist.clear()
                    val model = PictureModel()
                    model.path = realFilePath
                    photolist.add(model)
                    setResult(Activity.RESULT_OK, Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist))
                    finish()
                }
            }
        }
    }

    var isNullPath = false
    override fun onClick(v: View) {
        when (v.id) {
            R.id.back -> finish()
            R.id.lookPictureIv -> {
                for (pictureModel in photolist) {
                    if (TextUtils.isEmpty(pictureModel.path)) {
                        return
                    }
                }
                setResult(Activity.RESULT_OK, Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist))
                finish()

            }
            R.id.takePhoto -> if (safeToTakePicture) {
                safeToTakePicture = false
                mCamera!!.takePicture(null, null, mJpeg)
            }
            R.id.openLight -> turnLight(mCamera)
            R.id.cameraSwitch -> {
                releaseCamera()
                cameraPosition = (cameraPosition + 1) % Camera.getNumberOfCameras()
                mCamera = getCamera(cameraPosition)!!
                if (holder != null) {
                    startPreview(mCamera, holder)
                }
            }
        }
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                UCrop.REQUEST_CROP -> singleCropHandleResult(data)
                Constans.START_ALBUM_REQUEST_CODE -> if (pictureSelector.isEnableCrop) {
                    val uri = data!!.data
                    setCrop(uri, this)
                } else {
                    setResult(data)
                }
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            finish()
        } else if (resultCode == UCrop.RESULT_ERROR) {
            finish()
        }
    }

    /**
     * 相册不剪裁直接返回
     *
     * @param data
     */
    private fun setResult(data: Intent?) {
        photolist.clear()
        val model = PictureModel()
        val imgPath = getGalleryPhoto(data!!.data)
        model.path = imgPath
        photolist.add(model)
        setResult(Activity.RESULT_OK, Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist))
        finish()
    }

    /**
     * 单张图片裁剪
     *
     * @param data
     */
    private fun singleCropHandleResult(data: Intent?) {
        val resultUri = UCrop.getOutput(data!!)
        val cutPath = resultUri!!.path
        photolist.clear()
        val model = PictureModel()
        model.path = cutPath
        photolist.add(model)
        setResult(Activity.RESULT_OK, Intent().putExtra(Constans.CALL_BACK_PHOTO_LIST, photolist))
        finish()
    }
}