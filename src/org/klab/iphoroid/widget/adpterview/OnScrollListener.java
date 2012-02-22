/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.widget.adpterview;

import android.widget.AdapterView;


/**
 * スクロールが行われた際にコールされるリスナーです。
 * 
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/07/11 sano-n initial version <br>
 */
public interface OnScrollListener {

    /**
     * スクロールが発生していない状態です
     */
    static final int SCROLL_STATE_IDLE = 0;

    /**
     * スクロールが行われている状態です
     */
    static final int SCROLL_STATE_TOUCH_SCROLL = 1;

    /**
     * Flingが行われた状態です
     */
    static final int SCROLL_STATE_FLING = 2;

    /**
     * スクロールが行われた際にコールされます.
     * 
     * @param view
     * @param firstVisibleItem 表示領域内の最初のアイテムのインデックス
     * @param visibleItemCount 表示領域内のアイテム数
     * @param totalItemCount (表示領域、非表示領域関係なく)全てのアイテム数
     */
    void onScroll(AdapterView<?> view, int firstVisibleItem, int visibleItemCount, int totalItemCount);

    /**
     * スクロールの状態が変化した際にコールされます
     * 
     * @param view
     * @param scrollState 新しい状態
     */
    void onScrollStateChanged(AdapterView<?> view, int scrollState);
}
