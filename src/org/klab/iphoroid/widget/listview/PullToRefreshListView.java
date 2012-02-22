/*
 * Copyright (C) 2011 Johan Nilsson <http://markupartist.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.klab.iphoroid.widget.listview;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.klab.iphoroid.R;
import org.klab.iphoroid.widget.gallery.ScrollDetectableGallery;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.HeaderViewListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;


/**
 * PullToRefreshListView.
 *
 * {@link #setAdapter(ListAdapter)} した自分で作成した Adapter は
 * 必ず {@link #getWrappedAdapter()} で取得するようにしてください。
 *
 * @author Johan Nilsson
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version Johan Nilsson original
 * @version sano-n {@link ScrollDetectableGallery} を使用するようにした
 * @version jun 調整
 */
public class PullToRefreshListView extends ListView implements OnScrollListener {

    private static final int TAP_TO_REFRESH = 1;
    private static final int PULL_TO_REFRESH = 2;
    private static final int RELEASE_TO_REFRESH = 3;
    private static final int REFRESHING = 4;

    private static final int RELEASE_TO_REFRESH_FT = 5;

    private static final String TAG = "PullToRefreshListView";

    private OnRefreshListener mOnRefreshListener;

    /**
     * Listener that will receive notifications every time the list scrolls.
     */
    private OnScrollListener mOnScrollListener;

    protected LayoutInflater mInflater;

    private LinearLayout mRefreshView;
    private TextView mRefreshViewText;
    private ImageView mRefreshViewImage;
    private ProgressBar mRefreshViewProgress;
    private TextView mRefreshViewLastUpdated;

    private int mCurrentScrollState;
    private int mRefreshState;

    private RotateAnimation mFlipAnimation;
    private RotateAnimation mReverseFlipAnimation;

    private int mRefreshViewHeight;
    private int mRefreshOriginalTopPadding;
    private int mRefreshOriginalBottomPadding;
    private int mLastMotionY;

    private LinearLayout mRefreshViewFT;
    private TextView mRefreshViewTextFT;
    private ProgressBar mRefreshViewProgressFT;
    private int mRefreshFTOriginalTopPadding;
    private int mRefreshFTOriginalBottomPadding;
    private int mRefreshViewFTHeight;

    private boolean isPullHeaderVisible = false;
    private boolean isPullFooterVisible = false;
    @SuppressWarnings("unused")
    private boolean isLastPosition = false;
    private int mTotalItemCount = 0;
    @SuppressWarnings("unused")
    private int mVisibleItemCount = 0;

    public PullToRefreshListView(Context context) {
        super(context);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PullToRefreshListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        // Load all of the animations we need in code rather than through XML
        mFlipAnimation = new RotateAnimation(0, -180, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mFlipAnimation.setInterpolator(new LinearInterpolator());
        mFlipAnimation.setDuration(250);
        mFlipAnimation.setFillAfter(true);
        mReverseFlipAnimation = new RotateAnimation(-180, 0, RotateAnimation.RELATIVE_TO_SELF, 0.5f, RotateAnimation.RELATIVE_TO_SELF, 0.5f);
        mReverseFlipAnimation.setInterpolator(new LinearInterpolator());
        mReverseFlipAnimation.setDuration(250);
        mReverseFlipAnimation.setFillAfter(true);

        mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        // header
        mRefreshView = (LinearLayout) mInflater.inflate(R.layout.pull_to_refresh_header, null);
        mRefreshViewText = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_text);
        mRefreshViewImage = (ImageView) mRefreshView.findViewById(R.id.pull_to_refresh_image);
        mRefreshViewProgress = (ProgressBar) mRefreshView.findViewById(R.id.pull_to_refresh_progress);
        mRefreshViewLastUpdated = (TextView) mRefreshView.findViewById(R.id.pull_to_refresh_updated_at);
        mRefreshViewImage.setMinimumHeight(50);
        mRefreshView.setOnClickListener(new OnClickRefreshListener());
        mRefreshOriginalTopPadding = mRefreshView.getPaddingTop();
        mRefreshOriginalBottomPadding = mRefreshView.getPaddingBottom();
        addHeaderView(mRefreshView);

        // footer
        mRefreshViewFT = (LinearLayout) mInflater.inflate(R.layout.endress_footer, null);
        mRefreshViewTextFT = (TextView) mRefreshViewFT.findViewById(R.id.pull_to_refresh_updated_at_ft);
        mRefreshViewProgressFT = (ProgressBar) mRefreshViewFT.findViewById(R.id.pull_to_refresh_progress_ft);
        mRefreshViewFT.setOnClickListener(new OnClickRefreshListener());
        mRefreshFTOriginalTopPadding = mRefreshViewFT.getPaddingTop();
        mRefreshFTOriginalBottomPadding = mRefreshViewFT.getPaddingBottom();
        addFooterView(mRefreshViewFT);


        mRefreshState = TAP_TO_REFRESH;

        super.setOnScrollListener(this);

        measureView(mRefreshView);
        mRefreshViewHeight = mRefreshView.getMeasuredHeight();
        mRefreshViewFTHeight = mRefreshViewFT.getMeasuredHeight();

        // ヘッダ・フッタを非表示
        headerInvisible();
        footerInvisible();
    }

    @Override
    protected void onAttachedToWindow() {
        setSelection(1);
    }

    @Override
    public void setAdapter(ListAdapter adapter) {
        super.setAdapter(adapter);

        setSelection(1);
    }

    /**
     * Set the listener that will receive notifications every time the list
     * scrolls.
     *
     * @param l The scroll listener.
     */
    @Override
    public void setOnScrollListener(AbsListView.OnScrollListener l) {
        mOnScrollListener = l;
    }

    /**
     * Register a callback to be invoked when this list should be refreshed.
     *
     * @param onRefreshListener The callback to run.
     */
    public void setOnRefreshListener(OnRefreshListener onRefreshListener) {
        mOnRefreshListener = onRefreshListener;
    }

    /**
     * Set a text to represent when the list was last updated.
     *
     * @param lastUpdated Last updated at.
     */
    public void setLastUpdated(CharSequence lastUpdated) {
        // 未使用
//        if (lastUpdated != null) {
//            mRefreshViewLastUpdated.setVisibility(View.VISIBLE);
//            mRefreshViewLastUpdated.setText(lastUpdated);
//        } else {
//            mRefreshViewLastUpdated.setVisibility(View.GONE);
//        }
    }

    /**
     * Smoothly scroll by distance pixels over duration milliseconds.
     *
     * <p>
     * Using reflection internally to call smoothScrollBy for API Level 8
     * otherwise scrollBy is called.
     *
     * @param distance Distance to scroll in pixels.
     * @param duration Duration of the scroll animation in milliseconds.
     */
    @SuppressWarnings("unused")
    private void scrollListBy(int distance, int duration) {
        try {
            Method method = ListView.class.getMethod("smoothScrollBy", Integer.TYPE, Integer.TYPE);
            method.invoke(this, distance + 1, duration);
        } catch (NoSuchMethodException e) {
            // If smoothScrollBy is not available (< 2.2)
            setSelection(1);
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IllegalAccessException e) {
            Log.e("PullToRefreshListView", "unexpected " + e.getMessage(), e);
        } catch (InvocationTargetException e) {
            Log.e("PullToRefreshListView", "unexpected " + e.getMessage(), e);
        }
    }

    /**
     * ListViewに対するタッチアクションを管理します。
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        final int y = (int) event.getY();

        switch (event.getAction()) {

        // 画面から指を離したとき
        case MotionEvent.ACTION_UP:
            if (!isVerticalScrollBarEnabled()) {
                setVerticalScrollBarEnabled(true);
            }

            if (mRefreshState != REFRESHING) {

                if (mRefreshState == RELEASE_TO_REFRESH) {
                    // 上方リスト更新処理を開始
                    mRefreshState = REFRESHING;
                    resetHeaderPadding();
                    onRefresh();
                } else if (mRefreshState == PULL_TO_REFRESH) {
                    // 更新なし
//                    setSelectionFromTop(1, 0);
                    headerInvisible();
                    footerInvisible();
                } else if (mRefreshState == RELEASE_TO_REFRESH_FT) {
                    // 下方リスト更新処理を開始
                    mRefreshState = REFRESHING;
                    prepareForRefreshFT();
                    onRefreshFT();
                } else {
                    // ヘッダフッタ消去
                    headerInvisible();
                    footerInvisible();
                }
            }
            break;

        // 画面に指が触れたとき
        case MotionEvent.ACTION_DOWN:
            isPullHeaderVisible = false;
            isPullFooterVisible = false;
            // 指のy座標を保持する
            mLastMotionY = y;
            break;

        // 画面上で指が移動したとき
        case MotionEvent.ACTION_MOVE:

            // 更新処理していないかどうかをチェック
            // している場合は無視
            if (mRefreshState != REFRESHING) {
                if (getFirstVisiblePosition() == 0) {
                    // TopPaddingを制御してヘッダの高さを伸ばす処理
                    if (!isPullHeaderVisible && (mRefreshView.getBottom() > 20)) {
                        headerVisivle();
                        isPullHeaderVisible = true;
                    }
                    applyHeaderPadding(event);
                } else if (getLastVisiblePosition() >= mTotalItemCount - 2) {
                    // BottomPaddingを制御してフッタの高さを伸ばす処理
                   if (!isPullFooterVisible && (mRefreshViewFT.getTop() <= getHeight())) {
                        footerVisibleWithoutLoading();
                        isPullFooterVisible = true;
                    }
                    applyFooterPadding(event);
                }
            }

//            Log.d("junapp2", "lastpos:" + getLastVisiblePosition());
//            Log.d("junapp2", "mTotalItemCount:" + mTotalItemCount);
//            Log.d("junapp2", "height:" + getHeight());
//            Log.d("junapp2", "top:" + mRefreshView.getBottom());
            break;
        }
        return super.onTouchEvent(event);
    }

    /**
     * ヘッダを伸縮する処理
     *
     * @param ev
     */
    private void applyHeaderPadding(MotionEvent ev) {
        int topPadding = (int) (((ev.getY() - mLastMotionY) - mRefreshViewHeight) * 0.3f);
        mRefreshView.setPadding(mRefreshView.getPaddingLeft(), topPadding, mRefreshView.getPaddingRight(), mRefreshView.getPaddingBottom());
    }

    /**
     * フッタを伸縮する処理
     *
     * @param ev
     */
    private void applyFooterPadding(MotionEvent ev) {

        int bottomPadding = (int) ((-(ev.getY() - mLastMotionY) - mRefreshViewFTHeight) * 0.3f);
//        Log.d("junapp2", "bottomPadding:" + bottomPadding);
//        Log.d("junapp2", "getY:" + ev.getY());
//        Log.d("junapp2", "lastY:" + mLastMotionY);
        mRefreshViewFT.setPadding(mRefreshViewFT.getPaddingLeft(), mRefreshViewFT.getPaddingTop(), mRefreshViewFT.getPaddingRight(), bottomPadding);
    }

    /**
     * ヘッダのPadding値を初期状態に戻します。
     */
    private void resetHeaderPadding() {
        mRefreshView.setPadding(mRefreshView.getPaddingLeft(), mRefreshOriginalTopPadding, mRefreshView.getPaddingRight(), mRefreshOriginalBottomPadding);
    }

    /**
     * フッタのPadding値を初期状態に戻します。
     */
    private void resetFooterPadding() {
        mRefreshViewFT.setPadding(mRefreshViewFT.getPaddingLeft(), mRefreshFTOriginalTopPadding, mRefreshViewFT.getPaddingRight(), mRefreshFTOriginalBottomPadding);
    }

    /**
     * ヘッダを初期状態に戻します。
     */
    private void resetHeader() {
        if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshState = TAP_TO_REFRESH;

            //resetHeaderPadding();

            // Set refresh view text to the pull label
            mRefreshViewText.setText(R.string.pull_to_refresh_tap_label);
            // Replace refresh drawable with arrow drawable
            mRefreshViewImage.setImageResource(R.drawable.ic_pulltorefresh_arrow);
            // Clear the full rotation animation
            mRefreshViewImage.clearAnimation();
            // Hide progress bar and arrow.
            mRefreshViewImage.setVisibility(View.GONE);
            mRefreshViewProgress.setVisibility(View.GONE);

            headerInvisible();
        }
    }

    /**
     * フッタを初期状態に戻します。
     */
    private void resetFooter() {

        if (mRefreshState != TAP_TO_REFRESH) {
            mRefreshState = TAP_TO_REFRESH;
            footerInvisible();
        }
    }

    /**
     * Viewの高さを計算します。
     *
     * @param child
     */
    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0 + 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    /**
     * リストスクロール時に呼び出されます。
     */
    @Override
    public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {

        mTotalItemCount = totalItemCount;
        mVisibleItemCount = visibleItemCount;

        Log.d("junapp","scrollState:" + mCurrentScrollState);

        // When the refresh view is completely visible, change the text to say
        // "Release to refresh..." and flip the arrow drawable.
        if (mCurrentScrollState == SCROLL_STATE_TOUCH_SCROLL && mRefreshState != REFRESHING) {

            // ヘッダステート管理
            // 頭までスクロールしたかどうかの判定
            if (firstVisibleItem == 0) {

                mRefreshViewImage.setVisibility(View.VISIBLE);

                // ヘッダのステートをPullからReleaseに変化させる座標のしきい値
                int th = getHeight()/7 + mRefreshViewHeight;

                if ((mRefreshView.getBottom() < th) && mRefreshState != PULL_TO_REFRESH && mRefreshState != RELEASE_TO_REFRESH) {
                    mRefreshViewText.setText(R.string.pull_to_refresh_pull_label);
                    mRefreshState = PULL_TO_REFRESH;
                }
                else if ((mRefreshView.getBottom() > th) && mRefreshState != RELEASE_TO_REFRESH) {
                    // 更新処理突入
                    mRefreshViewText.setText(R.string.pull_to_refresh_release_label);
                    mRefreshState = RELEASE_TO_REFRESH;
                    prepareForRefresh();
                }
            }
            // フッタステート管理
            // 最後までスクロールしたかどうかの判定
            else if (totalItemCount == firstVisibleItem + visibleItemCount) {
                int th = getHeight() - (mRefreshViewFTHeight + getHeight()/7);

                if ((mRefreshViewFT.getTop() < th) && mRefreshState != RELEASE_TO_REFRESH_FT) {
                    // 更新処理突入
                    footerVisibleWithLoading();
                    mRefreshState = RELEASE_TO_REFRESH_FT;
                }
            }

        } else if (mCurrentScrollState == SCROLL_STATE_FLING && firstVisibleItem == 0 && mRefreshState != REFRESHING) {
            setSelection(1);
        }

        if (mOnScrollListener != null) {
            mOnScrollListener.onScroll(view, firstVisibleItem, visibleItemCount, totalItemCount);
        }
    }

    /**
     * スクロールステートが変化したときに呼び出されます。
     */
    @Override
    public void onScrollStateChanged(AbsListView view, int scrollState) {

        mCurrentScrollState = scrollState;

        if (mOnScrollListener != null) {
            mOnScrollListener.onScrollStateChanged(view, scrollState);
        }
    }

    /**
     * 上方更新処理のための前準備
     */
    public void prepareForRefresh() {
        //resetHeaderPadding();

        mRefreshViewImage.setVisibility(View.GONE);
        // We need this hack, otherwise it will keep the previous drawable.
        mRefreshViewImage.setImageDrawable(null);
        mRefreshViewProgress.setVisibility(View.VISIBLE);

        // Set refresh view text to the refreshing label
        mRefreshViewText.setText(R.string.pull_to_refresh_refreshing_label);
    }

    /**
     * 上方更新処理キック
     */
    public void onRefresh() {

        Log.d(TAG, "onRefresh");

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefresh();
        }
    }

    /**
     * 下方更新処理のための前準備
     */
    public void prepareForRefreshFT() {
        resetFooterPadding();
        invalidateViews();

        mRefreshState = REFRESHING;
    }

    /**
     * 下方更新処理をキック
     */
    public void onRefreshFT() {
        Log.d("junapp2", "onRefreshFT");

        if (mOnRefreshListener != null) {
            mOnRefreshListener.onRefreshFT();
        }
    }

    /**
     * 上方更新終了処理
     * @param lastUpdated Last updated at.
     */
    public void onRefreshComplete(CharSequence lastUpdated) {
        setLastUpdated(lastUpdated);
        onRefreshComplete();
    }

    /**
     * 下方更新終了処理
     * @param lastUpdated
     */
    public void onRefreshCompleteFT(CharSequence lastUpdated) {
        //setLastUpdated(lastUpdated);
        onRefreshCompleteFT();
    }

    public void onRefreshComplete() {
        Log.d(TAG, "onRefreshComplete");

        isLastPosition = false;

        resetHeader();

        if (mRefreshView.getBottom() > 0) {
            invalidateViews();
            setSelection(1);
        }
    }

    public void onRefreshCompleteFT() {
        Log.d(TAG, "onRefreshCompleteFT");

        isLastPosition = false;

        resetFooter();

        invalidateViews();
    }

    /**
     * Invoked when the refresh view is clicked on. This is mainly used when
     * there's only a few items in the list and it's not possible to drag the
     * list.
     */
    private class OnClickRefreshListener implements OnClickListener {
        @Override
        public void onClick(View v) {
            if (mRefreshState != REFRESHING) {
                prepareForRefresh();
                onRefresh();
            }
        }
    }

    /**
     * Interface definition for a callback to be invoked when list should be
     * refreshed.
     */
    public interface OnRefreshListener {
        /**
         * Called when the list should be refreshed.
         * <p>
         * A call to {@link PullToRefreshListView #onRefreshComplete()} is
         * expected to indicate that the refresh has completed.
         */
        public void onRefresh();
        public void onRefreshFT();
    }

    @Override
    public boolean performItemClick(View view, int position, long id) {
        // - 1 means that this list has dummy (for refresh) item at index 0
Log.d("PullToRefreshListView", "count: " + getAdapter().getCount() + ", position: " + position + " -> " + (position - 1) + ", adapter: " + getAdapter());
        return super.performItemClick(view, position - 1, id);
    }

    /** Gets the adapter you set */
    public ListAdapter getWrappedAdapter() {
        // HeaderViewListAdapter { YourAdapter }
Log.d("PullToRefreshListView", "wrapped adapter: " + ((HeaderViewListAdapter) getAdapter()).getWrappedAdapter());
        return ((HeaderViewListAdapter) getAdapter()).getWrappedAdapter();
    }

    /**
     * ヘッダ非表示処理
     */
    private void headerInvisible() {
        mRefreshView.setVisibility(View.GONE);
        mRefreshViewLastUpdated.setVisibility(View.GONE);
        mRefreshViewProgress.setVisibility(View.GONE);
        mRefreshViewImage.setVisibility(View.GONE);
        mRefreshViewText.setVisibility(View.GONE);
        mRefreshView.setPadding(mRefreshView.getPaddingLeft(), 0, mRefreshView.getPaddingRight(), 0);
    }

    /**
     * ヘッダ表示処理
     */
    private void headerVisivle() {
        resetHeaderPadding();
        mRefreshView.setVisibility(View.VISIBLE);
        mRefreshViewText.setVisibility(View.VISIBLE);
        mRefreshViewImage.setVisibility(View.VISIBLE);
        mRefreshViewImage.clearAnimation();
        mRefreshView.setPadding(mRefreshView.getPaddingLeft(), mRefreshOriginalTopPadding, mRefreshView.getPaddingRight(), mRefreshOriginalBottomPadding);
    }

    /**
     * フッタ表示（ローディング表示なし）
     */
    private void footerVisibleWithoutLoading() {
        mRefreshViewFT.setVisibility(View.VISIBLE);
        mRefreshViewTextFT.setVisibility(View.INVISIBLE);
        mRefreshViewProgressFT.setVisibility(View.INVISIBLE);
        mRefreshViewFT.setPadding(mRefreshViewFT.getPaddingLeft(), mRefreshFTOriginalTopPadding, mRefreshViewFT.getPaddingRight(), mRefreshFTOriginalBottomPadding);
    }

    /**
     * フッタ表示（ローディング表示あり）
     */
    private void footerVisibleWithLoading() {
        mRefreshViewTextFT.setVisibility(View.VISIBLE);
        mRefreshViewProgressFT.setVisibility(View.VISIBLE);
    }

    /**
     * フッタ非表示
     */
    private void footerInvisible() {
        mRefreshViewFT.setVisibility(View.GONE);
        mRefreshViewTextFT.setVisibility(View.GONE);
        mRefreshViewProgressFT.setVisibility(View.GONE);
        mRefreshViewFT.setPadding(mRefreshViewFT.getPaddingLeft(), 0, mRefreshViewFT.getPaddingRight(), 0);
    }
}
