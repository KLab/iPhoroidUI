/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.widget.listview;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.widget.ArrayAdapter;

import org.klab.iphoroid.widget.listview.PullToRefreshEndlessListView.DefaultRefreshListener;
import org.klab.iphoroid.widget.listview.PullToRefreshEndlessListView.RefreshListener;
import org.klab.iphoroid.widget.listview.PullToRefreshEndlessListView.Refreshable;


/**
 * {@link PullToRefreshEndlessListView} に適用する {@link android.widget.Adapter} です。 
 *
 * <li>やりすぎ、でも使う側は超簡単！</li>
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/07/10 sano-n initial version <br>
 */
public abstract class RefreshableArrayAdapter<T> extends ArrayAdapter<T> implements Refreshable<List<T>> {

    public RefreshableArrayAdapter(Context context, int textViewResourceId, List<T> objects) {
        super(context, textViewResourceId, objects);
    }

    @Override
    public RefreshListener<List<T>> getRefreshListener() {
        return new DefaultRefreshListener<T>(this, getPagingSize()) {
            @Override
            public List<T> getItems(int offset) throws IOException {
                return getItemsOnRefresh(offset);
            }
        };
    }
    
    /**
     * @return 何アイテム数ごとにページングするかを返します。
     */
    protected abstract int getPagingSize();

    /**
     * @param {{@link {@link DefaultRefreshListener#PULL_TO_REFRESH} call by pull to refresh 
     * @see DefaultRefreshListener
     */
    protected abstract List<T> getItemsOnRefresh(int offset) throws IOException;
}
