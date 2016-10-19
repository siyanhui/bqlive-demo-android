package com.siyanhui.mojif.bqliveapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;

import com.siyanhui.mojif.bqlive.BQLive;
import com.siyanhui.mojif.bqliveapp.graphics.BQLAnimationContext;
import com.siyanhui.mojif.bqliveapp.graphics.BQLPngSequencePlayer;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 用于播放动画的View
 * Created by fantasy on 16/9/6.
 */
public class BQLAnimationView extends View {
    private boolean mFullScreen;
    private Paint mFramePaint = new Paint();
    private Paint mFrameAlphaPaint = new Paint();
    private Paint mHostAvatarPaint = new Paint();
    private Paint mSenderAvatarPaint = new Paint();
    private BQLAnimationContext mAnimationContext;
    private Bitmap mBitmap;
    private Bitmap mAlphaBitmap;
    private Matrix mHostAvatarMatrix;
    private int mHostAvatarAlpha;
    private Matrix mSenderAvatarMatrix;
    private int mSenderAvatarAlpha;
    private Matrix mHostNickNameMatrix;
    private int mHostNickNameAlpha;
    private Matrix mSenderNickNameMatrix;
    private int mSenderNickNameAlpha;
    private float mHostNickNameHeight;
    private float mSenderNickNameHeight;
    private Bitmap mHostAvatar;
    private Bitmap mSenderAvatar;
    private TextPaint mSenderTextPaint;
    private TextPaint mSenderStrokeTextPaint;
    private TextPaint mHostTextPaint;
    private TextPaint mHostStrokeTextPaint;
    private Paint mBorderPaint;
    private String mHostNickName;
    private String mSenderNickName;
    private OnCompletionListener mOnCompletionListener;
    private String[] mSubAnimationNames;
    private Map<String, Bitmap> mSubAnimationSprites = new HashMap<>();
    private Map<String, Matrix> mSubAnimationMatrices = new HashMap<>();
    private Map<String, Paint> mSubAnimationPaints = new HashMap<>();

    public BQLAnimationView(Context context) {
        super(context);
    }

    public BQLAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public BQLAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 绘制一张图片
     *
     * @param canvas 目标画布
     * @param bitmap 待绘制的图片
     * @param matrix 图片需要做的变换
     * @param alpha  绘制的透明度
     * @param paint  绘制用的Paint
     */
    private static void drawBitmap(Canvas canvas, Bitmap bitmap, @Nullable Matrix matrix, int alpha, @Nullable Paint paint) {
        if (matrix != null && paint != null) {
            int saveCount = canvas.getSaveCount();
            canvas.save();
            canvas.concat(matrix);
            paint.setAlpha(alpha);
            canvas.drawBitmap(bitmap, 0, 0, paint);
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * 绘制一段文字
     *
     * @param canvas      目标画布
     * @param text        待绘制的文字
     * @param matrix      文字需要做的变换
     * @param alpha       绘制的透明度
     * @param height      文字高度
     * @param paint       绘制文字用的Paint
     * @param strokePaint 绘制文字描边用的Paint
     */
    private static void drawText(Canvas canvas, String text, @Nullable Matrix matrix, int alpha, float height, @Nullable Paint paint, @Nullable Paint strokePaint) {
        if (matrix != null && (paint != null || strokePaint != null)) {
            int saveCount = canvas.getSaveCount();
            canvas.save();
            canvas.concat(matrix);
            if (paint != null) {
                paint.setAlpha(alpha);
                canvas.drawText(text, 0, height / 2, paint);
            }
            if (strokePaint != null) {
                strokePaint.setAlpha(alpha);
                canvas.drawText(text, 0, height / 2, strokePaint);
            }
            canvas.restoreToCount(saveCount);
        }
    }

    public void setOnCompletionListener(OnCompletionListener onCompletionListener) {
        mOnCompletionListener = onCompletionListener;
    }

    /**
     * 播放动画
     * 播放动画的准备工作包括用BQLive的方法获得动画配置文件、设置好播放过程中需要用到的变量以及生成好需要绘制的Bitmap
     *
     * @param animationDirectory 动画在文件系统中的路径
     * @param hostNickname       主播昵称
     * @param senderNickname     送礼者昵称
     * @param fullScreen         是否全屏
     */
    public void playAnimation(String animationDirectory, String hostNickname, String senderNickname, boolean fullScreen) {
        mFullScreen = fullScreen;

        /**
         * 以下两个Paint需要配合使用。为了节省空间，动画中的每一帧主图都被存贮为了颜色通道和透明度通道两张图片，需要用这两个Paint分别绘制。
         */
        mFramePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));//颜色通道在透明度通道之后绘制，这个设置可以在绘制时保留像素的透明度。
        mFrameAlphaPaint.setColorFilter(new ColorMatrixColorFilter(new ColorMatrix(new float[]{
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                0, 0, 0, 0, 0,
                1, 0, 0, 0, 0})));//透明度通道是一张黑白图片，这个矩阵可以把图片上每一个像素的亮度转变为透明度。

        BQLive.AnimationConfig config = null;
        try {
            config = BQLive.generateConfig(animationDirectory);//生成配置文件
        } catch (JSONException | IOException e) {
            e.printStackTrace();
        }
        if (config == null) {
            return;
        }
        mHostNickName = hostNickname;
        mSenderNickName = senderNickname;
        BQLive.NicknameConfig hostNickNameConfig = config.getHostNickNameConfig();
        BQLive.NicknameConfig senderNickNameConfig = config.getSenderNickNameConfig();
        BQLive.SpriteConfig hostAvatarConfig = config.getHostAvatarConfig();
        BQLive.SpriteConfig senderAvatarConfig = config.getSenderAvatarConfig();
        mBorderPaint = new Paint();
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setStyle(Paint.Style.STROKE);//设置填充样式为描边

        if (hostAvatarConfig != null) {
            mHostAvatar = createSpriteBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.host_avatar), hostAvatarConfig);
            if (!TextUtils.isEmpty(hostAvatarConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + hostAvatarConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mHostAvatarPaint.setShadowLayer(hostAvatarConfig.getShadowBlur(), hostAvatarConfig.getShadowX(), hostAvatarConfig.getShadowY(), shadowColor);
                } else {
                    mHostAvatarPaint.clearShadowLayer();
                }
            }
        }
        if (senderAvatarConfig != null) {
            mSenderAvatar = createSpriteBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.sender_avatar), senderAvatarConfig);
            if (!TextUtils.isEmpty(senderAvatarConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + senderAvatarConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mSenderAvatarPaint.setShadowLayer(senderAvatarConfig.getShadowBlur(), senderAvatarConfig.getShadowX(), senderAvatarConfig.getShadowY(), shadowColor);
                } else {
                    mSenderAvatarPaint.clearShadowLayer();
                }
            }
        }

        if (senderNickNameConfig != null) {
            mSenderTextPaint = new TextPaint();
            mSenderTextPaint.setTypeface(Typeface.DEFAULT);
            mSenderTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mSenderTextPaint.setColor(Color.parseColor("#" + senderNickNameConfig.getColor()));
            mSenderTextPaint.setAntiAlias(true);
            mSenderTextPaint.setTextAlign(Paint.Align.LEFT);
            mSenderTextPaint.setTextSize(25);
            if (!TextUtils.isEmpty(senderNickNameConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + senderNickNameConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mSenderTextPaint.setShadowLayer(senderNickNameConfig.getShadowBlur(), senderNickNameConfig.getShadowX(), senderNickNameConfig.getShadowY(), shadowColor);
                } else {
                    mSenderTextPaint.clearShadowLayer();
                }
            }

            if (!TextUtils.isEmpty(senderNickNameConfig.getBorderColor())) {
                mSenderStrokeTextPaint = new TextPaint();
                mSenderStrokeTextPaint.setColor(Color.parseColor("#" + senderNickNameConfig.getBorderColor()));
                mSenderStrokeTextPaint.setStrokeWidth(senderNickNameConfig.getBorderWidth());
                mSenderStrokeTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mSenderStrokeTextPaint.setAntiAlias(true);
                mSenderStrokeTextPaint.setTextSize(25);
                mSenderStrokeTextPaint.setTextAlign(Paint.Align.LEFT);
            }
        }


        if (hostNickNameConfig != null) {
            mHostTextPaint = new TextPaint();
            mHostTextPaint.setTypeface(Typeface.DEFAULT);
            mHostTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
            mHostTextPaint.setColor(Color.parseColor("#" + hostNickNameConfig.getColor()));
            mHostTextPaint.setAntiAlias(true);
            mHostTextPaint.setTextAlign(Paint.Align.LEFT);
            mHostTextPaint.setTextSize(25);
            if (!TextUtils.isEmpty(hostNickNameConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + hostNickNameConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mHostTextPaint.setShadowLayer(hostNickNameConfig.getShadowBlur(), hostNickNameConfig.getShadowX(), hostNickNameConfig.getShadowY(), shadowColor);
                } else {
                    mHostTextPaint.clearShadowLayer();
                }
            }

            if (!TextUtils.isEmpty(hostNickNameConfig.getBorderColor())) {
                mHostStrokeTextPaint = new TextPaint();
                mHostStrokeTextPaint.setColor(Color.parseColor("#" + hostNickNameConfig.getBorderColor()));
                mHostStrokeTextPaint.setStrokeWidth(hostNickNameConfig.getBorderWidth());
                mHostStrokeTextPaint.setStyle(Paint.Style.FILL_AND_STROKE);
                mHostStrokeTextPaint.setAntiAlias(true);
                mHostStrokeTextPaint.setTextSize(25);
                mHostStrokeTextPaint.setTextAlign(Paint.Align.LEFT);
            }
        }
        this.mAnimationContext = new BQLAnimationContext(config.getHostAvatarAnimationFrames(), config.getSenderAvatarAnimationFrames(), config.getHostNickName(), config.getSenderNickName(), mHostAvatar, mSenderAvatar, mHostTextPaint, mSenderTextPaint, mHostNickName, mSenderNickName, hostNickNameConfig, senderNickNameConfig);

        Map<String, BQLive.SubAnimationConfig> subAnimations = config.getSubAnimations();
        if (subAnimations != null) {
            mSubAnimationNames = new String[subAnimations.keySet().size()];
            int i = 0;
            for (String name : subAnimations.keySet()) {
                mSubAnimationNames[i++] = name;
                Bitmap sprite = createSpriteBitmap(BitmapFactory.decodeFile(animationDirectory + File.separator + name), subAnimations.get(name).getConfig());
                mSubAnimationSprites.put(name, sprite);
                Paint paint = new Paint();
                BQLive.SpriteConfig spriteConfig = subAnimations.get(name).getConfig();
                if (!TextUtils.isEmpty(spriteConfig.getShadowColor())) {
                    int shadowColor = Color.parseColor("#" + spriteConfig.getShadowColor());
                    if (Color.alpha(shadowColor) != 0) {
                        paint.setShadowLayer(spriteConfig.getShadowBlur(), spriteConfig.getShadowX(), spriteConfig.getShadowY(), shadowColor);
                    }
                }
                mSubAnimationPaints.put(name, paint);
                mAnimationContext.addSubAnimation(name, sprite, subAnimations.get(name));
            }
        }

        /**
         * 生成主图列表
         */
        String[] colorFiles = new String[config.getFrame()];
        String[] alphaFiles = new String[config.getFrame()];
        for (int i = 0; i < config.getFrame(); ++i) {
            colorFiles[i] = animationDirectory + File.separator + String.valueOf(i) + "-a.jpg";
            alphaFiles[i] = animationDirectory + File.separator + String.valueOf(i) + "-b.jpg";
        }
        new BQLPngSequencePlayer(colorFiles, alphaFiles, config.getFrameIndices(), this, config.getType() == 0 ? 80 : 10000).start();
    }

    /**
     * 根据原图生成带描边的圆角图片
     */
    private Bitmap createSpriteBitmap(Bitmap source, BQLive.SpriteConfig config) {
        float targetWidth = config.getWidth(), targetHeight = config.getHeight();
        float scale = 1 / scaleToFill(source.getWidth(), source.getHeight(), targetWidth, targetHeight);
        float borderWidth = config.getBorderWidth() * scale;
        float borderRadius = config.getCornerRadius() * scale;
        mBorderPaint.setColor(Color.parseColor("#" + config.getBorderColor()));
        mBorderPaint.setStrokeWidth(borderWidth);//设置笔触宽度
        Paint paint = new Paint();
        paint.setAntiAlias(true);
        int bitmapWidth = (int) (targetWidth * scale + borderWidth * 2);
        int bitmapHeight = (int) (targetHeight * scale + borderWidth * 2);
        Bitmap target = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(target);

        RectF rect = new RectF(borderWidth, borderWidth, bitmapWidth - borderWidth, bitmapHeight - borderWidth);
        canvas.drawRoundRect(rect, borderRadius, borderRadius, paint);
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC_IN));
        canvas.drawBitmap(source, borderWidth - (source.getWidth() - targetWidth * scale) / 2, borderWidth - (source.getHeight() - targetWidth * scale) / 2, paint);

        float borderHalfWidth = borderWidth / 2;
        RectF rectF = new RectF(borderHalfWidth, borderHalfWidth, bitmapWidth - borderHalfWidth, bitmapHeight - borderHalfWidth);
        canvas.drawRoundRect(rectF, borderRadius + borderHalfWidth, borderRadius + borderHalfWidth, mBorderPaint);
        return target;
    }

    public BQLAnimationContext getAnimationContext() {
        return mAnimationContext;
    }

    /**
     * 给BQLPngSequencePlayer调用的函数，设置待显示的数据
     */
    public void setFrame(Bitmap bitmap, Bitmap alphaBitmap, Matrix hostAvatarMatrix, int hostAvatarAlpha, Matrix senderAvatarMatrix, int senderAvatarAlpha, Matrix hostNickNameMatrix, int hostNickNameAlpha, Matrix senderNickNameMatrix, int senderNickNameAlpha, float hostNickNameHeight, float senderNickNameHeight, Map<String, Matrix> matrices) {
        mBitmap = bitmap;
        mAlphaBitmap = alphaBitmap;
        mHostAvatarMatrix = hostAvatarMatrix;
        mHostAvatarAlpha = hostAvatarAlpha;
        mSenderAvatarMatrix = senderAvatarMatrix;
        mSenderAvatarAlpha = senderAvatarAlpha;
        mHostNickNameMatrix = hostNickNameMatrix;
        mHostNickNameAlpha = hostNickNameAlpha;
        mSenderNickNameMatrix = senderNickNameMatrix;
        mSenderNickNameAlpha = senderNickNameAlpha;
        mHostNickNameHeight = hostNickNameHeight;
        mSenderNickNameHeight = senderNickNameHeight;
        mSubAnimationMatrices = matrices;
        postInvalidate();
    }

    /**
     * 结束播放，数据归零
     */
    public void endAnimation() {
        mBitmap = null;
        mHostAvatarMatrix = null;
        mHostAvatarAlpha = 0;
        mSenderAvatarMatrix = null;
        mSenderAvatarAlpha = 0;
        mHostNickNameMatrix = null;
        mHostNickNameAlpha = 0;
        mSenderNickNameMatrix = null;
        mSenderNickNameAlpha = 0;
        mHostNickNameHeight = 0;
        mSenderNickNameHeight = 0;
        mHostTextPaint = null;
        mHostStrokeTextPaint = null;
        mSenderTextPaint = null;
        mSenderStrokeTextPaint = null;
        mSubAnimationNames = null;
        mSubAnimationSprites.clear();
        mSubAnimationMatrices.clear();
        mSubAnimationPaints.clear();
        postInvalidate();
        mOnCompletionListener.onCompletion();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (mBitmap != null) {
            int saveCount = canvas.getSaveCount();
            canvas.save();

            int drawableWidth = mBitmap.getWidth();
            int drawableHeight = mBitmap.getHeight();
            if (mFullScreen) {//如果这是一个全屏表情，就把canvas缩放到整个控件的大小
                float scale = scaleToFill(drawableWidth, drawableHeight, getWidth(), getHeight());
                canvas.scale(scale, scale);
            }
            //绘制主图时，首先画上透明度通道，然后画上颜色通道
            if (mAlphaBitmap != null) {
                canvas.drawBitmap(mAlphaBitmap, 0, 0, mFrameAlphaPaint);
            }
            canvas.drawBitmap(mBitmap, 0, 0, mFramePaint);

            //绘制头像、昵称及子图
            drawBitmap(canvas, mHostAvatar, mHostAvatarMatrix, mHostAvatarAlpha, mHostAvatarPaint);
            drawBitmap(canvas, mSenderAvatar, mSenderAvatarMatrix, mSenderAvatarAlpha, mSenderAvatarPaint);
            drawText(canvas, mHostNickName, mHostNickNameMatrix, mHostNickNameAlpha, mHostNickNameHeight, mHostTextPaint, mHostStrokeTextPaint);
            drawText(canvas, mSenderNickName, mSenderNickNameMatrix, mSenderNickNameAlpha, mSenderNickNameHeight, mSenderTextPaint, mSenderStrokeTextPaint);
            if (mSubAnimationNames != null) for (String name : mSubAnimationNames) {
                drawBitmap(canvas, mSubAnimationSprites.get(name), mSubAnimationMatrices.get(name), 255, mSubAnimationPaints.get(name));
            }

            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * 计算得到如果要将给定尺寸缩放到能够包含限定尺寸，且保持长宽比的话，最小的倍率是多少
     *
     * @param width       给定的宽度
     * @param height      给定的高度
     * @param limitWidth  限定宽度
     * @param limitHeight 限定高度
     * @return 倍率
     */
    private float scaleToFill(float width, float height, float limitWidth, float limitHeight) {
        int fittedHeight = (int) (limitWidth / width * height);
        if (fittedHeight > limitHeight) {
            return limitWidth / width;
        } else {
            return limitHeight / height;
        }
    }

    public interface OnCompletionListener {
        void onCompletion();
    }

}
