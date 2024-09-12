/**
 * Created by Karim Mreisi.
 */

package com.limelight.binding.input.virtual_controller.keyboard;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Vibrator;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.limelight.Game;
import com.limelight.R;
import com.limelight.binding.input.ControllerHandler;
import com.limelight.ccffee.StrokeTextView;
import com.limelight.nvstream.input.MouseButtonPacket;
import com.limelight.preferences.PreferenceConfiguration;

import java.util.HashMap;
import java.util.Map;

public class KeyBoardLayoutController {

    private final ControllerHandler controllerHandler;
    private final Context context;
    private final Game game;
    private FrameLayout frame_layout = null;
    private Vibrator vibrator;
    private LinearLayout keyboardView;
    private Map<Integer, Boolean> holdKeyMap;
    private Map<Integer, View> holdKeyViewMap;
    private Map<String, View> idViewMap;
    private Map<String, String[]> idValueMap;

    // 小键盘
    private StrokeTextView[] miniKeyboard = new StrokeTextView[21];

    private int layoutTag = 0;


    public KeyBoardLayoutController(final ControllerHandler controllerHandler, FrameLayout layout, final Context context, final Game game) {
        this.controllerHandler = controllerHandler;
        this.frame_layout = layout;
        this.context = context;
        this.game = game;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        this.keyboardView= (LinearLayout) LayoutInflater.from(context).inflate(R.layout.layout_ccffee_keyboard,null);
        this.holdKeyMap = new HashMap<>();
        this.holdKeyViewMap = new HashMap<>();
        this.idValueMap = new HashMap<>();
        this.idViewMap = new HashMap<>();


        initKeyboard();
    }

    private void initKeyboard(){
        View.OnTouchListener touchListener = new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        // 添加震动代码
                        Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);;
                        if (vibrator.hasVibrator()) {
                            vibrator.vibrate(50);  // for 500 ms
                        }

                        // 处理按下事件
                        String tag=(String) v.getTag();
                        if(TextUtils.equals("phone_key",tag)){
                            return true;
                        }
                        if(TextUtils.equals("change1", tag)) {
                            return true;
                        }
                        if(TextUtils.equals("change_color",tag)){
                            return true;
                        }
                        if(TextUtils.equals("change_visible",tag)){
                            return true;
                        }
                        if(TextUtils.equals("invisible",tag)){
                            return true;
                        }
                        if(TextUtils.equals("change_m",tag)){
                            return true;
                        }
                        if(TextUtils.equals("left_mouse",tag)){
                            return true;
                        }
                        if(TextUtils.equals("right_mouse",tag)){
                            return true;
                        }
                        if(TextUtils.equals("test",tag)){
                            return true;
                        }
                        if(TextUtils.equals("mini_keyboard",tag)){
                            return true;
                        }

                        KeyEvent keyEvent = new KeyEvent(KeyEvent.ACTION_DOWN, Integer.parseInt(tag));
                        keyEvent.setSource(0);
                        sendKeyEvent(keyEvent);
                        v.setBackgroundResource(R.drawable.bg_ax_keyboard_button_confirm);

                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        // 处理释放事件
                        String tag2=(String) v.getTag();
                        if(TextUtils.equals("phone_key",tag2)){
                            phoneKey();
                            return true;
                        }
                        if(TextUtils.equals("change1",tag2)){
                            handleKeyChang();
                            return true;
                        }
                        if(TextUtils.equals("change_color",tag2)){
                            handleColorChange();
                            return true;
                        }
                        if(TextUtils.equals("change_visible",tag2)){
                            handleChangeVisible();
                            return true;
                        }
                        if(TextUtils.equals("invisible",tag2)){
                            return true;
                        }
                        if(TextUtils.equals("change_m",tag2)){
                            handleChangeM();
                            return true;
                        }
                        if(TextUtils.equals("left_mouse",tag2)){
                            handleLeftMouse();
                            return true;
                        }
                        if(TextUtils.equals("right_mouse",tag2)){
                            handleRightMouse();
                            return true;
                        }
                        if(TextUtils.equals("test",tag2)){
                            test_cc();
                            return true;
                        }
                        if(TextUtils.equals("mini_keyboard",tag2)){
                            miniKeyboardChange();
                            return true;
                        }
                        int tag2int = Integer.parseInt(tag2);
                        if (handleHoldKey(tag2int)) {
                            return true;
                        }

                        KeyEvent keyUP = new KeyEvent(KeyEvent.ACTION_UP, tag2int);
                        keyUP.setSource(0);
                        sendKeyEvent(keyUP);
                        v.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
                        return true;
                }
                return false;
            }
        };
        for (int i = 0; i < keyboardView.getChildCount(); i++){
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++){
                keyboardRow.getChildAt(j).setOnTouchListener(touchListener);
                StrokeTextView strokeTextView = (StrokeTextView) keyboardRow.getChildAt(j);
                String tag2= (String) strokeTextView.getTag();
                if (isInt(tag2)) {
                    int key = Integer.parseInt(tag2);
                    holdKeyViewMap.put(key, strokeTextView);
                }

                int vid = strokeTextView.getId();
                if (vid != -1) {
                    String fullViewId = context.getResources().getResourceName(vid);
                    String[] parts = fullViewId.split("/");
                    String viewId = parts[parts.length - 1];
                    idViewMap.put(viewId, strokeTextView);
                }

                // 前三行的前七个按钮为小键盘
                if (i < 3 && j < 7) {
                    miniKeyboard[i * 7 + j] = strokeTextView;
                }
            }
        }

        // 初始隐藏
        miniKeyboardChange();
    }

    public boolean isInt(String str) {

        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public boolean handleHoldKey(int key) {
        if (key == 113 || key == 59 || key == 57) {
            // 如果按下的是shift、ctrl或者alt时处理
            if (!this.holdKeyMap.containsKey(key)) {
                this.holdKeyMap.put(key, false);
            }
            boolean holdTag = Boolean.TRUE.equals(this.holdKeyMap.get(key));

            // 状态转换
            this.holdKeyMap.put(key, !holdTag);
            return !holdTag;
        } else {
            // 如果按下的不是上述三个按钮，判断三个按钮的状态
            boolean key1 = Boolean.TRUE.equals(this.holdKeyMap.get(113));
            boolean key2 = Boolean.TRUE.equals(this.holdKeyMap.get(59));
            boolean key3 = Boolean.TRUE.equals(this.holdKeyMap.get(57));

            int keyCode = -1;
            if (key1) {
                // key1被按下，
                keyCode = 113;
            } else if (key2) {
                //key2被按下
                keyCode = 59;
            } else if (key3) {
                //key3 被按下
                keyCode = 57;
            }

            if (keyCode != -1) {
                // 延时释放被按下的key
                Handler handler = new Handler();
                int finalKeyCode = keyCode;
                this.holdKeyMap.put(finalKeyCode, false);
                handler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        // 这里写需要延时执行的代码
                        KeyEvent keyUP = new KeyEvent(KeyEvent.ACTION_UP, finalKeyCode);
                        keyUP.setSource(0);
                        sendKeyEvent(keyUP);

                        // 找到对应的view
                        View v = holdKeyViewMap.get(finalKeyCode);
                        if (v != null) {
                            v.setBackgroundResource(R.drawable.bg_ax_keyboard_button);
                        }
                    }
                }, 50); // 延时50毫秒

            }
        }
        return false;
    }

    public void handleKeyChang() {
        // 切换标志位
        layoutTag = layoutTag > 0 ? 0 : 1;

        // 遍历map
        for(String key : idViewMap.keySet()){
            StrokeTextView view = (StrokeTextView) idViewMap.get(key);

            String[] values = idValueMap.get(key);

            if (values != null && values.length >= 4) {
                // 设置文本
                view.updateText(values[layoutTag * 2]);

                // 设置tag
                view.setTag(values[layoutTag * 2 + 1]);
            }

        }
    }

    private int switchMIndex = 2;
    private void handleChangeM() {
        if (switchMIndex == 2) {
            switchMIndex = -1;
        } else if (switchMIndex == -1) {
            switchMIndex = 2;
        }

        String[] show = {"多","普", "控"};

        game.switchMouseModel(switchMIndex);


        for (int i = 0; i < keyboardView.getChildCount(); i++){
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++){
                StrokeTextView strokeTextView = (StrokeTextView) keyboardRow.getChildAt(j);
                String tag2= (String) strokeTextView.getTag();

                if (tag2.equals("change_m")) {
                    if (switchMIndex == -1) {
                        strokeTextView.setText("屏");
                    } else {
                        strokeTextView.setText(show[switchMIndex]);
                    }
                }

            }
        }

    }

    private void test_cc() {
//        game.conn.sendMousePosition((short)(game.streamView.getWidth() / 2), (short) 0, (short)game.streamView.getWidth(), (short)game.streamView.getHeight());
    }

    private void miniKeyboardChange() {
        for (int i = 0; i < miniKeyboard.length; i++) {
            StrokeTextView view = miniKeyboard[i];
            view.setVisibility(view.getVisibility() == View.VISIBLE ? View.INVISIBLE : View.VISIBLE);
        }
    }

    public Boolean leftMouseHoldState = false;

    public void handleLeftMouse() {
        // 处理鼠标左键按下逻辑
        leftMouseHoldState = !leftMouseHoldState;

        if (leftMouseHoldState) {
            game.conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_LEFT);
        } else {
            game.conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_LEFT);
        }

        for (int i = 0; i < keyboardView.getChildCount(); i++){
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++){
                StrokeTextView strokeTextView = (StrokeTextView) keyboardRow.getChildAt(j);
                String tag2= (String) strokeTextView.getTag();

                if (tag2.equals("left_mouse")) {
                    if (leftMouseHoldState) {
                        strokeTextView.setText("按下");
                    } else {
                        strokeTextView.setText("鼠标\n左键");
                    }
                }

            }
        }
    }

    public void handleRightMouse() {
        game.conn.sendMouseButtonDown(MouseButtonPacket.BUTTON_RIGHT);
        game.conn.sendMouseButtonUp(MouseButtonPacket.BUTTON_RIGHT);
    }

    private int colorIndex = 0;

    public void handleColorChange() {
        colorIndex++;
        if (colorIndex > 3) {
            colorIndex = 0;
        }
        int[] colorList = new int[]{Color.WHITE, Color.BLACK, Color.parseColor("#FF0000"), Color.parseColor("#9BED93")};
        int[] outlineColorList = new int[]{Color.BLACK, Color.WHITE, Color.parseColor("#E8F9FF"), Color.parseColor("#0E332D")};

        for (int i = 0; i < keyboardView.getChildCount(); i++){
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++){
                StrokeTextView strokeTextView = (StrokeTextView) keyboardRow.getChildAt(j);
                strokeTextView.setTextColor(colorList[colorIndex]);
                strokeTextView.setTypeface(null, Typeface.BOLD);
                strokeTextView.setOutlineColor(outlineColorList[colorIndex]);
            }
        }
    }

    private boolean visibleTag = true;

    public void handleChangeVisible() {
        visibleTag = !visibleTag;

        for (int i = 0; i < keyboardView.getChildCount(); i++){
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++){

                StrokeTextView strokeTextView = (StrokeTextView) keyboardRow.getChildAt(j);
                String tag2= (String) strokeTextView.getTag();

                if (!tag2.equals("change_visible") && !tag2.equals("invisible")) {
                    strokeTextView.setVisibility(visibleTag ? View.VISIBLE : View.INVISIBLE);
                }

            }
        }
    }

    public void phoneKey() {
        this.game.toggleKeyboard();
    }

    public void show() {
        keyboardView.setVisibility(View.VISIBLE);
    }

    public void switchShowHide() {
        if (keyboardView.getVisibility() == View.VISIBLE) {
            phoneKey();
        } else {
            show();
        }
    }

    public void refreshLayout() {
        frame_layout.removeView(keyboardView);
//        DisplayMetrics screen = context.getResources().getDisplayMetrics();
//        (int)(screen.heightPixels/0.4)
        int height=PreferenceConfiguration.readPreferences(context).oscKeyboardHeight;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,dip2px(context,height));
        params.gravity= Gravity.BOTTOM;
//        params.leftMargin = 20 + buttonSize;
//        params.topMargin = 15;
        keyboardView.setAlpha(PreferenceConfiguration.readPreferences(context).oscKeyboardOpacity/100f);
        frame_layout.addView(keyboardView,params);

        for (int i = 0; i < keyboardView.getChildCount(); i++){
            LinearLayout keyboardRow = (LinearLayout) keyboardView.getChildAt(i);
            for (int j = 0; j < keyboardRow.getChildCount(); j++){

                StrokeTextView strokeTextView = (StrokeTextView) keyboardRow.getChildAt(j);
                String tag2= (String) strokeTextView.getTag();

                if (tag2.equals("invisible")) {
//                    strokeTextView.setVisibility(visibleTag ? View.VISIBLE : View.INVISIBLE);
                    LinearLayout.LayoutParams params1 = (LinearLayout.LayoutParams) strokeTextView.getLayoutParams();
                    params1.weight = PreferenceConfiguration.readPreferences(context).oscKeyboardHoleWeight;
                    strokeTextView.setLayoutParams(params1);
                }

            }
        }

    }

    public int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public void sendKeyEvent(KeyEvent keyEvent) {
        if (Game.instance == null || !Game.instance.connected) {
            return;
        }
        //1-鼠标 0-按键 2-摇杆 3-十字键
        if (keyEvent.getSource() == 1) {
            Game.instance.mouseButtonEvent(keyEvent.getKeyCode(), KeyEvent.ACTION_DOWN == keyEvent.getAction());
        } else {
            Game.instance.onKey(null, keyEvent.getKeyCode(), keyEvent);
        }
//        if (PreferenceConfiguration.readPreferences(context).enableKeyboardVibrate && vibrator.hasVibrator()) {
//            vibrator.vibrate(10);
//        }
    }
}
