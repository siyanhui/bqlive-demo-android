package com.siyanhui.mojif.bqliveapp.graphics;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;

import com.siyanhui.mojif.bqlive.BQLive;
import com.siyanhui.mojif.bqliveapp.BQLAnimationView;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 用于播放BQLive定义的PNG序列动画
 * 它可以和BQLAnimationView配合使用，在后台线程中进行图片解码和矩阵计算的工作，并在预定的时间用setFrame()方法将解码和计算结果传给BQLAnimationView。
 * Created by lixiao on 16-9-22.
 */
public class BQLPngSequencePlayer {
    private static final int SHOW_BITMAP = 100, SKIP_FRAME = 101, PLAYBACK_FINISH = 102;
    private static final HandlerThread backgroundThread = new HandlerThread("BQLive_PNG_Sequence_Player");
    private static final ExecutorService decodeExecutor = Executors.newFixedThreadPool(3);
    private int mFrameDuration;
    private int mMaxBufferDepth = 3;
    private int mBufferDepth = 0;
    private WeakReference<BQLAnimationView> mTarget;
    private String[] mColorFiles;
    private String[] mAlphaFiles;
    private int[] mFrameIndices;
    private int mFrameCount;
    private int mCurrentFrame = 0;
    private long mLastFrameShowTime = 0;
    private int mFrameStep = 1;
    private Handler mHandler;

    /**
     * @param colorFiles    颜色通道文件列表，必须和alphaFiles一一对应
     * @param alphaFiles    透明度通道文件列表，必须和colorFiles一一对应
     * @param frameIndices  帧列表。列表中第n项的值为m，意味着动画第n帧的主图是colorFiles和alphaFiles中的第m项
     * @param target        用于展示动画的控件，含有动画参数的BQLAnimationContext也是从这个控件中拿出来的
     * @param frameDuration 每帧时长
     */
    public BQLPngSequencePlayer(String[] colorFiles, String[] alphaFiles, int[] frameIndices, BQLAnimationView target, final int frameDuration) {
        mColorFiles = colorFiles;
        mAlphaFiles = alphaFiles;
        mFrameIndices = frameIndices;
        mFrameCount = colorFiles.length;
        mTarget = new WeakReference<>(target);
        mFrameDuration = frameDuration;
        if (!backgroundThread.isAlive()) {
            backgroundThread.start();
        }
        mHandler = new Handler(backgroundThread.getLooper()) {
            @Override
            public void handleMessage(Message msg) {
                BQLAnimationView imageView = mTarget.get();
                if (imageView == null) return;
                DecodeTask task = (DecodeTask) msg.obj;
                msg.obj = null;
                switch (msg.what) {
                    case SHOW_BITMAP:
                        //解码成功，如果帧间隔被延长了的话（见下方），就略微缩短帧间隔
                        if (mFrameDuration > frameDuration) {
                            mFrameDuration *= 0.97;
                            if (mFrameDuration < frameDuration) {
                                if (mFrameStep > 1) {
                                    mFrameStep -= 1;
                                }
                                mFrameDuration = frameDuration;
                            }
                        }
                        //显示图片
                        imageView.setFrame(task.mBitmap, task.mAlphaBitmap, task.hostAvatarMatrix, task.hostAvatarAlpha, task.senderAvatarMatrix, task.senderAvatarAlpha, task.hostNickNameMatrix, task.hostNickNameAlpha, task.senderNickNameMatrix, task.senderNickNameAlpha, task.hostNickNameHeight, task.senderNickNameHeight, task.subAnimationMatrices);
                        scheduleNewFrames(imageView);
                        break;
                    case SKIP_FRAME:
                        //解码失败，延长帧间隔，直至跳帧（目的是减小解码压力，防止动画的总时长被拖得过长）
                        mFrameDuration *= 1.25;
                        if (mFrameDuration > 1.7 * mFrameStep * frameDuration) {
                            mFrameStep *= 2;
                            mFrameDuration = frameDuration;
                        }
                        scheduleNewFrames(imageView);
                        break;
                    case PLAYBACK_FINISH:
                        imageView.endAnimation();
                        break;
                }
            }
        };
    }

    /**
     * 安排新帧的解码
     *
     * @param view 用于从中获取BQLAnimationContext
     */
    private void scheduleNewFrames(BQLAnimationView view) {
        --mBufferDepth;
        //安排下一帧的解码
        while (mBufferDepth + 1 <= mMaxBufferDepth) {
            if (mLastFrameShowTime == 0) {
                mLastFrameShowTime = System.currentTimeMillis();
            }
            mLastFrameShowTime += mFrameDuration * mFrameStep;
            if (mCurrentFrame >= mFrameCount) break;
            int frameToDecode = mFrameIndices == null ? mCurrentFrame : mFrameIndices[mCurrentFrame];
            decodeExecutor.execute(new DecodeTask(mColorFiles[frameToDecode], mAlphaFiles[frameToDecode], mLastFrameShowTime, mCurrentFrame, view.getAnimationContext(), mHandler));
            mCurrentFrame += mFrameStep;
            ++mBufferDepth;
        }
        if (mCurrentFrame >= mFrameCount) {
            mHandler.sendMessageDelayed(mHandler.obtainMessage(PLAYBACK_FINISH), mLastFrameShowTime - System.currentTimeMillis());
        }
    }

    /**
     * 开始播放
     */
    public void start() {
        mCurrentFrame = 0;
        BQLAnimationView imageView = mTarget.get();
        if (imageView != null && ++mBufferDepth <= mMaxBufferDepth && mCurrentFrame < mFrameCount) {
            int frameToDecode = mFrameIndices == null ? mCurrentFrame : mFrameIndices[mCurrentFrame];
            decodeExecutor.execute(new DecodeTask(mColorFiles[frameToDecode], mAlphaFiles[frameToDecode], 0, 0, imageView.getAnimationContext(), mHandler));
            mCurrentFrame += mFrameStep;
        }
    }

    /**
     * 虽然名字叫做“解码任务”，但它的功能实际上包括解码和计算矩阵两部分
     */
    private static class DecodeTask implements Runnable {
        Matrix hostAvatarMatrix;
        int hostAvatarAlpha;
        Matrix senderAvatarMatrix;
        int senderAvatarAlpha;
        Matrix hostNickNameMatrix;
        int hostNickNameAlpha;
        Matrix senderNickNameMatrix;
        int senderNickNameAlpha;
        float hostNickNameHeight;
        float senderNickNameHeight;
        Map<String, Matrix> subAnimationMatrices;
        /**
         * 以下几个变量用于存储解码和计算结果
         */
        private Bitmap mBitmap;
        private Bitmap mAlphaBitmap;
        /**
         * 以下是解码与计算需要用到的数据
         */
        private String mPath;
        private String mAlphaPath;
        private long mShowTime;
        private int mFrameNumber;
        private Handler mHandler;
        private BQLAnimationContext mContext;

        public DecodeTask(String jpegPath, String alphaPath, long showTime, int frameNumber, BQLAnimationContext config, Handler handler) {
            mPath = jpegPath;
            mAlphaPath = alphaPath;
            mShowTime = showTime;
            mFrameNumber = frameNumber;
            mHandler = handler;
            mContext = config;
        }

        /**
         * 计算矩阵。原内容会首先被缩放到目标尺寸内（保持长宽比，考虑对齐方式），然后进行平移、缩放、旋转变换。
         *
         * @param contentWidth  原内容的宽度
         * @param contentHeight 原内容的高度
         * @param targetWidth   目标尺寸的宽度
         * @param targetHeight  目标尺寸的高度
         * @param translateX    横向平移距离
         * @param translateY    纵向平移距离
         * @param scale         缩放倍率
         * @param rotate        旋转角度
         * @param alignment     把原内容缩放到目标尺寸时的横向对齐方式，共有三个取值：0（左对齐）、1（居中）、2（右对齐）
         * @return 计算好的矩阵
         */
        private static Matrix calculateMatrix(float contentWidth, float contentHeight, float targetWidth, float targetHeight, float translateX, float translateY, float scale, float rotate, int alignment) {
            if (targetWidth == 0 || targetHeight == 0 || scale == 0) {//面积为0,不需要显示，直接返回
                return null;
            }
            Matrix matrix = new Matrix();
            matrix.preTranslate(translateX, translateY);//平移
            float hostAvatarScale = scaleToFit(contentWidth, contentHeight, targetWidth, targetHeight);//计算从原大小到目标大小需要缩放的倍数
            matrix.preScale(hostAvatarScale, hostAvatarScale);//将原内容缩放到目标大小，不改变长宽比
            float translatedCenterX;
            float translatedCenterY;
            switch (alignment) {
                case 0://横向左对齐，纵向居中
                    translatedCenterX = contentWidth / 2f + (targetWidth - contentWidth * hostAvatarScale) / 2;
                    translatedCenterY = contentHeight / 2f;
                    matrix.preTranslate(0, (targetHeight - contentHeight * hostAvatarScale) / 2);
                    break;
                case 1://居中
                    translatedCenterX = contentWidth / 2f;
                    translatedCenterY = contentHeight / 2f;
                    matrix.preTranslate((targetWidth - contentWidth * hostAvatarScale) / 2, (targetHeight - contentHeight * hostAvatarScale) / 2);
                    break;
                default://横向右对齐，纵向居中
                    translatedCenterX = contentWidth / 2f - (targetWidth - contentWidth * hostAvatarScale) / 2;
                    translatedCenterY = contentHeight / 2f;
                    matrix.preTranslate(targetWidth - contentWidth * hostAvatarScale, (targetHeight - contentHeight * hostAvatarScale) / 2);
                    break;
            }
            matrix.preScale(scale, scale, translatedCenterX, translatedCenterY);//缩放
            matrix.preRotate(rotate, translatedCenterX, translatedCenterY);//旋转
            return matrix;
        }

        /**
         * 计算得到如果要将给定尺寸缩放到限定尺寸范围内，且保持长宽比的话，最大的倍率是多少
         *
         * @param width       给定的宽度
         * @param height      给定的高度
         * @param limitWidth  限定宽度
         * @param limitHeight 限定高度
         * @return 倍率
         */
        private static float scaleToFit(float width, float height, float limitWidth, float limitHeight) {
            int fittedHeight = (int) (limitWidth / width * height);
            if (fittedHeight > limitHeight) {
                return limitHeight / height;
            } else {
                return limitWidth / width;
            }
        }

        @Override
        public void run() {
            mBitmap = BitmapFactory.decodeFile(mPath);//解码颜色通道
            mAlphaBitmap = BitmapFactory.decodeFile(mAlphaPath);//解码透明度通道
            if (mBitmap != null && mAlphaBitmap != null) {
                long time = System.currentTimeMillis();
                if (time <= mShowTime || mShowTime == 0) {//看一下时间，如果超时了的话，直接结束本任务
                    if (mContext != null) {//进行矩阵的计算
                        BQLive.FrameConfig hostAvatarAnimationFrame = mContext.getHostAvatarAnimationFrame(mFrameNumber);
                        if (hostAvatarAnimationFrame != null && (hostAvatarAlpha = (int) (hostAvatarAnimationFrame.getAlpha() * 255)) != 0) {
                            hostAvatarMatrix = calculateMatrix(mContext.getHostAvatarWidth(), mContext.getHostAvatarHeight(), hostAvatarAnimationFrame.getWidth(), hostAvatarAnimationFrame.getHeight(), hostAvatarAnimationFrame.getX(), hostAvatarAnimationFrame.getY(), hostAvatarAnimationFrame.getScale(), hostAvatarAnimationFrame.getRotate(), 1);
                        }
                        BQLive.FrameConfig senderAvatarAnimationFrame = mContext.getSenderAvatarAnimationFrame(mFrameNumber);
                        if (senderAvatarAnimationFrame != null && (senderAvatarAlpha = (int) (senderAvatarAnimationFrame.getAlpha() * 255)) != 0) {
                            senderAvatarMatrix = calculateMatrix(mContext.getSenderAvatarWidth(), mContext.getSenderAvatarHeight(), senderAvatarAnimationFrame.getWidth(), senderAvatarAnimationFrame.getHeight(), senderAvatarAnimationFrame.getX(), senderAvatarAnimationFrame.getY(), senderAvatarAnimationFrame.getScale(), senderAvatarAnimationFrame.getRotate(), 1);
                        }
                        RectF rectF = new RectF();
                        BQLive.FrameConfig hostNickNameFrame = mContext.prepareHostNickName(mFrameNumber, rectF);
                        if (hostNickNameFrame != null && (hostNickNameAlpha = (int) (hostNickNameFrame.getAlpha() * 255)) != 0) {
                            hostNickNameHeight = rectF.height();
                            hostNickNameMatrix = calculateMatrix(rectF.width(), hostNickNameHeight, hostNickNameFrame.getWidth(), hostNickNameFrame.getHeight(), hostNickNameFrame.getX(), hostNickNameFrame.getY(), hostNickNameFrame.getScale(), hostNickNameFrame.getRotate(), mContext.getHostNickNameAlignment());
                        }
                        BQLive.FrameConfig senderNickNameFrame = mContext.prepareSenderNickName(mFrameNumber, rectF);
                        if (senderNickNameFrame != null && (senderNickNameAlpha = (int) (senderNickNameFrame.getAlpha() * 255)) != 0) {
                            senderNickNameHeight = rectF.height();
                            senderNickNameMatrix = calculateMatrix(rectF.width(), senderNickNameHeight, senderNickNameFrame.getWidth(), senderNickNameFrame.getHeight(), senderNickNameFrame.getX(), senderNickNameFrame.getY(), senderNickNameFrame.getScale(), senderNickNameFrame.getRotate(), mContext.getSenderNickNameAlignment());
                        }
                        subAnimationMatrices = new HashMap<>(mContext.getSubAnimationNames().size());
                        for (String name : mContext.getSubAnimationNames()) {
                            BQLive.SubAnimationConfig config = mContext.getSubAnimationConfig(name);
                            BQLive.FrameConfig frameConfig = config.getFrames()[mFrameNumber];
                            subAnimationMatrices.put(name, calculateMatrix(mContext.getSubAnimationSpriteWidth(name), mContext.getSubAnimationSpriteHeight(name), frameConfig.getWidth(), frameConfig.getHeight(), frameConfig.getX(), frameConfig.getY(), frameConfig.getScale(), frameConfig.getRotate(), 1));
                        }
                    }
                    time = System.currentTimeMillis();
                    long delay = mShowTime - time;
                    if (delay < 0) {
                        delay = 0;
                    }
                    if (time <= mShowTime || mShowTime == 0 || mFrameNumber < 3) {//再看一下时间
                        mHandler.sendMessageDelayed(mHandler.obtainMessage(SHOW_BITMAP, this), delay);//解码任务成功，显示本帧
                        return;
                    }
                }
            }
            mHandler.sendMessage(mHandler.obtainMessage(SKIP_FRAME, this));//解码失败或超时，跳过本帧
        }
    }
}
