package com.siyanhui.mojif.bqliveapp;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.View;
import android.widget.Toast;

import com.siyanhui.mojif.bqlive.BQLive;
import com.siyanhui.mojif.bqlive.support.BQLiveSupport;

public class MainActivity extends Activity {
    private BQLAnimationView mImageView;
    private View mActionBar;
    private Handler mainHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            if (mActionBar != null) {
                mActionBar.setVisibility(View.VISIBLE);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.change_facing).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ((BQLCameraPreview) findViewById(R.id.video)).changeFacing();
            }
        });

        /**
         * 这个View用于播放礼物动画
         */
        mImageView = (BQLAnimationView) findViewById(R.id.image_live);
        mImageView.setOnCompletionListener(new BQLAnimationView.OnCompletionListener() {
            @Override
            public void onCompletion() {
                mainHandler.sendEmptyMessage(0);
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            mActionBar = findViewById(R.id.actions);
            mActionBar.setVisibility(View.VISIBLE);
            final String guid = bundle.getString("guid");
            final String name = bundle.getString("name");
            final String price = bundle.getString("price");
            final boolean fullScreen = bundle.getBoolean("fullScreen");
            findViewById(R.id.send).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /**
                     * 送礼物的时候，上报送礼物统计事件
                     */
                    BQLive.reportGiftSent("userId", "userName", "hostId", "hostName", guid, name, price);
                    mActionBar.setVisibility(View.GONE);
                    mImageView.playAnimation(BQLiveSupport.getLocalGiftPath(guid), "表情主播", "表情观众", fullScreen);
                }
            });

            findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    /**
                     * 观看礼物的时候，上报观看礼物统计事件
                     */
                    BQLive.reportGiftViewed("userId", "userName", "hostId", "hostName", guid, name, price);
                    mActionBar.setVisibility(View.GONE);
                    mImageView.playAnimation(BQLiveSupport.getLocalGiftPath(guid), "表情主播", "表情观众", fullScreen);
                }
            });
            findViewById(R.id.delete).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    BQLiveSupport.deleteGiftByID(guid);
                    Toast.makeText(MainActivity.this, "删除完毕", Toast.LENGTH_SHORT).show();
                    finish();
                }
            });
        }
    }
}
