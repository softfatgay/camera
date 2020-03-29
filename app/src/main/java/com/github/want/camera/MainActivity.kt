package com.github.want.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import com.github.want.camera.model.PictureModel
import com.github.want.camera.model.PictureSelector
import com.github.want.camera.utils.Constans
import com.tbruyelle.rxpermissions.RxPermissions
import com.yalantis.ucrop.UCrop
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : BasePictureActivity() {
   lateinit var rxPermissions: RxPermissions
    private var recyclerView: RecyclerView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerView)
        rxPermissions = RxPermissions.getInstance(this)
        val photolist: MutableList<PictureModel> = ArrayList()
        cameraBtn.setOnClickListener {
            rxPermissions.request(Manifest.permission.CAMERA,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    .subscribe { aBoolean ->
                        if (aBoolean) {
                            setPhotoList(photolist)
                            PictureSelector.build(this@MainActivity)
                                    .cameraType(1)
                                    .openLight(true)
                                    .enableCrop(false)
                                    .takePhotoModel(1)
                                    .photolist(photolist)
                                    .enableAbum(true).builder(Constans.START_PHOTO_REQUEST_CODE)
                        }
                    }
        }
        abulyBtn.setOnClickListener { galleryIntent }
    }

    private fun setPhotoList(photolist: MutableList<PictureModel>) {
        photolist.clear()
        val pictureModel = PictureModel()
        pictureModel.name = "车头"
        pictureModel.path = null
        photolist.add(pictureModel)
        val pictureModel1 = PictureModel()
        pictureModel1.name = "车尾"
        pictureModel1.path = null
        photolist.add(pictureModel1)
        val pictureModel2 = PictureModel()
        pictureModel2.name = "车左侧"
        pictureModel2.path = null
        photolist.add(pictureModel2)
        val pictureModel3 = PictureModel()
        pictureModel3.name = "车右侧"
        pictureModel3.path = null
        photolist.add(pictureModel3)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == Constans.START_PHOTO_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photolist = data!!.getSerializableExtra(Constans.CALL_BACK_PHOTO_LIST) as List<PictureModel>
            setView(photolist)
        }
        if (requestCode == Constans.START_ALBUM_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val uri = data!!.data
            setCrop(uri, this)
        } else if (requestCode == UCrop.REQUEST_CROP && resultCode == Activity.RESULT_OK) {
            val uri = UCrop.getOutput(data!!)
            val photolist: MutableList<PictureModel> = ArrayList()
            val model = PictureModel()
            val imgPath = getGalleryPhoto(uri)
            model.path = imgPath
            photolist.add(model)
            setView(photolist)
        }
    }

    private fun setView(photolist: List<PictureModel>) {
        val linearLayoutManager = LinearLayoutManager(this)
        linearLayoutManager.orientation = LinearLayoutManager.HORIZONTAL
        recyclerView!!.layoutManager = linearLayoutManager
        val imageAdapter = ImageAdapter(this)
        imageAdapter.setDataList(photolist)
        recyclerView!!.adapter = imageAdapter
    }
}