package com.siyanhui.mojif.bqliveapp.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.siyanhui.mojif.bqlive.support.BQLiveSupport;
import com.siyanhui.mojif.bqlive.support.model.BQLGift;
import com.siyanhui.mojif.bqliveapp.R;
import com.squareup.picasso.Picasso;

import java.util.List;

/**
 * 用于列出礼物列表
 * Created by lixiao on 16-10-13.
 */
public class GiftListAdapter extends BaseAdapter {
    private List<BQLGift> mGifts;

    @Override
    public int getCount() {
        if (mGifts != null) return mGifts.size();
        else return 0;
    }

    @Override
    public Object getItem(int position) {
        if (mGifts != null) return mGifts.get(position);
        else return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (mGifts == null) return null;
        BQLGift gift = mGifts.get(position);
        Object object = null;
        if (convertView != null) {
            object = convertView.getTag();
        }
        ViewHolder viewHolder;
        if (object != null && object instanceof ViewHolder) {
            viewHolder = (ViewHolder) object;
        } else {
            viewHolder = new ViewHolder();
            convertView = View.inflate(parent.getContext(), R.layout.item_gift, null);
            viewHolder.thumbnailView = (ImageView) convertView.findViewById(R.id.thumbnail);
            viewHolder.nameView = (TextView) convertView.findViewById(R.id.name);
            viewHolder.statusView = (TextView) convertView.findViewById(R.id.status);
            convertView.setTag(viewHolder);
        }
        Picasso.with(parent.getContext()).load(gift.getThumb()).into(viewHolder.thumbnailView);
        viewHolder.nameView.setText(gift.getName());
        /**
         * 给一个礼物三种状态，没有下载、需要升级、已经下载
         */
        if (gift.isNeedingUpdate()) {
            viewHolder.statusView.setText(R.string.has_update);
        } else if (BQLiveSupport.localGiftExists(gift.getGuid())) {
            viewHolder.statusView.setText(R.string.downloaded);
        } else {
            viewHolder.statusView.setText(R.string.not_downloaded);
        }
        return convertView;
    }

    public void setGifts(List<BQLGift> mGifts) {
        this.mGifts = mGifts;
    }

    private class ViewHolder {
        public ImageView thumbnailView;
        public TextView nameView;
        public TextView statusView;
    }
}
