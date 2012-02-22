/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.widget.listview;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

import org.klab.iphoroid.R;

import android.content.Context;
import android.os.AsyncTask;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;


/**
 * PullToRefreshEndlessListView.
 *
 * {@link #setAdapter(ListAdapter)} した自分で作成した Adapter は
 * 必ず {@link #getWrappedAdapter()} で取得するようにしてください。
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/07/10 sano-n initial version <br>
 */
public class PullToRefreshEndlessListView<T> extends PullToRefreshListView {

    /** */
    public PullToRefreshEndlessListView(Context context) {
        super(context);
    }

    public PullToRefreshEndlessListView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public PullToRefreshEndlessListView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    /** */
    public static interface RefreshListener<T> {

        /**
         * @return PullToRefresh した(上に引っ張る)場合にアイテムをダウンロードして返します。
         */
        T downloadUpper();

        /**
         * @param result PullToRefresh (上に引っ張る)でダウンロードしたアイテムを遅延バインドします。
         */
        void setResultUpper(T result);

        /**
         * @return OnRefresh した(上に引っ張る)場合にアイテムをダウンロードして返します。
         */
        T downloadLower();

        /**
         * @param OnRefresh (上に引っ張る)でダウンロードしたアイテムを遅延バインドします。
         */
        void setResultLower(T result);

        /**
         * @return ListView に現在存在するアイテム数を返します。
         */
        int sizeOf(T result);

        /**
         * @return 何アイテム数ごとにページングするかを返します。
         */
        int getPagingSize();
    }

    /** */
    private RefreshListener<T> refreshListener;

    /**
     * you should use #setAdapter({@link Refreshable})
     * @see #setAdapter(ListAdapter)
     */
    public void setRefreshListener(RefreshListener<T> listener) {
        this.refreshListener = listener;
        this.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                AsyncTask<Void, Void, T> task = new AsyncTask<Void, Void, T>() {
                    @Override
                    protected T doInBackground(Void... params) {
                        return refreshListener.downloadUpper();
                    }
                    @Override
                    protected void onPostExecute(T result) {
                        refreshListener.setResultUpper(result);

                        onRefreshComplete();
                    }
                };
                try {
                    task.execute();
                } catch (RejectedExecutionException e) {
                    Log.e("PullToRefreshEndlessListView", e.toString());
                    // TBD error handling
                }
            }

            @Override
            public void onRefreshFT() {
                AsyncTask<Void, Void, T> task = new AsyncTask<Void, Void, T>() {
                    @Override
                    protected T doInBackground(Void... params) {
                        return refreshListener.downloadLower();
                    }
                    @Override
                    protected void onPostExecute(T result) {
                        refreshListener.setResultLower(result);

                        onRefreshCompleteFT();
                    }
                };
                try {
                    task.execute();
                } catch (RejectedExecutionException e) {
                    Log.e("PullToRefreshEndlessListView", e.toString());
                    // TBD error handling
                }
            }
        });
    }

    /** implement to adapter */
    public static interface Refreshable<T> {
        RefreshListener<T> getRefreshListener();
    }

    /**
     * @param adapter should be implemented {@link Refreshable}
     */
    @SuppressWarnings("unchecked")
    @Override
    public void setAdapter(ListAdapter adapter) {
        if (!(adapter instanceof Refreshable)) {
            throw new IllegalStateException("adapter should be implemented Refreshable");
        }
        setRefreshListener(((Refreshable<T>) adapter).getRefreshListener());

        EndlessAdapter endlessAdapter = new EndlessAdapter(adapter, refreshListener.getPagingSize()) {
            @Override
            protected View getPendingView(ViewGroup parent) {
                return mInflater.inflate(R.layout.endress_footer, null);
            }
            @Override
            protected View getDummyView(ViewGroup parent) {
                return mInflater.inflate(R.layout.endress_dummy, null);
            }
            /** TBD thread safe? */
            private T result;
            @Override
            protected boolean cacheInBackground() throws Exception {
                this.result = refreshListener.downloadLower();
Log.d("PullToRefreshEndlessListView", "size: " + refreshListener.sizeOf(result));
                return refreshListener.sizeOf(result) > 0;
            }
            @Override
            protected void appendCachedData() {
                refreshListener.setResultLower(result);
            }
        };
        super.setAdapter(endlessAdapter);
    }

    /**
     * @param <I> should be implement equals() method correctly.
     * @see RefreshableArrayAdapter
     */
    public static abstract class DefaultRefreshListener<I> implements RefreshListener<List<I>> {

        /** */
        private ArrayAdapter<I> adapter;

        /** */
        private int pagingSize;

        public int getPagingSize() {
            return pagingSize;
        }

        /** */
        public static final int PULL_TO_REFRESH = -1;

        /** */
        public DefaultRefreshListener(ArrayAdapter<I> adapter, int pagingSize) {
            this.adapter = adapter;
            this.pagingSize = pagingSize;
        }

        @Override
        public List<I> downloadUpper() {
            return getUniqueItems(PULL_TO_REFRESH);
        }

        @Override
        public void setResultUpper(List<I> result) {
            for (int i = 0; i < result.size(); i++) {
                adapter.insert(result.get(i), i);
            }
        }

        @Override
        public List<I> downloadLower() {
            int page = adapter.getCount() / pagingSize;
Log.d("DefaultRefreshListener", "page: " + page);
            return getUniqueItems(page * pagingSize);
        }

        @Override
        public void setResultLower(List<I> result) {
            for (I item : result) {
                adapter.add(item);
            }
        }

        @Override
        public int sizeOf(List<I> result) {
            return result.size();
        }

        /** */
        public abstract List<I> getItems(int offset) throws IOException;

        /**
         * remove duplication
         * TBD これは外出ししたほうが良い？
         */
        private List<I> getUniqueItems(int offset) {
            List<I> result = new ArrayList<I>();
            try {
                for (I item : getItems(offset)) {
                    boolean exists = false;
                    for (int i = 0; i < adapter.getCount(); i++) {
//Log.d("DefaultRefreshListener", "COMPARE [" + i + "]: " + adapter.getItem(i) + ", " + item);
                        if (adapter.getItem(i).equals(item)) {
Log.d("DefaultRefreshListener", "DUPLICATE [" + i + "]: " + adapter.getItem(i) + ", " + item);
                            exists = true;
                            break;
                        }
                    }
                    if (!exists) {
                        result.add(item);
                    }
                }
            } catch (IOException e) {
                Log.e("DefaultRefreshListener", e.getMessage(), e);
            }
Log.d("DefaultRefreshListener", "offset: " + offset + ", size: " + result.size());
            return result;
        }
    }

    /** Gets the adapter you set */
    public ListAdapter getWrappedAdapter() {
        // EndlessAdapter { YourAdapter implements Refreshable }
        return ((EndlessAdapter) super.getWrappedAdapter()).getWrappedAdapter();
    }
}

/* */
