package com.github.want.camera

import android.content.Context
import android.text.TextUtils
import android.view.View
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.bumptech.glide.Glide
import com.github.want.camera.model.PictureModel
import com.github.want.camera.utils.OnClickListener
import com.github.want.camera.utils.RecyclerViewBaseAdapter
import com.github.want.camera.utils.SuperViewHolder

class ImageAdapter(context: Context?) : RecyclerViewBaseAdapter<PictureModel?>(context) {
    private var photoSselect = 0
    override fun getLayoutId(): Int {
        return R.layout.item_image
    }

    override fun onBindItemHolder(holder: SuperViewHolder, position: Int) {
        val rl = holder.getView<RelativeLayout>(R.id.rl)
        val image = holder.getView<ImageView>(R.id.image)
        val delete = holder.getView<ImageView>(R.id.delete)
        val image_name_tv = holder.getView<TextView>(R.id.image_name_tv)
        val pictureModel = mDataList[position]!!
        if (TextUtils.isEmpty(pictureModel.path)) {
            val no_png = R.mipmap.no_png
            //加载图片
            Glide.with(mContext).load(no_png).into(image)
            delete.visibility = View.GONE
        } else {
            delete.visibility = View.VISIBLE
            Glide.with(mContext).load(mDataList[position]!!.path).into(image)
        }
        image_name_tv.text = pictureModel.name
        if (photoSselect == position) {
            val circle_red = R.drawable.circle_red
            rl.setBackgroundResource(circle_red)
        } else {
            val circle_transport = R.drawable.circle_transport
            rl.setBackgroundResource(circle_transport)
        }
        delete.setOnClickListener {
            if (clickListenner != null) {
                clickListenner!!.onClick(position)
            }
        }
    }

    private var clickListenner: OnClickListener? = null
    fun setDeleteClickListenner(clickListenner: OnClickListener?) {
        this.clickListenner = clickListenner
    }

    fun setPhotoSelect(photoSselect: Int) {
        this.photoSselect = photoSselect
        notifyDataSetChanged()
    }
}