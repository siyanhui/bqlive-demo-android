package com.siyanhui.mojif.bqliveapp.graphics;

import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextPaint;

import com.siyanhui.mojif.bqlive.BQLive;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 含有动画在播放过程中需要的各种相关信息，和BQLPngSequencePlayer配合使用
 * 由于动画播放的参数十分繁杂，将它们整合成一个类有助于简化代码
 * Created by lixiao on 16-9-26.
 */
public class BQLAnimationContext {
    /**
     * 主播和送礼者头像的帧参数列表，用于计算头像矩阵
     */
    private BQLive.FrameConfig[] mHostAvatarAnimationFrames;
    private BQLive.FrameConfig[] mSenderAvatarAnimationFrames;
    /**
     * 这里存储头像Bitmap的宽高信息
     */
    private int mHostAvatarWidth;
    private int mHostAvatarHeight;
    private int mSenderAvatarWidth;
    private int mSenderAvatarHeight;
    /**
     * 子动画的参数和头像类似，包括帧参数列表和Bitmap的宽高，唯一不确定的是子动画的数量不确定，因此需要记下每个子动画的名称，并将所有参数放在Map中
     */
    private List<String> mSubAnimationNames = new ArrayList<>();
    private Map<String, BQLive.SubAnimationConfig> mSubAnimations = new HashMap<>();//用于获取子动画的帧参数列表
    private Map<String, Integer> mSubAnimationSpriteWidths = new HashMap<>();
    private Map<String, Integer> mSubAnimationSpriteHeights = new HashMap<>();
    /**
     * 以下八个个参数用于计算昵称矩阵
     */
    private String mHostName;
    private String mSenderName;
    private BQLive.FrameConfig[] mHostNickNameFrames;
    private BQLive.FrameConfig[] mSenderNickNameFrames;
    private BQLive.NicknameConfig mHostNickNameConfig;   //用于获取昵称的对齐方式
    private BQLive.NicknameConfig mSenderNickNameConfig;
    private TextPaint mHostNickNamePaint;                //用于计算昵称在屏幕上的大小
    private TextPaint mSenderNickNamePaint;

    /**
     * 除子动画之外，所有参数的传入都在构造函数里完成
     */
    public BQLAnimationContext(BQLive.FrameConfig[] hostAvatarAnimationFrames, BQLive.FrameConfig[] senderAvatarAnimationFrames, BQLive.FrameConfig[] hostNickNameFrames, BQLive.FrameConfig[] senderNickNameFrames, Bitmap hostAvatar, Bitmap senderAvatar, TextPaint hostNickNamePaint, TextPaint senderNickNamePaint, String hostNickName, String senderNickName, BQLive.NicknameConfig hostNickNameConfig, BQLive.NicknameConfig senderNickNameConfig) {
        mHostAvatarAnimationFrames = hostAvatarAnimationFrames;
        mSenderAvatarAnimationFrames = senderAvatarAnimationFrames;
        mHostNickNameFrames = hostNickNameFrames;
        mSenderNickNameFrames = senderNickNameFrames;
        if (hostAvatar != null) {
            mHostAvatarWidth = hostAvatar.getWidth();
            mHostAvatarHeight = hostAvatar.getHeight();
        }
        if (senderAvatar != null) {
            mSenderAvatarWidth = senderAvatar.getWidth();
            mSenderAvatarHeight = senderAvatar.getHeight();
        }
        mHostNickNamePaint = hostNickNamePaint;
        mSenderNickNamePaint = senderNickNamePaint;
        mHostName = hostNickName;
        mSenderName = senderNickName;
        mHostNickNameConfig = hostNickNameConfig;
        mSenderNickNameConfig = senderNickNameConfig;
    }

    /**
     * 由于子动画的数量不定，因此每个子动画的参数都用调用本函数的方式传入
     *
     * @param name   子动画名称
     * @param sprite 子动画Bitmap
     * @param config 子动画参数
     */
    public void addSubAnimation(String name, Bitmap sprite, BQLive.SubAnimationConfig config) {
        mSubAnimationNames.add(name);
        mSubAnimationSpriteWidths.put(name, sprite.getWidth());
        mSubAnimationSpriteHeights.put(name, sprite.getHeight());
        mSubAnimations.put(name, config);
    }

    public int getHostAvatarWidth() {
        return mHostAvatarWidth;
    }

    public int getHostAvatarHeight() {
        return mHostAvatarHeight;
    }

    /**
     * 返回某一帧主播头像的参数
     * @param frameNumber 帧序号
     * @return 帧参数，可能为null
     */
    public BQLive.FrameConfig getHostAvatarAnimationFrame(int frameNumber) {
        return mHostAvatarAnimationFrames == null ? null : mHostAvatarAnimationFrames[frameNumber];
    }

    public int getSenderAvatarWidth() {
        return mSenderAvatarWidth;
    }

    public int getSenderAvatarHeight() {
        return mSenderAvatarHeight;
    }

    /**
     * 返回某一帧送礼者头像的参数
     * @param frameNumber 帧序号
     * @return 帧参数，可能为null
     */
    public BQLive.FrameConfig getSenderAvatarAnimationFrame(int frameNumber) {
        return mSenderAvatarAnimationFrames == null ? null : mSenderAvatarAnimationFrames[frameNumber];
    }

    /**
     * 返回某一帧的主播昵称参数，并将主播昵称在屏幕上占用空间的尺寸写入rectF
     * @param frameNumber 帧序号
     * @param rectF 待写入尺寸的RectF
     * @return 指定帧的参数，可能为null
     */
    public BQLive.FrameConfig prepareHostNickName(int frameNumber, RectF rectF) {
        if (mHostNickNameFrames == null) return null;
        BQLive.FrameConfig frame = mHostNickNameFrames[frameNumber];
        if (frame.getWidth() == 0 || frame.getHeight() == 0 || frame.getScale() == 0 || frame.getAlpha() == 0) {
            return frame;
        }
        float textWidth = mHostNickNamePaint.measureText(mHostName);
        Paint.FontMetrics metrics = mHostNickNamePaint.getFontMetrics();
        float textHeight = metrics.bottom - metrics.top;
        rectF.right = textWidth;
        rectF.bottom = textHeight;
        return frame;
    }

    public int getHostNickNameAlignment() {
        return mHostNickNameConfig == null ? 1 : mHostNickNameConfig.getAlignment();
    }

    /**
     * 返回某一帧的送礼者昵称参数，并将主播昵称在屏幕上占用空间的尺寸写入rectF
     * @param frameNumber 帧序号
     * @param rectF 待写入尺寸的RectF
     * @return 指定帧的参数，可能为null
     */
    public BQLive.FrameConfig prepareSenderNickName(int frameNumber, RectF rectF) {
        if (mSenderNickNameFrames == null) return null;
        BQLive.FrameConfig frame = mSenderNickNameFrames[frameNumber];
        if (frame.getWidth() == 0 || frame.getHeight() == 0 || frame.getScale() == 0 || frame.getAlpha() == 0) {
            return frame;
        }
        float textWidth = mSenderNickNamePaint.measureText(mSenderName);
        Paint.FontMetrics metrics = mSenderNickNamePaint.getFontMetrics();
        float textHeight = metrics.bottom - metrics.top;
        rectF.right = textWidth;
        rectF.bottom = textHeight;
        return frame;
    }

    public int getSenderNickNameAlignment() {
        return mSenderNickNameConfig == null ? 1 : mSenderNickNameConfig.getAlignment();
    }

    public List<String> getSubAnimationNames() {
        return mSubAnimationNames;
    }

    public int getSubAnimationSpriteWidth(String name) {
        return mSubAnimationSpriteWidths.get(name);
    }

    public int getSubAnimationSpriteHeight(String name) {
        return mSubAnimationSpriteHeights.get(name);
    }

    public BQLive.SubAnimationConfig getSubAnimationConfig(String name) {
        return mSubAnimations.get(name);
    }
}
