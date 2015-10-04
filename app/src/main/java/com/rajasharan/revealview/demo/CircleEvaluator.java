package com.rajasharan.revealview.demo;

import android.animation.TypeEvaluator;
import android.util.Log;

/**
 * Copied from PointFEvaluator (API 21)
 */
public class CircleEvaluator implements TypeEvaluator<CircleEvaluator.Circle> {
    private static final String TAG = "TypeEvaluator<>";
    private Circle mCircle;

    public CircleEvaluator(Circle reuse) {
        mCircle = reuse;
    }

    @Override
    public Circle evaluate(float fraction, Circle startValue, Circle endValue) {
        int x = (int) (startValue.x + (fraction * (endValue.x - startValue.x)));
        int y = (int) (startValue.y + (fraction * (endValue.y - startValue.y)));
        int r = (int) (startValue.radius + (fraction * (endValue.radius - startValue.radius)));

        //Log.d(TAG, String.format("Circle {%s, %s, %s: %s}", x, y, r, fraction));

        if (mCircle != null) {
            mCircle.set(x, y, r);
            return mCircle;
        } else {
            return new Circle(x, y, r);
        }
    }

    public static class Circle {
        public int x, y, radius;
        public Circle() {}

        public Circle(int x, int y, int radius) {
            set(x, y, radius);
        }

        public void set(int x, int y, int radius) {
            this.x = x;
            this.y = y;
            this.radius = radius;
        }

        @Override
        public String toString() {
            return String.format("{%s, %s: %s}", x, y, radius);
        }
    }
}
