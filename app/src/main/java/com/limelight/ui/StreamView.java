package com.limelight.ui;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceView;
import android.view.ViewTreeObserver;
import android.view.WindowManager;

public class StreamView extends SurfaceView {
    private double desiredAspectRatio;
    private InputCallbacks inputCallbacks;
    private int keyboardHeight = 0; // 添加软键盘高度变量

    // 缩放和移动相关变量
    private float currentScale = 1.0f;
    public float translateX = 0.0f;
    public float translateY = 0.0f;

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
        // 使用ViewTreeObserver在布局完成后获取实际可用的容器尺寸
        ViewTreeObserver vto = getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 获取父容器的尺寸作为可用屏幕尺寸
                android.view.ViewGroup parent = (android.view.ViewGroup) getParent();
                if (parent != null) {
                    screenWidth = parent.getWidth();
                    screenHeight = parent.getHeight();
                } else {
                    // 备用方案：获取根视图尺寸
                    android.view.View rootView = getRootView();
                    if (rootView != null) {
                        screenWidth = rootView.getWidth();
                        screenHeight = rootView.getHeight();
                    } else {
                        // 最后备用方案：使用DisplayMetrics
                        WindowManager windowManager = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
                        DisplayMetrics displayMetrics = new DisplayMetrics();
                        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
                        screenWidth = displayMetrics.widthPixels;
                        screenHeight = displayMetrics.heightPixels;
                    }
                }
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

        invalidate();
        requestLayout();

        applyTransformation();
        
        // 通知监听器缩放变化
        if (scaleChangeListener != null) {
            scaleChangeListener.onScaleChanged(scale);
        }
    }

    /**
     * 获取当前缩放比例
     * @return 当前缩放因子
     */
    public float getScaleFactor() {
        return this.currentScale;
    }

    public interface ScaleChangeListener {
        void onScaleChanged(float newScale);
    }

    private ScaleChangeListener scaleChangeListener;

    public void setScaleChangeListener(ScaleChangeListener listener) {
        this.scaleChangeListener = listener;
    }

    /**
     * 设置平移偏移
     * @param deltaX 水平方向偏移
     * @param deltaY 垂直方向偏移
     */
    public void setTranslationOffset(float deltaX, float deltaY) {
        this.translateX += deltaX;
        this.translateY += deltaY;

        // 限制translateX的值在屏幕范围内
        int width = this.getWidth();
        int height = this.getHeight();
        translateX = Math.max(-width / 2.0f, Math.min(width / 2.0f, translateX));
        translateY = Math.max(-height / 2.0f, Math.min(height / 2.0f, translateY));

        applyTransformation();
    }

    // 添加设置软键盘高度的方法
    public void setKeyboardHeight(int height) {
        this.keyboardHeight = height;
        applyTransformation();
    }

    private void applyTransformation() {

        // 获取当前view的宽高
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        // 可用屏幕高度
        float valuableScreenHeight = screenHeight - keyboardHeight;

        float targetTranslateX = translateX;
        float targetTranslateY = translateY; // 先保持原有的 translateY，不要预先加偏移

        // 限制x方向
        if (viewWidth <= screenWidth) {
            // 当视图宽度小于等于屏幕宽度时，画面应该居中，不允许移动
            targetTranslateX = 0;
        } else
        {
            // 计算可移动的最大范围
            float maxX = (viewWidth - screenWidth) / 2.0f;
            // 限制不要超出屏幕
            targetTranslateX = Math.min(Math.max(targetTranslateX, -maxX), maxX);
        }

        // 限制y方向
        if (viewHeight <= valuableScreenHeight) {
            // 当视图高度小于等于可用屏幕高度时，需要考虑用户的位置设置
            // 获取当前的布局参数来确定用户设置的位置
            android.view.ViewGroup.LayoutParams layoutParams = getLayoutParams();
            if (layoutParams instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams frameParams = (android.widget.FrameLayout.LayoutParams) layoutParams;
                int gravity = frameParams.gravity;
                
                // 检查是否设置为顶部对齐
                if ((gravity & android.view.Gravity.TOP) == android.view.Gravity.TOP) {
                    // 顶部对齐：考虑软键盘高度，但保持在顶部
                    if (keyboardHeight > 0) {
                        // 有软键盘时，稍微向下偏移以避免被遮挡
                        targetTranslateY = Math.min(keyboardHeight / 4.0f, (valuableScreenHeight - viewHeight) / 2.0f);
                    } else {
                        // 无软键盘时，保持顶部对齐
                        targetTranslateY = 0;
                    }
                } else if ((gravity & android.view.Gravity.BOTTOM) == android.view.Gravity.BOTTOM) {
                    // 底部对齐：避免被软键盘遮挡
                    targetTranslateY = -keyboardHeight / 2.0f;
                } else {
                    // 居中或其他对齐方式：在可用区域内居中显示
                    float centerOffsetY = (valuableScreenHeight - viewHeight) / 2.0f;
                    targetTranslateY = centerOffsetY;
                }
            } else {
                // 默认情况：在可用区域内居中显示
                float centerOffsetY = (valuableScreenHeight - viewHeight) / 2.0f;
                targetTranslateY = centerOffsetY;
            }
        } else {
            // 画面大于可用高度时，允许移动但限制范围
            // 保持与小画面情况下相同的基准计算方式
            
            // 获取布局参数，确保与小画面时的逻辑一致
            android.view.ViewGroup.LayoutParams layoutParams = getLayoutParams();
            float baseTranslateY = 0; // 默认基准位置
            
            if (layoutParams instanceof android.widget.FrameLayout.LayoutParams) {
                android.widget.FrameLayout.LayoutParams frameParams = (android.widget.FrameLayout.LayoutParams) layoutParams;
                int gravity = frameParams.gravity;
                
                if ((gravity & android.view.Gravity.TOP) == android.view.Gravity.TOP) {
                    // 顶部对齐：基准位置为顶部
                    baseTranslateY = keyboardHeight > 0 ? Math.min(keyboardHeight / 4.0f, (valuableScreenHeight - viewHeight) / 2.0f) : 0;
                } else if ((gravity & android.view.Gravity.BOTTOM) == android.view.Gravity.BOTTOM) {
                    // 底部对齐：基准位置考虑键盘
                    baseTranslateY = -keyboardHeight / 2.0f;
                } else {
                    // 居中：基准位置为可用区域中央
                    baseTranslateY = (valuableScreenHeight - viewHeight) / 2.0f;
                }
            } else {
                // 默认：居中
                baseTranslateY = (valuableScreenHeight - viewHeight) / 2.0f;
            }
            
            // 计算允许的移动范围
            float maxMovement = (viewHeight - valuableScreenHeight) / 2.0f;
            float maxY = baseTranslateY + maxMovement;
            float minY = baseTranslateY - maxMovement;
            
            // 如果当前 translateY 在合理范围内，保持它；否则设置为基准位置
            if (targetTranslateY < minY || targetTranslateY > maxY) {
                targetTranslateY = baseTranslateY;
            }
            
            // 限制在允许的范围内
            targetTranslateY = Math.min(Math.max(targetTranslateY, minY), maxY);
        }


        // 应用变换
        this.setTranslationX(targetTranslateX);
        // 统一的 Y 轴变换逻辑，避免跳跃
        this.setTranslationY(targetTranslateY);
    }

    public interface InputCallbacks {
        boolean handleKeyUp(KeyEvent event);
        boolean handleKeyDown(KeyEvent event);
    }
}
