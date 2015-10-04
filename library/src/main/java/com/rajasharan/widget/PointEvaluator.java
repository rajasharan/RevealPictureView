package com.rajasharan.widget;

import android.animation.TypeEvaluator;
import android.graphics.Point;
import android.util.Log;

/**
 * Copied from PointFEvaluator (API 21)
 */
public class PointEvaluator implements TypeEvaluator<Point> {
    private static final String TAG = "TypeEvaluator<>";
    private Point mPoint;

    public PointEvaluator(Point reuse) {
        mPoint = reuse;
    }

    @Override
    public Point evaluate(float fraction, Point startValue, Point endValue) {
        int x = (int) (startValue.x + (fraction * (endValue.x - startValue.x)));
        int y = (int) (startValue.y + (fraction * (endValue.y - startValue.y)));

        //Log.d(TAG, String.format("Point {%s, %s: %s}", startValue, endValue, fraction));

        if (mPoint != null) {
            mPoint.set(x, y);
            return mPoint;
        } else {
            return new Point(x, y);
        }
    }
}

