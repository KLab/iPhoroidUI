/*
 * Copyright (C) 2011 Patrik Åkerfeldt
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.klab.iphoroid.widget.flowview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.util.AttributeSet;
import android.view.View;

import org.klab.iphoroid.R;


/**
 * A FlowIndicator which draws circles (one for each view). The current view
 * position is filled and others are only striked.<br/>
 * <br/>
 * Availables attributes are:<br/>
 * <ul>
 * fillColor: Define the color used to fill a circle (default to white)
 * </ul>
 * <ul>
 * strokeColor: Define the color used to stroke a circle (default to white)
 * </ul>
 * <ul>
 * radius: Define the circle radius (default to 4)
 * </ul>
 */
public class CircleFlowIndicator extends View implements FlowIndicator {

    private int radius = 4;

    private final Paint mPaintStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mPaintFill = new Paint(Paint.ANTI_ALIAS_FLAG);

    private FlowView viewFlow;

    private int currentScroll = 0;

    private int flowWidth = 0;

    /**
     * Default constructor
     * 
     * @param context
     */
    public CircleFlowIndicator(Context context) {
        super(context);
        initColors(0xFFFFFFFF, 0xFFFFFFFF);
    }

    /**
     * The contructor used with an inflater
     * 
     * @param context
     * @param attrs
     */
    public CircleFlowIndicator(Context context, AttributeSet attrs) {
        super(context, attrs);
        // Retrieve styles attributs
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.org_klab_iphoroid_widget_flowview_CircleFlowIndicator);
        // Retrieve the colors to be used for this view and apply them.
        int fillColor = a.getColor(R.styleable.org_klab_iphoroid_widget_flowview_CircleFlowIndicator_fillColor, 0xFFFFFFFF);
        int strokeColor = a.getColor(R.styleable.org_klab_iphoroid_widget_flowview_CircleFlowIndicator_strokeColor, 0xFFFFFFFF);
        // Retrieve the radius
        radius = a.getInt(R.styleable.org_klab_iphoroid_widget_flowview_CircleFlowIndicator_radius, 4);
        initColors(fillColor, strokeColor);
    }

    private void initColors(int fillColor, int strokeColor) {
        mPaintStroke.setStyle(Style.STROKE);
        mPaintStroke.setColor(strokeColor);
        mPaintFill.setStyle(Style.FILL);
        mPaintFill.setColor(fillColor);
    }

    /*
     * (non-Javadoc)
     * 
     * @see android.view.View#onDraw(android.graphics.Canvas)
     */
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int count = 3;
        if (viewFlow != null) {
            count = viewFlow.getViewsCount();
        }
        // Draw stroked circles
        for (int iLoop = 0; iLoop < count; iLoop++) {
            canvas.drawCircle(getPaddingLeft() + radius + (iLoop * (2 * radius + radius)), getPaddingTop() + radius, radius, mPaintStroke);
        }
        int cx = 0;
        if (flowWidth != 0) {
            // Draw the filled circle according to the current scroll
            cx = (currentScroll * (2 * radius + radius)) / flowWidth;
        }
        // The flow width has been upadated yet. Draw the default position
        canvas.drawCircle(getPaddingLeft() + radius + cx, getPaddingTop() + radius, radius, mPaintFill);

    }

    /*
     * @see org.taptwo.android.widget.ViewFlow.ViewSwitchListener#onSwitched(android.view.View, int)
     */
    @Override
    public void onSwitched(View view, int position) {
    }

    /*
     * @see org.taptwo.android.widget.FlowIndicator#setViewFlow(org.taptwo.android.widget.ViewFlow)
     */
    @Override
    public void setViewFlow(FlowView view) {
        viewFlow = view;
        flowWidth = viewFlow.getWidth();
        invalidate();
    }

    /*
     * @see org.taptwo.android.widget.FlowIndicator#onScrolled(int, int, int, int)
     */
    @Override
    public void onScrolled(int h, int v, int oldh, int oldv) {
        currentScroll = h;
        flowWidth = viewFlow.getWidth();
        invalidate();
    }

    /*
     * @see android.view.View#onMeasure(int, int)
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(measureWidth(widthMeasureSpec), measureHeight(heightMeasureSpec));
    }

    /**
     * Determines the width of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The width of the view, honoring constraints from measureSpec
     */
    private int measureWidth(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        // We were told how big to be
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        }
        // Calculate the width according the views count
        else {
            int count = 3;
            if (viewFlow != null) {
                count = viewFlow.getViewsCount();
            }
            result = getPaddingLeft() + getPaddingRight() + (count * 2 * radius) + (count - 1) * radius + 1;
            // Respect AT_MOST value if that was what is called for by
            // measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Determines the height of this view
     * 
     * @param measureSpec A measureSpec packed into an int
     * @return The height of the view, honoring constraints from measureSpec
     */
    private int measureHeight(int measureSpec) {
        int result = 0;
        int specMode = MeasureSpec.getMode(measureSpec);
        int specSize = MeasureSpec.getSize(measureSpec);

        // We were told how big to be
        if (specMode == MeasureSpec.EXACTLY) {
            result = specSize;
        }
        // Measure the height
        else {
            result = 2 * radius + getPaddingTop() + getPaddingBottom() + 1;
            // Respect AT_MOST value if that was what is called for by
            // measureSpec
            if (specMode == MeasureSpec.AT_MOST) {
                result = Math.min(result, specSize);
            }
        }
        return result;
    }

    /**
     * Sets the fill color
     * 
     * @param color ARGB value for the text
     */
    public void setFillColor(int color) {
        mPaintFill.setColor(color);
        invalidate();
    }

    /**
     * Sets the stroke color
     * 
     * @param color ARGB value for the text
     */
    public void setStrokeColor(int color) {
        mPaintStroke.setColor(color);
        invalidate();
    }
}
