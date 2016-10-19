package com.siyanhui.mojif.bqliveapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.siyanhui.mojif.bqlive.BQLive;
import com.siyanhui.mojif.bqlive.support.BQLiveSupport;
import com.siyanhui.mojif.bqlive.support.api.callback.BQLDownloadCallback;
import com.siyanhui.mojif.bqlive.support.api.callback.BQLGetGiftsCallback;
import com.siyanhui.mojif.bqlive.support.model.BQLGift;
import com.siyanhui.mojif.bqliveapp.adapter.GiftListAdapter;

import java.util.List;

/**
 * 提供一个礼物包的列表
 * Created by lixiao on 16-10-13.
 */
public class EntranceActivity extends Activity {
    private static final int LIST_GOT = 100, GIFT_DOWNLOADED = 101, GIFT_UPDATED = 102;
    private GiftListAdapter mAdapter;
    private Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case LIST_GOT:
                    List<BQLGift> gifts = castAdapterGifts(msg.obj);
                    mAdapter.setGifts(gifts);
                    mAdapter.notifyDataSetChanged();
                    break;
                case GIFT_DOWNLOADED:
                    Toast.makeText(EntranceActivity.this, R.string.completing_download, Toast.LENGTH_SHORT).show();
                    mAdapter.notifyDataSetChanged();
                    break;
                case GIFT_UPDATED:
                    Toast.makeText(EntranceActivity.this, R.string.completing_update, Toast.LENGTH_SHORT).show();
                    mAdapter.notifyDataSetChanged();
            }
        }

        @SuppressWarnings("unchecked")
        private List<BQLGift> castAdapterGifts(Object gifts) {
            return (List<BQLGift>) gifts;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);
        mAdapter = new GiftListAdapter();
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Object object = parent.getItemAtPosition(position);
                if (object != null && object instanceof BQLGift) {
                    BQLGift gift = (BQLGift) object;
                    if (gift.isNeedingUpdate()) {
                        Toast.makeText(EntranceActivity.this, R.string.starting_update, Toast.LENGTH_SHORT).show();
                        //升级的时候，首先删除，然后下载
                        BQLiveSupport.deleteGiftByID(gift.getGuid());
                        BQLive.reportGiftUpdated("userId", "userName", gift.getGuid(), gift.getName(), gift.getPrice());
                        BQLiveSupport.downloadGift(gift, new BQLDownloadCallback() {
                            @Override
                            public void onSuccess() {
                                mainHandler.sendEmptyMessage(GIFT_UPDATED);
                            }

                            @Override
                            public void onFailure() {
                            }
                        });
                    } else if (BQLiveSupport.localGiftExists(gift.getGuid())) {
                        //调用MainActivity进行播放
                        Intent intent = new Intent(EntranceActivity.this, MainActivity.class);
                        intent.putExtra("guid", gift.getGuid());
                        intent.putExtra("name", gift.getName());
                        intent.putExtra("price", gift.getPrice());
                        intent.putExtra("fullScreen", gift.getFullScreenType() == 1);
                        startActivity(intent);
                    } else {
                        Toast.makeText(EntranceActivity.this, R.string.starting_download, Toast.LENGTH_SHORT).show();
                        //下载的时候，直接使用BQLiveSupport中的函数
                        BQLive.reportGiftDownloaded("userId", "userName", gift.getGuid(), gift.getName(), gift.getPrice());
                        BQLiveSupport.downloadGift(gift, new BQLDownloadCallback() {
                            @Override
                            public void onSuccess() {
                                mainHandler.sendEmptyMessage(GIFT_DOWNLOADED);
                            }

                            @Override
                            public void onFailure() {
                            }
                        });
                    }
                }
            }
        });
        //异步获取服务器上的礼物列表
        BQLiveSupport.getRemoteGiftList(new BQLGetGiftsCallback() {
            @Override
            public void onSuccess(List<BQLGift> gifts) {
                mainHandler.sendMessage(mainHandler.obtainMessage(LIST_GOT, gifts));
            }

            @Override
            public void onFailure(Throwable e) {

            }
        });
    }
}
