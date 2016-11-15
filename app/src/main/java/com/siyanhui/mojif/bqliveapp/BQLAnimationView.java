package com.siyanhui.mojif.bqliveapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
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
    private Paint mHostAvatarBorderPaint = new Paint();
    private Paint mSenderAvatarBorderPaint = new Paint();
    private BQLAnimationContext mAnimationContext;
    private Bitmap mBitmap;
    private int mHostAvatarAlpha;
    private int mSenderAvatarAlpha;
    private Matrix mHostNickNameMatrix;
    private int mHostNickNameAlpha;
    private Matrix mSenderNickNameMatrix;
    private int mSenderNickNameAlpha;
    private float mHostNickNameHeight;
    private float mSenderNickNameHeight;
    private Bitmap mHostAvatar;
    private Bitmap mSenderAvatar;
    private BQLive.SpriteConfig mHostAvatarConfig;
    private BQLive.SpriteConfig mSenderAvatarConfig;
    private TextPaint mSenderTextPaint = new TextPaint();
    private TextPaint mSenderStrokeTextPaint = new TextPaint();
    private TextPaint mHostTextPaint = new TextPaint();
    private TextPaint mHostStrokeTextPaint = new TextPaint();
    private String mHostNickName;
    private String mSenderNickName;
    private OnCompletionListener mOnCompletionListener;
    private String[] mSubAnimationNames;
    private Map<String, Bitmap> mSubAnimationSprites = new HashMap<>();
    private Map<String, Matrix> mSubAnimationMatrices = new HashMap<>();
    private Map<String, Paint> mSubAnimationPaints = new HashMap<>();
    private Matrix mHostAvatarOuterMatrix;
    private Matrix mHostAvatarInnerMatrix;
    private RectF mHostAvatarBorderRect;
    private RectF mHostAvatarRect;
    private Matrix mSenderAvatarOuterMatrix;
    private Matrix mSenderAvatarInnerMatrix;
    private RectF mSenderAvatarBorderRect;
    private RectF mSenderAvatarRect;

    public BQLAnimationView(Context context) {
        super(context);
        init();
    }

    public BQLAnimationView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public BQLAnimationView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
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
    private static void drawText(Canvas canvas, String text, @Nullable Matrix matrix, int alpha, float height, Paint paint, Paint strokePaint) {
        if (matrix != null) {
            int saveCount = canvas.getSaveCount();
            canvas.save();
            canvas.concat(matrix);
            strokePaint.setAlpha(alpha);
            canvas.drawText(text, 0, (height - strokePaint.descent() - strokePaint.ascent()) / 2f, strokePaint);
            paint.setAlpha(alpha);
            canvas.drawText(text, 0, (height - paint.descent() - paint.ascent()) / 2f, paint);
            canvas.restoreToCount(saveCount);
        }
    }

    private static void drawAvatar(Canvas canvas, int alpha, int borderWidth, int cornerRadius, Matrix outerMatrix, Matrix innerMatrix, RectF borderRect, RectF rect, Paint borderPaint, Paint paint) {
        if (outerMatrix == null || innerMatrix == null || borderRect == null || paint == null)
            return;
        int saveCount = canvas.getSaveCount();
        canvas.save();
        canvas.concat(outerMatrix);
        //先画边框
        borderPaint.setAlpha(alpha);
        float borderCornerRadius = cornerRadius > 0 ? cornerRadius + borderWidth / 2f : 0;
        canvas.drawRoundRect(borderRect, borderCornerRadius, borderCornerRadius, borderPaint);
        //再画图片
        paint.setAlpha(alpha);
        paint.getShader().setLocalMatrix(innerMatrix);
        canvas.drawRoundRect(rect, cornerRadius, cornerRadius, paint);
        canvas.restoreToCount(saveCount);
    }

    private void init() {
        mHostAvatarPaint.setAntiAlias(true);
        mHostAvatarBorderPaint.setAntiAlias(true);
        mHostAvatarBorderPaint.setStyle(Paint.Style.STROKE);
        mSenderAvatarPaint.setAntiAlias(true);
        mSenderAvatarBorderPaint.setAntiAlias(true);
        mSenderAvatarBorderPaint.setStyle(Paint.Style.STROKE);
        mHostTextPaint.setTypeface(Typeface.DEFAULT);
        mHostTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mHostTextPaint.setAntiAlias(true);
        mHostTextPaint.setTextAlign(Paint.Align.LEFT);
        mHostTextPaint.setTextSize(25);
        mSenderTextPaint.setTypeface(Typeface.DEFAULT);
        mSenderTextPaint.setFlags(Paint.ANTI_ALIAS_FLAG);
        mSenderTextPaint.setAntiAlias(true);
        mSenderTextPaint.setTextAlign(Paint.Align.LEFT);
        mSenderTextPaint.setTextSize(25);
        mHostStrokeTextPaint.setStyle(Paint.Style.STROKE);
        mHostStrokeTextPaint.setAntiAlias(true);
        mHostStrokeTextPaint.setTextSize(25);
        mHostStrokeTextPaint.setTextAlign(Paint.Align.LEFT);
        mSenderStrokeTextPaint.setStyle(Paint.Style.STROKE);
        mSenderStrokeTextPaint.setAntiAlias(true);
        mSenderStrokeTextPaint.setTextSize(25);
        mSenderStrokeTextPaint.setTextAlign(Paint.Align.LEFT);
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
        mHostAvatarConfig = config.getHostAvatarConfig();
        mSenderAvatarConfig = config.getSenderAvatarConfig();

        if (mHostAvatarConfig != null) {
            mHostAvatar = BitmapFactory.decodeResource(getResources(), R.drawable.host_avatar);
            BitmapShader shader = new BitmapShader(mHostAvatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mHostAvatarPaint.setShader(shader);
            mHostAvatarBorderPaint.setStrokeWidth(mHostAvatarConfig.getBorderWidth() * 1.1f);//将边框稍微加粗一点，以避免边框和头像之间出现缝隙
            if (!TextUtils.isEmpty(mHostAvatarConfig.getBorderColor())) {
                int borderColor = Color.parseColor("#" + mHostAvatarConfig.getBorderColor());
                mHostAvatarBorderPaint.setColor(borderColor);
            }
            if (!TextUtils.isEmpty(mHostAvatarConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + mHostAvatarConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mHostAvatarBorderPaint.setShadowLayer(mHostAvatarConfig.getShadowBlur(), mHostAvatarConfig.getShadowX(), mHostAvatarConfig.getShadowY(), shadowColor);
                }
            }
        }
        if (mSenderAvatarConfig != null) {
            mSenderAvatar = BitmapFactory.decodeResource(getResources(), R.drawable.sender_avatar);
            BitmapShader shader = new BitmapShader(mSenderAvatar, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            mSenderAvatarPaint.setShader(shader);
            mSenderAvatarBorderPaint.setStrokeWidth(mSenderAvatarConfig.getBorderWidth() * 1.1f);
            if (!TextUtils.isEmpty(mSenderAvatarConfig.getBorderColor())) {
                int borderColor = Color.parseColor("#" + mSenderAvatarConfig.getBorderColor());
                mSenderAvatarBorderPaint.setColor(borderColor);
            }
            if (!TextUtils.isEmpty(mSenderAvatarConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + mSenderAvatarConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mSenderAvatarBorderPaint.setShadowLayer(mSenderAvatarConfig.getShadowBlur(), mSenderAvatarConfig.getShadowX(), mSenderAvatarConfig.getShadowY(), shadowColor);
                }
            }
        }

        if (senderNickNameConfig != null) {
            mSenderTextPaint.setColor(Color.parseColor("#" + senderNickNameConfig.getColor()));
            if (!TextUtils.isEmpty(senderNickNameConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + senderNickNameConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mSenderStrokeTextPaint.setShadowLayer(senderNickNameConfig.getShadowBlur(), senderNickNameConfig.getShadowX(), senderNickNameConfig.getShadowY(), shadowColor);
                }
            }

            if (!TextUtils.isEmpty(senderNickNameConfig.getBorderColor())) {
                mSenderStrokeTextPaint.setColor(Color.parseColor("#" + senderNickNameConfig.getBorderColor()));
                mSenderStrokeTextPaint.setStrokeWidth(senderNickNameConfig.getBorderWidth());
            }
        }


        if (hostNickNameConfig != null) {
            mHostTextPaint.setColor(Color.parseColor("#" + hostNickNameConfig.getColor()));
            if (!TextUtils.isEmpty(hostNickNameConfig.getShadowColor())) {
                int shadowColor = Color.parseColor("#" + hostNickNameConfig.getShadowColor());
                if (Color.alpha(shadowColor) != 0) {
                    mHostStrokeTextPaint.setShadowLayer(hostNickNameConfig.getShadowBlur(), hostNickNameConfig.getShadowX(), hostNickNameConfig.getShadowY(), shadowColor);
                }
            }

            if (!TextUtils.isEmpty(hostNickNameConfig.getBorderColor())) {
                mHostStrokeTextPaint.setColor(Color.parseColor("#" + hostNickNameConfig.getBorderColor()));
                mHostStrokeTextPaint.setStrokeWidth(hostNickNameConfig.getBorderWidth());
            }
        }
        this.mAnimationContext = new BQLAnimationContext(config.getHostAvatarAnimationFrames(), config.getSenderAvatarAnimationFrames(), mHostAvatarConfig, mSenderAvatarConfig, config.getHostNickName(), config.getSenderNickName(), mHostAvatar, mSenderAvatar, mHostTextPaint, mSenderTextPaint, mHostNickName, mSenderNickName, hostNickNameConfig, senderNickNameConfig);

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
        new BQLPngSequencePlayer(colorFiles, alphaFiles, config.getFrameIndices(), this, config.getType() == 0 ? 1000 / config.getFps() : 10000).start();
    }

    /**
     * 根据原图生成带描边的圆角图片
     */
    private Bitmap createSpriteBitmap(Bitmap source, BQLive.SpriteConfig config) {
        float targetWidth = config.getWidth(), targetHeight = config.getHeight();
        float scale = 1 / scaleToFill(source.getWidth(), source.getHeight(), targetWidth, targetHeight);
        float borderWidth = config.getBorderWidth() * scale;
        float borderRadius = config.getCornerRadius() * scale;
        Paint borderPaint = new Paint();
        borderPaint.setColor(Color.parseColor("#" + config.getBorderColor()));
        borderPaint.setStrokeWidth(borderWidth);//设置笔触宽度
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
        canvas.drawRoundRect(rectF, borderRadius + borderHalfWidth, borderRadius + borderHalfWidth, borderPaint);
        return target;
    }

    public BQLAnimationContext getAnimationContext() {
        return mAnimationContext;
    }

    /**
     * 给BQLPngSequencePlayer调用的函数，设置待显示的数据
     */
    public void setFrame(Bitmap bitmap, Matrix hostAvatarOuterMatrix, Matrix hostAvatarInnerMatrix, RectF hostAvatarBorderRect, RectF hostAvatarRect, Matrix senderAvatarOuterMatrix, Matrix senderAvatarInnerMatrix, RectF senderAvatarBorderRect, RectF senderAvatarRect, int hostAvatarAlpha, int senderAvatarAlpha, Matrix hostNickNameMatrix, int hostNickNameAlpha, Matrix senderNickNameMatrix, int senderNickNameAlpha, float hostNickNameHeight, float senderNickNameHeight, Map<String, Matrix> matrices) {
        mBitmap = bitmap;
        mHostAvatarOuterMatrix = hostAvatarOuterMatrix;
        mHostAvatarInnerMatrix = hostAvatarInnerMatrix;
        mHostAvatarBorderRect = hostAvatarBorderRect;
        mHostAvatarRect = hostAvatarRect;
        mHostAvatarAlpha = hostAvatarAlpha;
        mSenderAvatarOuterMatrix = senderAvatarOuterMatrix;
        mSenderAvatarInnerMatrix = senderAvatarInnerMatrix;
        mSenderAvatarBorderRect = senderAvatarBorderRect;
        mSenderAvatarRect = senderAvatarRect;
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
        mHostAvatarAlpha = 0;
        mSenderAvatarAlpha = 0;
        mHostAvatarOuterMatrix = null;
        mHostAvatarInnerMatrix = null;
        mHostAvatarRect = null;
        mHostAvatarBorderRect = null;
        mSenderAvatarOuterMatrix = null;
        mSenderAvatarInnerMatrix = null;
        mSenderAvatarRect = null;
        mSenderAvatarBorderRect = null;
        mHostNickNameMatrix = null;
        mHostNickNameAlpha = 0;
        mSenderNickNameMatrix = null;
        mSenderNickNameAlpha = 0;
        mHostNickNameHeight = 0;
        mSenderNickNameHeight = 0;
        mSubAnimationNames = null;
        mSubAnimationSprites.clear();
        mSubAnimationMatrices.clear();
        mSubAnimationPaints.clear();
        mHostAvatarBorderPaint.setColor(0);
        mHostAvatarBorderPaint.setStrokeWidth(0);
        mHostAvatarBorderPaint.clearShadowLayer();
        mSenderAvatarBorderPaint.setColor(0);
        mSenderAvatarBorderPaint.setStrokeWidth(0);
        mSenderAvatarBorderPaint.clearShadowLayer();
        mHostTextPaint.setColor(0);
        mHostStrokeTextPaint.setColor(0);
        mHostStrokeTextPaint.setStrokeWidth(0);
        mHostStrokeTextPaint.clearShadowLayer();
        mSenderTextPaint.setColor(0);
        mSenderStrokeTextPaint.setColor(0);
        mSenderStrokeTextPaint.setStrokeWidth(0);
        mSenderStrokeTextPaint.clearShadowLayer();
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
            int viewWidth = canvas.getWidth();
            int viewHeight = canvas.getHeight();
            if (mFullScreen) {//如果这是一个全屏表情，就把canvas缩放到整个控件的大小
                float scale = scaleToFill(drawableWidth, drawableHeight, viewWidth, viewHeight);
                if (drawableHeight * scale > viewHeight) {//纵向对齐底边
                    canvas.translate(0, viewHeight - drawableHeight * scale);
                } else if (drawableWidth * scale > viewWidth) {//横向居中
                    canvas.translate((viewWidth - drawableWidth * scale) / 2, 0);
                }
                canvas.scale(scale, scale);
            } else {
                canvas.translate((viewWidth - drawableWidth) / 2, (viewHeight - drawableHeight) / 2);
            }
            canvas.drawBitmap(mBitmap, 0, 0, mFramePaint);

            //绘制头像、昵称及子图
            if (mHostAvatarConfig != null) {
                drawAvatar(canvas, mHostAvatarAlpha, mHostAvatarConfig.getBorderWidth(), mHostAvatarConfig.getCornerRadius(), mHostAvatarOuterMatrix, mHostAvatarInnerMatrix, mHostAvatarBorderRect, mHostAvatarRect, mHostAvatarBorderPaint, mHostAvatarPaint);
            }
            if (mSenderAvatarConfig != null) {
                drawAvatar(canvas, mSenderAvatarAlpha, mSenderAvatarConfig.getBorderWidth(), mSenderAvatarConfig.getCornerRadius(), mSenderAvatarOuterMatrix, mSenderAvatarInnerMatrix, mSenderAvatarBorderRect, mSenderAvatarRect, mSenderAvatarBorderPaint, mSenderAvatarPaint);
            }
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
