package com.limelight.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.KeyEvent;
import android.view.SurfaceView;

import com.limelight.preferences.PreferenceConfiguration;

public class StreamView extends SurfaceView {
    private double desiredAspectRatio;
    private InputCallbacks inputCallbacks;

    public float scale = 1.0f;
    public float translationX = 0f;
    public float translationY = 0f;
    private int screenWidth;
    private int screenHeight;
    private int originalViewHeight;
    private int originalViewWidth;
    public float mousePosX;
    public float mousePosY;

    private StreamViewInteractionListener interactionListener;

    public void setDesiredAspectRatio(double aspectRatio) {
        this.desiredAspectRatio = aspectRatio;
    }

    public void setInputCallbacks(InputCallbacks callbacks) {
        this.inputCallbacks = callbacks;
    }

    public StreamView(Context context) {
        super(context);
    }

    public StreamView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public StreamView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setInteractionListener(StreamViewInteractionListener listener) {
        this.interactionListener = listener;
    }

    private float maxScalePer = 1f;
    private float minScalePer = 1;

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (desiredAspectRatio == 0) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);
            return;
        }

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec  - dip2px(super.getContext(), PreferenceConfiguration.readPreferences(super.getContext()).oscKeyboardHeight));


        int measuredHeight, measuredWidth;
//        if (widthSize > heightSize * desiredAspectRatio) {
            measuredHeight = heightSize;
            measuredWidth = (int)(measuredHeight * desiredAspectRatio);
//        } else {
//            measuredWidth = widthSize;
//            measuredHeight = (int)(measuredWidth / desiredAspectRatio);
//        }

        DisplayMetrics displayMetrics = new DisplayMetrics();
        ((Activity) super.getContext()).getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        screenWidth = displayMetrics.widthPixels;
        screenHeight = displayMetrics.heightPixels;

        // Apply scale
        measuredWidth = (int)(measuredWidth * scale);
        measuredHeight = (int)(measuredHeight * scale);
        setMeasuredDimension(measuredWidth, measuredHeight);

        originalViewWidth = originalViewWidth > 0 ? originalViewWidth : getMeasuredWidth();
        originalViewHeight = originalViewHeight > 0 ? originalViewHeight : getMeasuredHeight();

        maxScalePer = maxScalePer > 1 ? maxScalePer : (float) screenHeight / originalViewHeight; //MeasureSpec.getSize(heightMeasureSpec) * desiredAspectRatio
    }
    @Override
    public boolean onKeyPreIme(int keyCode, KeyEvent event) {
        // This callbacks allows us to override dumb IME behavior like when
        // Samsung's default keyboard consumes Shift+Space.
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

    public void setScale(float scale) {
        if (scale > maxScalePer) {
            scale = maxScalePer;
            return;
        } else if (scale < minScalePer) {
            scale = minScalePer;
            return;
        }
        this.scale = scale;
        requestLayout();
        updatePosition();
        if (interactionListener != null) {
            interactionListener.onScale(scale);
        }

        // 缩放后重新设置鼠标位置
        resetMousePos();
    }

    public void resetMousePos() {
        // 计算当前屏幕中心点距离视图中心点的位置，然后赋值给鼠标
        this.translationX = getTranslationX();
        this.translationY = getTranslationY();

        this.mousePosX = this.translationX;
        this.mousePosY = this.translationY;
        this.setMousePos(0, 0);
    }

    public float getScaleCC() {
        return this.scale;
    }

    public void setTranslation(float dx, float dy) {
        this.translationX += dx;
        this.translationY += dy;
        setTranslationX(translationX);
        setTranslationY(translationY);
        this.updatePosition();
        if (interactionListener != null) {
            interactionListener.onMove(dx, dy);
        }
    }

    public void setMousePos(float dx, float dy) {
        this.mousePosX += dx;
        this.mousePosY += dy;

        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        // 限制鼠标边界
        if (mousePosX > 0) {
            mousePosX = Math.min(mousePosX, (float) viewWidth / 2);
        } else {
            mousePosX = Math.max(mousePosX, -(float) viewWidth / 2);
        }

        if (mousePosY >= 0) {
            mousePosY = Math.min(mousePosY, (float) viewHeight / 2);
        } else {
            mousePosY = Math.max(mousePosY, -(float) viewHeight / 2);
        }
    }

    public Boolean mouseTakeScreenX() {
        int viewWidth = getMeasuredWidth();
        return !((float) viewWidth / 2 - Math.abs(this.mousePosX) < (float) screenWidth / 2);
    }

    public Boolean mouseTakeScreenY() {
        int viewHeight = getMeasuredHeight();
        int keyHeight = dip2px(super.getContext(), PreferenceConfiguration.readPreferences(super.getContext()).oscKeyboardHeight);

        return !((float) viewHeight / 2 - Math.abs(this.mousePosY) < (float) screenHeight / 2 - keyHeight);
    }


    private void updatePosition() {
        int viewWidth = getMeasuredWidth();
        int viewHeight = getMeasuredHeight();

        // 限制移动边界
        if (translationX > 0) {
            translationX = Math.min(translationX, (float) (viewWidth - screenWidth) / 2);
        } else {
            translationX = Math.max(translationX, -(float) (viewWidth - screenWidth) / 2);
        }

        int keyHeight = dip2px(super.getContext(), PreferenceConfiguration.readPreferences(super.getContext()).oscKeyboardHeight);
        if (translationY >= 0) {
            translationY = Math.min(translationY, 0);
        } else {
            translationY = Math.max(translationY, -(float) ((viewHeight + keyHeight) - screenHeight));
        }

        setTranslationX(translationX);
        setTranslationY(translationY);

    }

    public interface InputCallbacks {
        boolean handleKeyUp(KeyEvent event);
        boolean handleKeyDown(KeyEvent event);
    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }
}
