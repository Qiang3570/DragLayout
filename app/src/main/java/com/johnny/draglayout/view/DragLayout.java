package com.johnny.draglayout.view;

import android.content.Context;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nineoldandroids.view.ViewHelper;

public class DragLayout extends FrameLayout{

    /**视图拖拽辅助类*/
    private ViewDragHelper mHelper;
    /**控件高度*/
    private int mHeight;
    /**控件宽度*/
    private int mWidth;
    /**水平方向拖拽的范围*/
    private int mRange;
    /**左面板*/
    private ViewGroup mLeftContent;
    /**主面板*/
    private ViewGroup mMainContent;

    public static enum Status{
        Close, Open, Draging
    }
    private Status status = Status.Close;

    public interface OnDragUpdateListener {
        void onOpen();/*打开监听*/
        void onClose();/*关闭监听*/
        void onDraging(float percent);/*拖拽监听*/

    }
    private OnDragUpdateListener onDragUpdateListener;

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public OnDragUpdateListener getOnDragUpdateListener() {
        return onDragUpdateListener;
    }

    public void setOnDragUpdateListener(OnDragUpdateListener onDragUpdateListener) {
        this.onDragUpdateListener = onDragUpdateListener;
    }

    public DragLayout(Context context) {
        this(context, null);
    }

    public DragLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DragLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mHelper = ViewDragHelper.create(this, 0.5f, mCallback);/*forParent 要进行触摸滑动的父控件,sensitivity 敏感度, 越大敏感,创建ViewDragHelper*/

    }

    ViewDragHelper.Callback mCallback = new ViewDragHelper.Callback() {/*重写事件回调*/
        @Override
        public boolean tryCaptureView(View child, int pointerId) {/*返回值决定了, 被按下的child是否可以被拖拽*/
            return true;/*child 手指触摸按下的子View,pointerId 多点触摸的id*/
        }
        @Override
        public int getViewHorizontalDragRange(View child) {/*返回view水平方向的拖拽距离. > 0 . 决定了松手时动画执行时长, 水平方向是否可以滑动*/
            return mRange;
        }
        public int clampViewPositionHorizontal(View child, int left, int dx) {/*返回值决定了child将要移动到的位置, 此时还未发生真正的位置*/
            if(child == mMainContent){
                left = fixLeft(left);/*拖拽的是主面板, 限定拖拽范围*/
            }
            return left;
        }
        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {/*当子View位置发生变化之后被调用, 伴随动画, 更新状态, 触发监听*/
            if(changedView == mLeftContent){/*如果移动的是左面板, 将左面板的变化量转交给主面板, 自己不动*/
                mLeftContent.layout(0, 0, mWidth, mHeight);/*左面板放回去了*/
                int newLeft = mMainContent.getLeft() + dx;/*转交变化量dx给主面板*/
                newLeft = fixLeft(newLeft);/*修正左边值*/
                mMainContent.layout(newLeft, 0, newLeft + mWidth, mHeight);
            }
            dispathDragEvent();
            invalidate();/*手动在移动之后, 引发界面的重绘, 为了兼容低版本*/
        }
        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {/*当View被释放时候, 做结束动画*/
            if(xvel == 0 && mMainContent.getLeft() > mRange * 0.5f){/*考虑打开的情况*/
                open();
            } else if (xvel > 0) {
                open();
            } else {
                close();
            }

        }
        @Override
        public void onViewDragStateChanged(int state) {/*拖拽状态更新的时候调用*/
            super.onViewDragStateChanged(state);
        }

    };

    /**修正范围*/
    private int fixLeft(int left) {
        if(left < 0){ /*限定左边界*/
            return 0;
        }else if (left > mRange) { /*限定右边界*/
            return mRange;
        }
        return left;
    }

    protected void dispathDragEvent() {/*伴随动画, 更新状态, 执行回调*/
        float percent = mMainContent.getLeft() * 1.0f / mRange;
        animViews(percent);
        if(onDragUpdateListener != null){
            onDragUpdateListener.onDraging(percent);
        }
        Status lastStatus = status; /*更新状态*/
        status = updateStatus(percent);
        if(lastStatus != status && onDragUpdateListener != null){/*执行监听回调, 状态变化的时候*/
            if(status == Status.Open){
                onDragUpdateListener.onOpen();
            }else if (status == Status.Close) {
                onDragUpdateListener.onClose();
            }
        }
    }

    private Status updateStatus(float percent) {
        if(percent == 0){
            return Status.Close;
        }else if (percent == 1.0f) {
            return Status.Open;
        }
        return Status.Draging;
    }

    private void animViews(float percent) {
        ViewHelper.setScaleX(mLeftContent, evaluate(percent, 0.5f, 1.0f));
        ViewHelper.setScaleY(mLeftContent, evaluate(percent, 0.5f, 1.0f));
        ViewHelper.setTranslationX(mLeftContent, evaluate(percent, - mWidth / 2.0f, 0));
        ViewHelper.setAlpha(mLeftContent, evaluate(percent, 0.2f, 1.0f));
        ViewHelper.setScaleX(mMainContent, evaluate(percent, 1.0f, 0.8f));
        ViewHelper.setScaleY(mMainContent, evaluate(percent, 1.0f, 0.8f));
        getBackground().setColorFilter((Integer)evaluateColor(percent, Color.BLACK, Color.TRANSPARENT), PorterDuff.Mode.SRC_OVER);
    }

    /**估值器*/
    public Float evaluate(float fraction, Number startValue, Number endValue) {
        float startFloat = startValue.floatValue();
        return startFloat + fraction * (endValue.floatValue() - startFloat);
    }

    /**估算中间颜色*/
    public Object evaluateColor(float fraction, Object startValue, Object endValue) {
        int startInt = (Integer) startValue;
        int startA = (startInt >> 24) & 0xff;
        int startR = (startInt >> 16) & 0xff;
        int startG = (startInt >> 8) & 0xff;
        int startB = startInt & 0xff;
        int endInt = (Integer) endValue;
        int endA = (endInt >> 24) & 0xff;
        int endR = (endInt >> 16) & 0xff;
        int endG = (endInt >> 8) & 0xff;
        int endB = endInt & 0xff;
        return (int)((startA + (int)(fraction * (endA - startA))) << 24) |
                (int)((startR + (int)(fraction * (endR - startR))) << 16) |
                (int)((startG + (int)(fraction * (endG - startG))) << 8) |
                (int)((startB + (int)(fraction * (endB - startB))));
    }

    /**关闭*/
    protected void close() {
        close(true);
    }

    public void close(boolean isSmooth){
        int finalLeft = 0;
        if(isSmooth){
            if(mHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)){/*触发一个平滑动画.Scroller*/
                ViewCompat.postInvalidateOnAnimation(this);/*触发界面重绘*/
            }
        }else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    /**打开*/
    protected void open() {
        open(true);
    }

    public void open(boolean isSmooth){
        int finalLeft = mRange;
        if(isSmooth){
            if(mHelper.smoothSlideViewTo(mMainContent, finalLeft, 0)){/*触发一个平滑动画.Scroller*/
                ViewCompat.postInvalidateOnAnimation(this);/*触发界面重绘*/
            }
        }else {
            mMainContent.layout(finalLeft, 0, finalLeft + mWidth, 0 + mHeight);
        }
    }

    /**维持动画的继续*/
    @Override
    public void computeScroll() { // invalidate -> drawChild -> child.draw -> computeScroll()
        super.computeScroll();
        if(mHelper.continueSettling(true)){
            ViewCompat.postInvalidateOnAnimation(this);/*true表示. 动画还需要继续. 模拟器还没有结束, 还没有移动到指定位置*/
        }

    }

    /**转交拦截判断, 触摸事件*/
    public boolean onInterceptTouchEvent(android.view.MotionEvent ev) {
        return mHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        try {
            mHelper.processTouchEvent(event);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mHeight = getMeasuredHeight();
        mWidth = getMeasuredWidth();
        mRange = (int) (mWidth * 0.6f);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if(getChildCount() < 2){/*健壮性检查,检查子View个数*/
            throw new IllegalStateException("Your viewgroup must contains 2 children at least.布局至少有两个子View");
        }
        if(!((getChildAt(0) instanceof ViewGroup) && (getChildAt(1) instanceof ViewGroup))){/*检查子View类型ViewGroup子类*/
            throw new IllegalArgumentException("Your child must be an instance of ViewGroup. 子View必须是ViewGroup子类");
        }
        mLeftContent = (ViewGroup) getChildAt(0);
        mMainContent = (ViewGroup) getChildAt(1);
    }
}