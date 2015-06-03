package cn.dream.cropperimageview.cropper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import cn.dream.cropperimageview.R;

/**
 * Created by Jasen Huang on 2015/5/29.
 */
public class ImageCropperView extends ImageView implements View.OnTouchListener {

    private static final String TAG = "ImageCropperView";

    public enum ScaleDirection {
        LEFT, LEFT_TOP, TOP, RIGHT_TOP, RIGHT, RIGHT_BOTTOM, BOTTOM, LEFT_BOTTOM
    }

    Paint mPaint;

    /**
     * 默认的最大尺寸:2M
     */
    private static final long DEFAULT_MAX_SIZE = 2 * 1024 * 1024 * 8;

    private static final float STROKE_WIDTH = 3f;
    private static final float CIRCLE_RADIUS = 12f;

    private static final float SPACE = 30f;

    private static final int INIT_WIDTH = 400;
    private static final int INIT_HEIGHT = 400;

    private ScaleDirection mScaleDirection;

    private int mRectColor;
    private int mShadowColor;

    private float mStrokeWidth;
    private float mRadius;

    /**
     * 矩形框边与边之间的最小距离
     */
    private float mPadding;

    private long mMaxSize;

    /**
     * view的宽高
     */
    private float mWidth;
    private float mHeight;

    /**
     * 矩形框相对于view的位置
     */
    private float mRectLeft;
    private float mRectTop;

    private float mRectWidth;
    private float mRectHeight;


    private Bitmap mBitmap;

    private float mBmpWidth;
    private float mBmpHeight;

    /**
     * 图片相对于view的位置
     */
    private float mBmpLeft;
    private float mBmpTop;

    /**
     * 图片的缩放比例
     */
    private float mRatio;

    public ImageCropperView(Context context) {
        this(context, null, 0);
    }

    public ImageCropperView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageCropperView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.ImageCropperView, defStyleAttr, 0);

        mRectColor = a.getColor(R.styleable.ImageCropperView_rectColor, getResources().getColor(R.color.default_rect_color));
        mShadowColor = a.getColor(R.styleable.ImageCropperView_shadowColor, getResources().getColor(R.color.default_shadow_color));
        mStrokeWidth = a.getDimension(R.styleable.ImageCropperView_strokeWidth, STROKE_WIDTH);
        mRadius = a.getDimension(R.styleable.ImageCropperView_radius, CIRCLE_RADIUS);
        mRectWidth = a.getDimension(R.styleable.ImageCropperView_initWidth, INIT_WIDTH);
        mRectHeight = a.getDimension(R.styleable.ImageCropperView_initHeight, INIT_HEIGHT);

        a.recycle();

        init();
    }

    private void init() {
        setWillNotDraw(false);
        setOnTouchListener(this);

        setMaxSize(DEFAULT_MAX_SIZE);

        mPaint = new Paint();

        mPadding = mRadius > mStrokeWidth ? mRadius : mStrokeWidth;
    }

    public void setMaxSize(long size) {
        this.mMaxSize = size;
    }

    public long getMaxSize() {
        return this.mMaxSize;
    }

    public void setBitmap(Bitmap bitmap) {
        setImageBitmap(bitmap);
        this.mBitmap = bitmap;

        // 对图片进行压缩（如果超过最大尺寸）
        compressBitmap();

        // // 获取图片的大小、在view中的位置
        getBitmapPosition();
    }

    public Bitmap getBitmap() {
        if (mBitmap == null) {
            mBitmap = getBitmapFromDrawable();
        }

        return mBitmap;
    }

    private Bitmap getBitmapFromDrawable() {
        Drawable src = getDrawable();

        if (src == null) return null;

        int width = src.getIntrinsicWidth();
        int height = src.getIntrinsicHeight();

        if (src instanceof BitmapDrawable) {
            mBitmap = ((BitmapDrawable) src).getBitmap();

        } else {

            mBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mBitmap);
            src.setBounds(0, 0, width, height);
            src.draw(canvas);
        }

        // 对图片进行压缩（如果超过最大尺寸）
        compressBitmap();

        // 获取图片的大小、在view中的位置
        getBitmapPosition();

        return mBitmap;
    }

    private void compressBitmap() {
        long size = mBitmap.getByteCount() * 8;

        mBmpWidth = mBitmap.getWidth();
        mBmpHeight = mBitmap.getHeight();

        if (size > mMaxSize) {
            double ratio = mBmpWidth / mBmpHeight;
            float res = (float) Math.sqrt((double) mMaxSize / (getBytesPerPixel(mBitmap.getConfig()) * 8 *ratio));

            int width = (int) (res * ratio);
            int height = (int) res;

            Bitmap tmp = Bitmap.createScaledBitmap(mBitmap, width, height, true);

            mBitmap = tmp.copy(mBitmap.getConfig(), false);

            tmp.recycle();
            tmp = null;
        }
    }

    /**
     * 根据图像的配置，获取没像素所占大小
     *
     * @param config 图像配置
     * @return bytes per pixel
     */
    public static int getBytesPerPixel(Bitmap.Config config) {
        if (config == Bitmap.Config.ARGB_8888) {
            return 4;
        } else if (config == Bitmap.Config.RGB_565) {
            return 2;
        } else if (config == Bitmap.Config.ARGB_4444) {
            return 2;
        } else if (config == Bitmap.Config.ALPHA_8) {
            return 1;
        }
        return 1;
    }

    /**
     * 计算图片相对于view的位置
     */
    private void getBitmapPosition() {
        mBmpWidth = mBitmap.getWidth();
        mBmpHeight = mBitmap.getHeight();

        if (mBmpWidth <= mWidth && mBmpHeight <= mHeight) {
            mRatio = 1;
            mBmpLeft = (mWidth - mBmpWidth) / 2;
            mBmpTop = (mHeight - mBmpHeight) / 2;
            return;
        }

        if (mBmpWidth > mWidth && mBmpHeight > mHeight) {
            float ratioX = mWidth / mBmpWidth;
            float ratioY = mHeight / mBmpHeight;

            if (ratioX < ratioY) {
                mRatio = ratioX;
                mBmpLeft = 0;
                mBmpTop = (mHeight - mBmpHeight * ratioX) / 2;

            } else {
                mRatio = ratioY;
                mBmpTop = 0;
                mBmpLeft = (mWidth - mBmpWidth * ratioY) / 2;
            }

            return;
        }

        if (mBmpWidth > mWidth && mBmpHeight <= mHeight) {
            mRatio = mWidth / mBmpWidth;
            mBmpLeft = 0;
            mBmpTop = (mHeight - mBmpHeight * mRatio) / 2;

            return;
        }

        if (mBmpHeight > mHeight && mBmpWidth <= mWidth) {
            mRatio = mHeight / mBmpHeight;
            mBmpLeft = (mWidth - mBmpWidth * mRatio) / 2;
            mBmpTop = 0;
        }
    }

    public Bitmap getCroppedBitmap() {
        getBitmap();

        if (mBitmap == null) {
            Log.d(TAG, "#cropper# bitmap is null");
            return  null;
        }

        if (mRatio == 0) {
            Log.d(TAG, "#cropper# width or height is 0");
            return null;
        }

        /**
         * 矩形框在图片外面
         */
        if (mRectLeft > mBmpLeft + mBmpWidth * mRatio
                || mRectLeft + mRectWidth * mRatio <= mBmpLeft
                || mRectTop > mBmpTop + mBmpHeight * mRatio
                || mRectTop + mRectHeight * mRatio <= mBmpTop) {
            Toast.makeText(getContext(), "未选中图片哦~可拖动矩形进行选择~", Toast.LENGTH_SHORT).show();
            return null;
        }

        float left = mRectLeft < mBmpLeft ? 0 : (mRectLeft - mBmpLeft) / mRatio;
        float top = mRectTop < mBmpTop ? 0 : (mRectTop - mBmpTop) / mRatio;
        float right = mRectLeft + mRectWidth > mBmpLeft + mBmpWidth * mRatio ? mBmpWidth : (mRectLeft + mRectWidth - mBmpLeft) / mRatio;
        float bottom = mRectTop + mRectHeight > mBmpTop + mBmpHeight * mRatio ? mBmpHeight : (mRectTop + mRectHeight - mBmpTop) / mRatio;

        int width = (int) (right - left);
        int height = (int) (bottom - top);

        Bitmap dstBmp = Bitmap.createBitmap(mBitmap, (int) left, (int) top, width, height);

        return dstBmp;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType == ScaleType.CENTER_INSIDE) {
            super.setScaleType(scaleType);
        } else {
            Log.w(TAG, "unsupported scale type (only CENTER_INSIDE allowed)");
        }
    }

    float lastX;
    float lastY;

    boolean mIsRectMoving;
    boolean mIsRectScaling;

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mIsRectMoving = false;
                mIsRectScaling = false;

                lastX = event.getX();
                lastY = event.getY();

                mIsRectMoving = isInRect(lastX, lastY);
                mIsRectScaling = isOnSide(lastX, lastY);

                return true;

            case MotionEvent.ACTION_MOVE:
                if (mIsRectMoving) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;

                    moveRect(dx, dy);

                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                }

                if (mIsRectScaling) {
                    float dx = event.getX() - lastX;
                    float dy = event.getY() - lastY;

                    scaleRect(dx, dy);

                    lastX = event.getX();
                    lastY = event.getY();
                    return true;
                }

                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
                mIsRectMoving = false;
                mIsRectScaling = false;
                break;

            case MotionEvent.ACTION_CANCEL:
                mIsRectMoving = false;
                mIsRectScaling = false;
                break;

            default:
                mIsRectMoving = false;
                mIsRectScaling = false;
                break;
        }

        return false;
    }

    /**
     * 移动矩形边框
     *
     * @param dx x方向上的位移
     * @param dy y方向上的位移
     */
    private void moveRect(float dx, float dy) {
        mRectLeft += dx;
        mRectTop += dy;

        if (mRectLeft < 0) {
            mRectLeft = 0;
        }

        if (mRectLeft + mRectWidth >= mWidth) {
            mRectLeft = mWidth - mRectWidth;
        }

        if (mRectTop < 0) {
            mRectTop = 0;
        }

        if (mRectTop + mRectHeight >= mHeight) {
            mRectTop = mHeight - mRectHeight;
        }

        invalidate();
    }

    /**
     * 对矩形边框进行缩放
     *
     * @param dx x方向上的位移
     * @param dy y方向上的位移
     */
    private void scaleRect(float dx, float dy) {
        float rectRight = mRectLeft + mRectWidth;
        float rectBottom = mRectTop + mRectHeight;

        switch (mScaleDirection) {
            case LEFT:
                calculateLeft(dx, rectRight);
                break;

            case LEFT_TOP:
                calculateLeft(dx, rectRight);
                calculateTop(dy, rectBottom);
                break;

            case TOP:
                calculateTop(dy, rectBottom);
                break;

            case RIGHT_TOP:
                calculateTop(dy, rectBottom);
                calculateRight(dx);
                break;

            case RIGHT:
                calculateRight(dx);
                break;

            case RIGHT_BOTTOM:
                calculateRight(dx);
                calculateBottom(dy);
                break;

            case BOTTOM:
                calculateBottom(dy);
                break;

            case LEFT_BOTTOM:
                calculateBottom(dy);
                calculateLeft(dx, rectRight);
                break;
        }

        invalidate();
    }

    private void calculateLeft(float dx, float rectRight) {
        mRectLeft += dx;

        if (mRectLeft < 0) {
            mRectLeft = 0;
        }

        if (mRectLeft > rectRight - mPadding) {
            mRectLeft = rectRight - mPadding;
        }

        mRectWidth = rectRight - mRectLeft;
    }

    private void calculateTop(float dy, float rectBottom) {
        mRectTop += dy;

        if (mRectTop < 0) {
            mRectTop = 0;
        }

        if (mRectTop > rectBottom - mPadding) {
            mRectTop = rectBottom - mPadding;
        }

        mRectHeight = rectBottom - mRectTop;
    }

    private void calculateRight(float dx) {
        mRectWidth += dx;

        if (mRectWidth < mPadding) {
            mRectWidth = mPadding;

        }

        if (mRectLeft + mRectWidth > mWidth) {
            mRectWidth = mWidth - mRectLeft;
        }
    }

    private void calculateBottom(float dy) {
        mRectHeight += dy;

        if (mRectHeight < mPadding) {
            mRectHeight = mPadding;
        }

        if (mRectTop + mRectHeight > mHeight) {
            mRectHeight = mHeight - mRectTop;
        }
    }

    /**
     * 判断是否在正方形区域内
     *
     * @param x 相对于view的x方向上的值
     * @param y 相对于view的y方向上的值
     * @return 是否在区域内
     */
    private boolean isInRect(float x, float y) {
        return x >= 0
                && x >= mRectLeft + SPACE
                && x <= mWidth
                && x <= mRectLeft + mRectWidth - SPACE
                && y >= 0
                && y >= mRectTop + SPACE
                && y <= mHeight
                && y <= mRectTop + mRectHeight - SPACE;
    }

    /**
     * 判断触摸点是否在边边上以及在哪一边
     * (LEFT, LEFT_TOP, TOP, RIGHT_TOP, RIGHT, RIGHT_BOTTOM, BOTTOM, LEFT_BOTTOM)
     *
     * @param x 相对于view的x方向上的值
     * @param y 相对于view的y方向上的值
     * @return 是否在区域内
     */
    private boolean isOnSide(float x, float y) {
        if (x < 0 || x > mWidth || y < 0 || y > mHeight) {
            return false;
        }

        float rectRight = mRectLeft + mRectWidth;
        float rectBottom = mRectTop + mRectHeight;

        if (x >= mRectLeft - SPACE && x <= mRectLeft + SPACE
                && y >= mRectTop - SPACE && y <= rectBottom + SPACE) {
            if (y <= mRectTop + SPACE) {
                mScaleDirection = ScaleDirection.LEFT_TOP;

            } else if (y < rectBottom - SPACE) {
                mScaleDirection = ScaleDirection.LEFT;

            } else {
                mScaleDirection = ScaleDirection.LEFT_BOTTOM;
            }
            return true;
        }

        if (x >= rectRight - SPACE && x <= rectRight + SPACE
                && y >= mRectTop - SPACE && y <= rectBottom + SPACE) {
            if (y <= mRectTop + SPACE) {
                mScaleDirection = ScaleDirection.RIGHT_TOP;

            } else if (y < rectBottom - SPACE) {
                mScaleDirection = ScaleDirection.RIGHT;

            } else {
                mScaleDirection = ScaleDirection.RIGHT_BOTTOM;
            }
            return true;
        }

        if (x > mRectLeft + SPACE && x < rectRight - SPACE) {
            if (y >= mRectTop - SPACE && y <= mRectTop + SPACE) {
                mScaleDirection = ScaleDirection.TOP;
                return true;
            }

            if (y >= rectBottom - SPACE && y <= rectBottom + SPACE) {
                mScaleDirection = ScaleDirection.BOTTOM;
                return true;
            }
        }

        return false;
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);

        if (changed) {

            mWidth = right - left;
            mHeight = bottom - top;

            if (mRectWidth >= mWidth) {
                mRectWidth = mRectHeight = mWidth / 2;
            }

            if (mRectHeight >= mHeight) {
                mRectWidth = mRectHeight = mHeight / 2;
            }

            mRectLeft = (mWidth - mRectWidth) / 2;
            mRectTop = (mHeight - mRectHeight) / 2;
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        drawRect(canvas);
        drawShadow(canvas);
        drawCircles(canvas);
    }

    /**
     * 绘制方形框
     *
     * @param canvas 画布
     */
    private void drawRect(Canvas canvas) {
        mPaint.setColor(mRectColor);
        mPaint.setStrokeWidth(mStrokeWidth);
        mPaint.setStyle(Paint.Style.STROKE);

        canvas.drawRect(mRectLeft,
                mRectTop,
                mRectLeft + mRectWidth,
                mRectTop + mRectHeight,
                mPaint);
    }

    /**
     * 绘制位于矩形边框四个顶点的圆圈
     *
     * @param canvas
     */
    private void drawCircles(Canvas canvas) {
        float rectRight = mRectLeft + mRectWidth;
        float rectBottom = mRectTop + mRectHeight;

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mRectColor);
        canvas.drawCircle(mRectLeft, mRectTop, mRadius, mPaint);
        canvas.drawCircle(rectRight, mRectTop, mRadius, mPaint);
        canvas.drawCircle(rectRight, rectBottom, mRadius, mPaint);
        canvas.drawCircle(mRectLeft, rectBottom, mRadius, mPaint);
    }

    /**
     * 绘制方形框外的阴影 —— 分成四个方形区域绘制，如下图所示
     *
     * @param canvas 画布
     */
    private void drawShadow(Canvas canvas) {
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setColor(mShadowColor);
        canvas.drawRect(0f, 0f, mRectLeft - mStrokeWidth / 2, mRectTop + mRectHeight + mStrokeWidth / 2, mPaint);
        canvas.drawRect(mRectLeft - mStrokeWidth / 2, 0f, mWidth, mRectTop - mStrokeWidth / 2, mPaint);
        canvas.drawRect(mRectLeft + mRectWidth + mStrokeWidth / 2, mRectTop - mStrokeWidth / 2, mWidth, mHeight, mPaint);
        canvas.drawRect(0f, mRectTop + mRectHeight + mStrokeWidth / 2, mRectLeft + mRectWidth + mStrokeWidth / 2, mHeight, mPaint);
    }
}
