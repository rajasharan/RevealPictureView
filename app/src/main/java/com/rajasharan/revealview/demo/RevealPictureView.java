/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Raja Sharan Mamidala
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.rajasharan.revealview.demo;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Region;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by rajasharan on 10/4/15.
 */
public class RevealPictureView extends View implements ValueAnimator.AnimatorUpdateListener, Animator.AnimatorListener {
    private static final String TAG = "RevealPictureView-TAG";
    private static final int INITIAL_ANIM_STATE = 0;
    private static final int BACKWORD_ANIM_STATE = 1;
    private static final int FORWARD_ANIM_STATE = 2;
    private static final int FINAL_ANIM_STATE = 3;

    private Point mFinalSize;
    private Point mInitialSize;

    private Path mInitialClipPath;
    private Path mAnimationClipPath;
    private int mAnimationState;
    private ValueAnimator mCircleAnimator;
    private CircleEvaluator.Circle mCircle;
    private ValueAnimator mSizeAnimator;
    private Point mViewSize;

    private Paint mPaint;

    public RevealPictureView(Context context) {
        this(context, null, 0);
    }

    public RevealPictureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealPictureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        mInitialSize = new Point();
        mFinalSize = new Point();
        mViewSize = new Point();
        mInitialClipPath = new Path();
        mAnimationClipPath = new Path();
        mAnimationState = INITIAL_ANIM_STATE;
        mCircle = new CircleEvaluator.Circle();

        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(5f);
        mPaint.setColor(Color.RED);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        if (mAnimationState == INITIAL_ANIM_STATE) {
            int w = MeasureSpec.getSize(widthMeasureSpec);
            int h = MeasureSpec.getSize(heightMeasureSpec);
            widthMeasureSpec = MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY);
            heightMeasureSpec = MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY);
            setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);

            mInitialSize.set(w, h);

            View parent = (View) getParent();
            ViewGroup.MarginLayoutParams margins = (ViewGroup.MarginLayoutParams) getLayoutParams();
            w = parent.getWidth() - parent.getPaddingLeft() - parent.getPaddingRight() - margins.leftMargin - margins.rightMargin;
            h = parent.getHeight() - parent.getPaddingTop() - parent.getPaddingBottom() - margins.topMargin - margins.bottomMargin;
            mFinalSize.set(w, h);
        }
        if (mAnimationState == FORWARD_ANIM_STATE || mAnimationState == BACKWORD_ANIM_STATE || mAnimationState == FINAL_ANIM_STATE) {
            int w = MeasureSpec.makeMeasureSpec(mViewSize.x, MeasureSpec.EXACTLY);
            int h = MeasureSpec.makeMeasureSpec(mViewSize.y, MeasureSpec.EXACTLY);
            setMeasuredDimension(w, h);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mAnimationState == INITIAL_ANIM_STATE) {
            canvas.save();
            mInitialClipPath.rewind();
            int initialRadius = mInitialSize.x < mInitialSize.y ? mInitialSize.x / 2 : mInitialSize.y / 2;
            mInitialClipPath.addCircle(mInitialSize.x / 2, mInitialSize.y / 2, initialRadius, Path.Direction.CW);
            canvas.clipPath(mInitialClipPath, Region.Op.INTERSECT);
            canvas.drawARGB(128, 128, 128, 128);
            canvas.drawCircle(mInitialSize.x/2, mInitialSize.y/2, 2, mPaint);
            canvas.restore();

            if (mCircleAnimator == null) {
                createAnimators();
            }
        }

        if (mAnimationState == FORWARD_ANIM_STATE || mAnimationState == BACKWORD_ANIM_STATE) {
            canvas.save();
            mAnimationClipPath.rewind();
            mAnimationClipPath.addCircle(mCircle.x, mCircle.y, mCircle.radius, Path.Direction.CW);
            canvas.clipPath(mAnimationClipPath, Region.Op.INTERSECT);
            canvas.drawARGB(128, 128, 128, 128);
            canvas.drawCircle(mCircle.x, mCircle.y, 2, mPaint);
            canvas.restore();
        }

        if (mAnimationState == FINAL_ANIM_STATE) {
            canvas.drawARGB(128, 128, 128, 128);
            canvas.drawCircle(mCircle.x, mCircle.y, 2, mPaint);
        }
        canvas.drawRect(0, 0, getWidth(), getHeight(), mPaint);
    }

    private void createAnimators() {
        int initialRadius = mInitialSize.x < mInitialSize.y ? mInitialSize.x / 2 : mInitialSize.y / 2;

        CircleEvaluator.Circle initialCircle = new CircleEvaluator.Circle();
        initialCircle.set(mInitialSize.x / 2, mInitialSize.y / 2, initialRadius);

        CircleEvaluator.Circle finalCircle = new CircleEvaluator.Circle();
        int finalRadius = mFinalSize.x > mFinalSize.y ? mFinalSize.x / 2 : mFinalSize.y / 2;
        finalCircle.set(mFinalSize.x / 2, mFinalSize.y / 2, finalRadius);

        mCircleAnimator = ValueAnimator.ofObject(new CircleEvaluator(mCircle), initialCircle, finalCircle);
        mCircleAnimator.addUpdateListener(this);
        mCircleAnimator.addListener(this);

        Point initialSize = new Point(mInitialSize);
        Point finalSize = new Point(mFinalSize);
        mSizeAnimator = ValueAnimator.ofObject(new PointEvaluator(mViewSize), initialSize, finalSize);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int x = (int) event.getX();
        int y = (int) event.getY();

        switch (event.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                return true;
            case MotionEvent.ACTION_UP:
                if (checkTouchInsideView(getLeft() + x, getTop() + y)) {
                    //Log.d(TAG, String.format("onTouchUp: (%s, %s)", x, y));
                    startAnimation();
                    return true;
                }
                break;
        }
        return false;
    }

    private boolean checkTouchInsideView(int x, int y) {
        Rect r = new Rect();
        getHitRect(r);
        return r.contains(x, y);
    }

    private void startAnimation() {
        if (mAnimationState == INITIAL_ANIM_STATE) {
            mCircleAnimator.start();
            mSizeAnimator.start();
            mAnimationState = FORWARD_ANIM_STATE;
        }
        if (mAnimationState == FINAL_ANIM_STATE) {
            mCircleAnimator.reverse();
            mSizeAnimator.reverse();
            mAnimationState = BACKWORD_ANIM_STATE;
        }
    }

    @Override
    public void onAnimationUpdate(ValueAnimator animation) {
        requestLayout();
        invalidate();
    }

    @Override
    public void onAnimationStart(Animator animation) {
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        if (mAnimationState == FORWARD_ANIM_STATE) {
            mAnimationState = FINAL_ANIM_STATE;
            requestLayout();
            invalidate();
        }
        if (mAnimationState == BACKWORD_ANIM_STATE) {
            mAnimationState = INITIAL_ANIM_STATE;
            requestLayout();
            invalidate();
        }
    }

    @Override
    public void onAnimationCancel(Animator animation) {
    }

    @Override
    public void onAnimationRepeat(Animator animation) {
    }
}
