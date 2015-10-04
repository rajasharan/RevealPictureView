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

package com.rajasharan.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.drawable.BitmapDrawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

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
    private Bitmap mBitmap;
    private Paint mPaint;
    private Rect mDstRect;

    public RevealPictureView(Context context) {
        this(context, null, 0);
    }

    public RevealPictureView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RevealPictureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        Point windowSize = new Point();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        wm.getDefaultDisplay().getSize(windowSize);

        mInitialSize = new Point();
        mFinalSize = new Point();
        mViewSize = new Point();
        mInitialClipPath = new Path();
        mAnimationClipPath = new Path();
        mAnimationState = INITIAL_ANIM_STATE;
        mCircle = new CircleEvaluator.Circle();
        mPaint = new Paint(Paint.FILTER_BITMAP_FLAG);
        mDstRect = new Rect();

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RevealPictureView);
        BitmapDrawable picture = (BitmapDrawable) a.getDrawable(R.styleable.RevealPictureView_picture);
        a.recycle();

        if (picture == null) {
            throw new UnsupportedOperationException("app:picture attr is mandatory, cannot be skipped");
        }
        createScaledBitmap(picture, windowSize);
    }

    private void createScaledBitmap(BitmapDrawable picture, Point size) {
        Bitmap b = picture.getBitmap();
        RectF src = new RectF(0, 0, b.getWidth(), b.getHeight());
        RectF dst = new RectF(0, 0, size.x/2, size.y/2);
        Matrix matrix = new Matrix();
        matrix.setRectToRect(src, dst, Matrix.ScaleToFit.CENTER);
        mBitmap = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), matrix, true);
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
            //Log.d(TAG, String.format("onMeasure: (%s, %s)", w, h));

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
            int initialRadius = mInitialSize.x > mInitialSize.y ? mInitialSize.x / 2 : mInitialSize.y / 2;
            mInitialClipPath.addCircle(mInitialSize.x / 2, mInitialSize.y / 2, initialRadius, Path.Direction.CW);
            canvas.clipPath(mInitialClipPath, Region.Op.INTERSECT);
            drawScaledBitmap(canvas, mInitialSize);
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
            drawScaledBitmap(canvas, mViewSize);
            canvas.restore();
        }

        if (mAnimationState == FINAL_ANIM_STATE) {
            canvas.drawARGB(255, 255, 255, 255);
            drawScaledBitmap(canvas, mViewSize);
        }
    }

    private void drawScaledBitmap(Canvas canvas, Point size) {
        float aspectRatio = (mBitmap.getWidth()*1.0f) / (mBitmap.getHeight()*1.0f);
        if (aspectRatio < 1) {
            int w = (int) (size.y * aspectRatio);
            int h = size.y;
            int disp = (size.x - w)/2;
            mDstRect.set(disp, 0, w + disp, h);
        }
        else {
            int w = size.x;
            int h = (int) (size.x / aspectRatio);
            int disp = (size.y - h) / 2;
            mDstRect.set(0, disp, w, h + disp);
        }
        canvas.drawARGB(70, 128, 128, 128);
        canvas.drawBitmap(mBitmap, null, mDstRect, mPaint);
    }

    private void createAnimators() {
        int initialRadius = mInitialSize.x > mInitialSize.y ? mInitialSize.x / 2 : mInitialSize.y / 2;

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
                    bringToFront();
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
