package com.ixyxj.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextPaint;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;

/**
 * created by ixyxj on 2019/6/19 16:03
 */
public class VerificationCodeView extends android.support.v7.widget.AppCompatEditText implements TextWatcher {
    private int textMargin;
    private int textNum;
    private int boxWidth;
    private int cursorColor;
    private int currentPosition;
    private boolean isCursorShowing;
    private int bottomLineHeight;
    private int bottomLineSelectColor;
    private int bottomLineNormalColor;
    private Paint paint = new Paint();
    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            isCursorShowing = !isCursorShowing;
            handler.sendEmptyMessageDelayed(1, 400);
            postInvalidate();
            return true;
        }
    });
    private OnVerificationCodeChangedListener onCodeChangedListener;

    public VerificationCodeView(Context context) {
        this(context, null);
    }

    public VerificationCodeView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public VerificationCodeView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        TypedArray array = getContext().obtainStyledAttributes(attrs, R.styleable.VerificationCodeView);
        textNum = array.getInteger(R.styleable.VerificationCodeView_textNum, 0);
        textMargin = array.getDimensionPixelSize(R.styleable.VerificationCodeView_textMargin, 0);
        bottomLineHeight = array.getDimensionPixelSize(R.styleable.VerificationCodeView_bottomLineHeight, 1);
        bottomLineNormalColor = array.getColor(R.styleable.VerificationCodeView_bottomLineNormalColor, Color.BLACK);
        bottomLineSelectColor = array.getColor(R.styleable.VerificationCodeView_bottomLineSelectColor, Color.BLACK);
        cursorColor = array.getColor(R.styleable.VerificationCodeView_cursorColor, Color.BLACK);
        paint.setColor(cursorColor);
        array.recycle();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            setLayoutDirection(LAYOUT_DIRECTION_LTR);
        }
        setFocusableInTouchMode(true);
        addTextChangedListener(this);
    }

    public void setOnCodeChangedListener(OnVerificationCodeChangedListener onCodeChangedListener) {
        this.onCodeChangedListener = onCodeChangedListener;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthResult;
        int heightResult;
        //最终的宽度
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        if (widthMode == MeasureSpec.EXACTLY) {
            widthResult = widthSize;
        } else {
            widthResult = ((ViewGroup) getParent()).getWidth();
        }
        //每个矩形形的宽度
        boxWidth = (widthResult - (textMargin * (textNum - 1))) / textNum;
        //最终的高度
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        if (heightMode == MeasureSpec.EXACTLY) {
            heightResult = heightSize;
        } else {
            heightResult = boxWidth;
        }
        setMeasuredDimension(widthResult, heightResult);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        currentPosition = getTextLength();
        int width = boxWidth - getPaddingLeft() - getPaddingRight();
        int height = getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
        //绘制底线
        for (int i = 0; i < textNum; i++) {
            canvas.save();
            float lineY = height - bottomLineHeight / 2f;
            int start = width * i + i * textMargin;
            int end = width + start;
            if (i < currentPosition) {
                paint.setColor(bottomLineSelectColor);
                canvas.drawLine(start, lineY, end, lineY, paint);
            } else {
                paint.setColor(bottomLineNormalColor);
                canvas.drawLine(start, lineY, end, lineY, paint);
            }
            canvas.restore();
        }
        //绘制文字
        String text = getText() == null ? "" : getText().toString();
        for (int i = 0; i < text.length(); i++) {
            canvas.save();
            int start = width * i + i * textMargin;
            float x = start + width / 2f;
            TextPaint paint = getPaint();
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setColor(getCurrentTextColor());
            Paint.FontMetrics fontMetrics = paint.getFontMetrics();
            float baseline = (height - fontMetrics.bottom + fontMetrics.top) / 2
                    - fontMetrics.top;
            canvas.drawText(String.valueOf(text.charAt(i)), x, baseline, paint);
            canvas.restore();
        }
        //绘制光标
        if (!isCursorShowing && isCursorVisible() && currentPosition < textNum && hasFocus()) {
            canvas.save();
            int startX = currentPosition * (width + textMargin) + width / 2;
            int startY = height / 4;
            int endY = height - height / 4;
            canvas.drawLine(startX, startY, startX, endY, paint);
            canvas.restore();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            requestFocus();
            setSelection(getTextLength());
            showKeyBoard(getContext());
            return false;
        }
        return super.onTouchEvent(event);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        currentPosition = getTextLength();
        postInvalidate();
    }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) {
        currentPosition = getTextLength();
        postInvalidate();
        if (onCodeChangedListener != null) {
            onCodeChangedListener.onVerCodeChanged(getText(), start, before, count);
        }
    }

    @Override
    public void afterTextChanged(Editable s) {
        currentPosition = getTextLength();
        postInvalidate();
        if (getText().length() == textNum) {
            if (onCodeChangedListener != null) {
                onCodeChangedListener.onInputCompleted(getText());
            }
        } else if (getText().length() > textNum) {
            getText().delete(textNum, getText().length());
        }
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        handler.sendEmptyMessage(1);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        handler.removeMessages(1);
    }

    public int getTextLength() {
        if (getText() == null) return 0;
        return getText().length();
    }

    public void showKeyBoard(Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(this, InputMethodManager.SHOW_FORCED);
    }

    public interface OnVerificationCodeChangedListener {
        void onVerCodeChanged(CharSequence s, int start, int before, int count);

        void onInputCompleted(CharSequence s);
    }
}
