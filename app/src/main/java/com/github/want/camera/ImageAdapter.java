package com.github.want.camera;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.github.want.camera.utils.OnClickListener;
import com.github.want.camera.utils.RecyclerViewBaseAdapter;
import com.github.want.camera.utils.SuperViewHolder;

public class ImageAdapter extends RecyclerViewBaseAdapter<PictureModel> {

    private int photoSselect ;
    public ImageAdapter(Context context) {
        super(context);
    }

    @Override
    public int getLayoutId() {
        return R.layout.item_image;
    }

    @Override
    public void onBindItemHolder(SuperViewHolder holder, final int position) {
        RelativeLayout rl = holder.getView(R.id.rl);
        ImageView image = holder.getView(R.id.image);
        ImageView delete = holder.getView(R.id.delete);
        TextView image_name_tv = holder.getView(R.id.image_name_tv);
        PictureModel pictureModel = mDataList.get(position);
        if (TextUtils.isEmpty(pictureModel.getPath())) {
            int no_png = R.mipmap.no_png;
            //加载图片
            Glide.with(mContext).load(no_png).into(image);
            delete.setVisibility(View.GONE);
        } else {
            delete.setVisibility(View.VISIBLE);
            Glide.with(mContext).load(mDataList.get(position).getPath()).into(image);
        }
        image_name_tv.setText(pictureModel.getName());
        if (photoSselect == position) {
            int circle_red = R.drawable.circle_red;
            rl.setBackgroundResource(circle_red);
        } else {
            int circle_transport = R.drawable.circle_transport;
            rl.setBackgroundResource(circle_transport);
        }
        delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (clickListenner != null) {
                    clickListenner.onClick(position);
                }
            }
        });
    }

    private OnClickListener clickListenner;

    public void setDeleteClickListenner(OnClickListener clickListenner) {
        this.clickListenner = clickListenner;
    }

    public void setPhotoSelect(int photoSselect){
        this.photoSselect = photoSselect;
        notifyDataSetChanged();
    }
}
