/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.widget.gallery;

import org.klab.iphoroid.widget.adpterview.OnScrollListener;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.Gallery;
import android.widget.Scroller;


/**
 * ListView と同等な OnScrollListener を設定できる Gallery。 
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/07/09 sano-n initial version <br>
 */
public class ScrollDetectableGallery extends Gallery {

    /** */
    public ScrollDetectableGallery(Context context) {
        super(context);
    }

    /** */
    public ScrollDetectableGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    /** */
    public ScrollDetectableGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** */
    private OnScrollListener onScrollListener;

    /** */
    public void setOnScrollListener(OnScrollListener onScrollListener) {
        this.onScrollListener = onScrollListener;
    }

    /** */
    private int scrollState = OnScrollListener.SCROLL_STATE_IDLE;

    /***
     * Executes the delta scrolls from a fling or scroll movement.
     */
    private FlingRunnable mFlingRunnable = new FlingRunnable();

    /**
     * Responsible for fling behavior. Use {@link #startUsingVelocity(int)} to
     * initiate a fling. Each frame of the fling is handled in {@link #run()}.
     * A FlingRunnable will keep re-posting itself until the fling is done.
     *
     */
    private class FlingRunnable implements Runnable {
        /***
         * Tracks the decay of a fling scroll
         */
        private Scroller mScroller;

        public FlingRunnable() {
            mScroller = new Scroller(getContext());
        }

        private void startCommon() {
            // Remove any pending flings
            removeCallbacks(this);
        }
        
        public void startUsingVelocity(int initialVelocity) {
            if (initialVelocity == 0) {
                return;
            }
            
            startCommon();
            
            int initialX = initialVelocity < 0 ? Integer.MAX_VALUE : 0;
            mScroller.fling(initialX, 0, initialVelocity, 0, 0, Integer.MAX_VALUE, 0, Integer.MAX_VALUE);
            post(this);
        }

        private void endFling(boolean scrollIntoSlots) {
            // Force the scroller's status to finished (without setting its
            // position to the end)
            mScroller.forceFinished(true);

            fireScrollStateChanged(OnScrollListener.SCROLL_STATE_IDLE);
        }

        public void run() {
            if (getChildCount() == 0) {
                endFling(true);
                return;
            }

            final Scroller scroller = mScroller;
            boolean more = scroller.computeScrollOffset();

            if (more) {
                post(this);
            } else {
                endFling(true);
            }
        }
    }    

    /** */
    protected void fireScrollStateChanged(int scrollState) {
        if (this.scrollState != scrollState) {
            onScrollListener.onScrollStateChanged(this, scrollState);
            this.scrollState = scrollState;
        }
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        mFlingRunnable.startUsingVelocity((int) -velocityX);
        fireScrollStateChanged(OnScrollListener.SCROLL_STATE_FLING);
        return super.onFling(e1, e2, velocityX, velocityY);
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        fireScrollStateChanged(OnScrollListener.SCROLL_STATE_TOUCH_SCROLL);
        return super.onScroll(e1, e2, distanceX, distanceY);
    }
}
