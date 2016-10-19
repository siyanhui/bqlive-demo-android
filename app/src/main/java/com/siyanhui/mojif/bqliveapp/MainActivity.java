package com.siyanhui.mojif.bqliveapp;

import android.app.Activity;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import android.widget.VideoView;

import com.siyanhui.mojif.bqlive.BQLive;
import com.siyanhui.mojif.bqlive.support.BQLiveSupport;

public class MainActivity extends Activity {
    VideoView mVideoView;
    private BQLAnimationView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mVideoView = (VideoView) findViewById(R.id.video);
        mVideoView.setVideoURI(Uri.parse("android.resource://com.siyanhui.mojif.bqliveapp/" + R.raw.tos));
        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mp.start();
            }
        });
        mVideoView.start();

        /**
         * 这个View用于播放礼物动画
         */
        mImageView = (BQLAnimationView) findViewById(R.id.image_live);
        mImageView.setOnCompletionListener(new BQLAnimationView.OnCompletionListener() {
            @Override
            public void onCompletion() {
                setButtonsClickable(true);
            }
        });

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
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
                    setButtonsClickable(false);
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
                    setButtonsClickable(false);
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

    @Override
    protected void onResume() {
        super.onResume();
        if (!mVideoView.isPlaying()) {
            mVideoView.start();
        }
    }

    private void setButtonsClickable(boolean b) {
        findViewById(R.id.send).setClickable(b);
        findViewById(R.id.view).setClickable(b);
        findViewById(R.id.delete).setClickable(b);
    }
}
