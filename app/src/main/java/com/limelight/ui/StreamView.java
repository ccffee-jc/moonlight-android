package com.limelight.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;

public class StreamView extends SurfaceView {
    private double desiredAspectRatio;
    private InputCallbacks inputCallbacks;

    // 缩放和移动相关变量
    private float currentScale = 1.0f;
    private float translateX = 0.0f;
    private float translateY = 0.0f;

    // 屏幕尺寸
    private int screenWidth;
    private int screenHeight;

    public void setDesiredAspectRatio(double aspectRatio) {
        this.desiredAspectRatio = aspectRatio;
    }

    public void setInputCallbacks(InputCallbacks callbacks) {
        this.inputCallbacks = callbacks;
    }

    public StreamView(Context context) {
        super(context);
        init(context);
    }

    public StreamView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context);
    }

    private void init(Context context) {
        // 获取屏幕尺寸
        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                screenWidth = getWidth();
                screenHeight = getHeight();
                getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
        });
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 如果没有提供固定的宽高比，使用默认的onMeasure()行为
        if (desiredAspectRatio == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int measuredHeight, measuredWidth;
        if (widthSize > heightSize * desiredAspectRatio) {
            measuredHeight = heightSize;
            measuredWidth = (int)(measuredHeight * desiredAspectRatio);
        } else {
            measuredWidth = widthSize;
            measuredHeight = (int)(measuredWidth / desiredAspectRatio);
        }

        // 应用缩放比例
        measuredWidth = (int)(measuredWidth * currentScale);
        measuredHeight = (int)(measuredHeight * currentScale);

        setMeasuredDimension(measuredWidth, measuredHeight);
    }


    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // 回调允许我们覆盖一些IME行为
        if (inputCallbacks != null) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (inputCallbacks.handleKeyDown(event)) {
                    return true;
                }
            }
            else if (event.getAction() == KeyEvent.ACTION_UP) {
                if (inputCallbacks.handleKeyUp(event)) {
                    return true;
                }
            }
        }

        return super.onKeyPreIme(keyCode, event);
    }

    /**
     * 设置缩放比例
     * @param scale 缩放因子
     */
    public void setScaleFactor(float scale) {
        this.currentScale = scale;
        applyTransformation();
    }

    /**
     * 设置平移偏移
     * @param deltaX 水平方向偏移
     * @param deltaY 垂直方向偏移
     */
    public void setTranslationOffset(float deltaX, float deltaY) {
        this.translateX = deltaX;
        this.translateY = deltaY;
        applyTransformation();
    }

    /**
     * 应用缩放和平移，并限制视图位置
     */
    private void applyTransformation() {
        invalidate();
        requestLayout();
    }

    public interface InputCallbacks {
        boolean handleKeyUp(KeyEvent event);
        boolean handleKeyDown(KeyEvent event);
    }
}
