package com.tech.playin.widget;

import android.content.Context;
import android.os.Handler;
import android.util.AttributeSet;

/**
 * 倒计时
 */
public class CountdownView extends androidx.appcompat.widget.AppCompatTextView {

    public interface CountdownListener {
        void countfinish();
    }

    private final Handler mHandler = new Handler();

    private CountdownListener listener;
    private int countDown;
    private boolean pause;

    private Runnable runnable = new Runnable() {
        @Override
        public void run() {
            setText(countDown + "");
            countDown--;
            if (countDown >= 0) {
                mHandler.postDelayed(runnable, 1000);
            } else {
                mHandler.removeCallbacksAndMessages(null);
                CountdownView.this.listener.countfinish();
            }
        }
    };

    public CountdownView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void onPause() {
        pause = true;
        mHandler.removeCallbacksAndMessages(null);
    }

    public void onResume() {
        if (pause) {
            mHandler.postDelayed(runnable, 1000);
            pause = false;
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mHandler.removeCallbacksAndMessages(null);
    }

    public void countDown(int count, CountdownListener listener) {
        this.countDown = count;
        this.listener = listener;
        mHandler.post(runnable);
    }
}
