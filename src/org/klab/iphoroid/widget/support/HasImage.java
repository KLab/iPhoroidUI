/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.widget.support;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.AsyncTask.Status;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;

import org.klab.iphoroid.widget.support.ImageDownloadTask.ImageDownloadHelper;


/**
 * {@link ListView} のヘルパークラスです。スクロール状態を検知して
 * イメージのダウンロードのコントロールを行います。
 * <p>
 * {@link org.klab.iphoroid.widget.adpterview.OnScrollListener} を実装している
 * {@link AdapterView} のために似たような機構を追加してあります。
 * </p>
 *
 * @see org.klab.iphoroid.widget.gallery.ScrollDetectableGallery
 * @see org.klab.iphoroid.widget.flowview.FlowView
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/06/16 sano-n initial version <br>
 */
public interface HasImage {

    /**
     * 以下の様に実装してください。
     * <pre>
     * class FooListActivity extends Activity implements HasImage {
     *
     *     private HasImage.ListViewOnScrollListener onScrollListener;
     *
     *     public int getScrollState() {
     *         return onScrollListener.getScrollState();
     *     }
     *
     *     private ListView listView;
     *
     *     public void onCreate(Bundle savedInstanceState) {
     *         super.onCreate(savedInstanceState);
     *
     *             :
     *
     *         this.listView = (ListView) findViewById(R.id.listView);
     *         this.onScrollListener = new HasImage.ListViewOnScrollListener();
     *         listView.setOnScrollListener(onScrollListener);
     *
     *             :
     * </pre>
     */
    int getScrollState();

    /**
     * スクロール状態が
     * <li>IDLE の場合は、ListView の更新、イメージキャッシュの破棄されたキャッシュイメージの解放</li>
     * <li>FLING → TOUCH_SCROLL の場合、ListView の更新</li>
     * を行います。
     */
    class ListViewOnScrollListener implements OnScrollListener {

        /** 以前のスクロール状態 */
        protected int scrollState;

        /** */
        public int getScrollState() {
            return scrollState;
        }

        /* AbsListView 返すから Gallery と統合できない...orz */
        public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//Log.i("HasImage.ListOnScrollListener", "onScroll: " + scrollState);
        }

        public void onScrollStateChanged(AbsListView view, int scrollState) {
            if (scrollState == SCROLL_STATE_IDLE) {
                // 1. update view (必要確認済み)
                view.invalidateViews();

                // 2. clear cache
                Util.recycleImages(view);
            } else if (this.scrollState == SCROLL_STATE_FLING &&
                       scrollState == SCROLL_STATE_TOUCH_SCROLL) {
//Log.i("HasImage.ListOnScrollListener", "onScroll: 2 -> 1");
                // update view (必要確認済み)
                view.invalidateViews();
            }

            this.scrollState = scrollState;
//Log.i("HasImage.ListOnScrollListener", "onScrollStateChanged: " + scrollState);
        }
    }

    /**
     * スクロール状態が
     * <li>IDLE の場合は、AdapterView の更新、イメージキャッシュの破棄されたキャッシュイメージの解放</li>
     * <li>FLING → TOUCH_SCROLL の場合、AdapterView の更新</li>
     * を行います。
     */
    class AdapterViewOnScrollListener implements org.klab.iphoroid.widget.adpterview.OnScrollListener {
        /** 以前のスクロール状態 */
        private int scrollState;

        /** */
        public int getScrollState() {
            return scrollState;
        }

        public void onScroll(AdapterView<?> view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
//Log.i("HasImage.AdapterViewOnScrollListener", "onScroll: " + scrollState);
        }

        public void onScrollStateChanged(AdapterView<?> view, int scrollState) {
Log.i("HasImage.AdapterViewOnScrollListener", "onScrollStateChanged.1: " + this.scrollState + ", " + scrollState);
            if (scrollState == SCROLL_STATE_IDLE) {
                // 1. update view
                ((BaseAdapter) view.getAdapter()).notifyDataSetChanged();

                // 2. clear cache
                Util.recycleImages(view);
            } else if (scrollState == SCROLL_STATE_TOUCH_SCROLL) {
//Log.i("HasImage.AdapterViewOnScrollListener", "onScroll: 2 -> 1");
                // update view
                ((BaseAdapter) view.getAdapter()).notifyDataSetChanged();
            }

            this.scrollState = scrollState;
Log.i("HasImage.AdapterViewOnScrollListener", "onScrollStateChanged.2: " + scrollState);
        }
    }

    /**
     * イメージの遅延ローディングやキャッシングをサポートするユーティリティクラスです。
     */
    public class Util {

        /** tasks currently running */
        private static Map<View, AsyncTask<?, ?, ?>> tasks = new HashMap<View, AsyncTask<?, ?, ?>>();

        /**
         * task で取得してきた Bitmap を imageView に設定します。
         *
         * @see #setImage(Context, String, ImageView, Drawable, AsyncTask, String)
         * @param context should be {@link HasImage}
         * @param url for image
         * @param loadingDrawable
         * @param noImageDrawable image for empty
         * @param task image download
         */
        public static void setImage(Context context, String url, ImageView imageView, Drawable loadingDrawable, Drawable noImageDrawable) {
            setImage(context, url, imageView, new ImageDownloadTask(imageView, new ImageDownloadTask.DefaultImageDownloadHelper(loadingDrawable, noImageDrawable)), null);
        }

        /**
         * task で取得してきた Bitmap を imageView に設定します。
         *
         * @see #setImage(Context, String, ImageView, Drawable, AsyncTask, String)
         * @param context should be {@link HasImage}
         * @param url for image
         * @param imageDownloadHelper
         * @param task image download
         */
        public static void setImage(Context context, String url, ImageView imageView, ImageDownloadHelper<String> imageDownloadHelper) {
            setImage(context, url, imageView, new ImageDownloadTask(imageView, imageDownloadHelper), null);
        }

        /**
         * task で取得してきた Bitmap を imageView に設定します。
         *
         * @param context should be {@link HasImage}
         * @param url for image
         * @param imageView the image to be set, if null using progress bar
         * @param task image download
         */
        public static void setImage(Context context, String url, ImageView imageView, AsyncTask<String, Void, Bitmap> task) {
            setImage(context, url, imageView, task, null);
        }

        /**
         * task で取得してきた Bitmap を imageView に設定します。
         *
         * <ul>
         * <li>イメージがキャッシュにあればそのまま表示
         * <li>無ければ
         *   <ul>
         *   <li>スクロール状態が FLING ではない場合、
         *     <ul>
         *     <li>ImageView をキーにしたタスクリストが存在すればすべてタスクキャンセル
         *     <li>イメージダウンロードタスク実行
         *     </ul>
         *   </ul>
         * </ul>
         *
         * @param context should be {@link HasImage}
         * @param url for image
         * @param imageView the image to be set, if null using progress bar
         * @param task image download
         * @param postfix key for cache (when null, ignored)
         */
        public static void setImage(Context context, String url, ImageView imageView, AsyncTask<String, Void, Bitmap> task, String postfix) {
            if (context instanceof HasImage) {
//Log.d("HasImage.Util", "scrolling: " + ((HasImage) context).getScrollState());
                Bitmap image = ImageCache.getImage(postfix != null ? url + postfix : url);
                if (image == null) {
                    if (((HasImage) context).getScrollState() != ListView.OnScrollListener.SCROLL_STATE_FLING) { // OnScrollListener#SCROLL_STATE_FLING
                        for (Map.Entry<View, AsyncTask<?, ?, ?>> entry : tasks.entrySet()) {
                            if (entry.getKey().equals(imageView)) {
                                boolean r = entry.getValue().cancel(true);
Log.d("HasImage.Util", "cancel: " + imageView + ", " + r);
                            }
                        }
                        Iterator<AsyncTask<?, ?, ?>> i = tasks.values().iterator();
                        try {
                            while (i.hasNext()) {
                                AsyncTask<?, ?, ?> t = i.next();
                                if (t.isCancelled() || t.getStatus().equals(Status.FINISHED)) {
                                    i.remove();
                                }
                            }
                        } catch (Exception e) { // for ConcurrentModificationException
                            Log.e("HasImage.Util", e.toString());
                        }
                        try {
                            task.execute(url);
                            tasks.put(imageView, task);
Log.d("HasImage.Util", "mem: " + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().maxMemory() + ", tasks: " + tasks.size());
                        } catch (RejectedExecutionException e) {
                            Log.e("HasImage.Util", e.toString());
                        }
                    } else {
Log.i("HasImage.Util", "flinging");
                    }
                } else {
                    imageView.setImageBitmap(image);
                }
            } else {
                throw new IllegalStateException("context must be HasImage");
            }
        }

        /** @see #recycleImages(ViewGroup) */
        private static Executor executor = Executors.newSingleThreadExecutor();

        /**
         * キャッシュでエキスパイアされたイメージで且つ ViewGroup で表示されていない
         * イメージを別 Thread で recycle() します。
         */
        public static void recycleImages(final ViewGroup viewGroup) {
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        setImageViewsOfCurrentActivity(viewGroup);
                        recycleImages();
                        clearImageViewsOfCurrentActivity();
                    } catch (Exception e) {
                        Log.e("HasImage.Util", e.getMessage(), e);
                    }
                }
            });
        }

        /**
         * キャッシュでエキスパイアされたイメージで且つ ViewGroup で表示されていない
         * イメージを recycle() します。
         *
         * 現在の Activity の ImageView は登録されていませんので自分で追加しておいてください。
         *
         * キャッシュはいつか消えてくれるので ConcurrentModificationException は無視します。
         * パフォーマンスのため、ImageCache をスレッドセーフにはしません。
         *
         * TBD background?
         *
         * @see #setImageViewsOfActivity(Activity)
         */
        private static void recycleImages() {
            try {
                for (Map.Entry<String, Bitmap> entry : ImageCache.getExpired().entrySet()) {
                    String key = entry.getKey();
                    Bitmap bitmap = entry.getValue();
Log.v("HasImage.Util", "expired: " + bitmap);
                    if (bitmap != null) {
                        if (!bitmap.isRecycled()) { // 2011.7.27 fixed by jun
                            if (!isUsed(bitmap)) {
                                bitmap.recycle();
Log.w("HasImage.Util", "recycled: " + bitmap);
                            } else {
                                ImageCache.setImage(key, bitmap);
Log.v("HasImage.Util", "used, recache: " + bitmap);
                            }
                        }
                    }
                }
            } catch (Exception e) { // for ConcurrentModificationException
                Log.e("HasImage.Util", e.toString());
            }
            try {
                Iterator<Bitmap> i = ImageCache.getExpired().values().iterator();
                while (i.hasNext()) {
                    Bitmap bitmap = i.next();
                    if (bitmap != null && bitmap.isRecycled()) {
                        i.remove();
                    }
                }
            } catch (Exception e) { // for ConcurrentModificationException
                Log.e("HasImage.Util", e.toString());
            }
        }

        /** Gets image of views on screen. */
        private static void getChildImageViews(ViewGroup viewGroup, Set<ImageView> used) {
            for (int i = 0; i < viewGroup.getChildCount(); i++) {
                View view = viewGroup.getChildAt(i);
                if (view instanceof ImageView) {
                    ImageView imageView = (ImageView) viewGroup.getChildAt(i);
                    used.add(imageView);
                } else if (view instanceof ViewGroup) {
                    ViewGroup viewGroup_ = (ViewGroup) viewGroup.getChildAt(i);
                    getChildImageViews(viewGroup_, used);
                }
            }
        }

        /** imageViews currently used, key is {@link Activity#hashCode()} */
        private static Map<Integer, Set<ImageView>> imageViewsOfActivity = new HashMap<Integer, Set<ImageView>>();

        /** @see #imageViewsOfActivity */
        private static boolean isUsed(Bitmap target) {
            for (ImageView imageView : getImageViewsOfActivities()) {
                BitmapDrawable bitmapDrawable = (BitmapDrawable) imageView.getDrawable();
                if (bitmapDrawable != null) {
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    if (bitmap == target) {
                        return true;
                    }
                }
            }
            return false;
        }

        /** @see #imageViewsOfActivity */
        private static Set<ImageView> getImageViewsOfActivities() {
            Set<ImageView> result = new HashSet<ImageView>();
            for (Set<ImageView> set : imageViewsOfActivity.values()) {
                for (ImageView imageView : set) {
                    result.add(imageView);
                }
            }
            return result;
        }

        /**
         * Activity 中の ImageView を {@link #imageViewsOfActivity} に追加します。
         * 
         * @see #clearImageViewsOfActivity(Activity)
         */
        private static void setImageViewsOfActivity(Activity activity) {
            ViewGroup root = (ViewGroup) activity.findViewById(android.R.id.content);
            setImageViewsOfActivity(root, activity.hashCode());
Log.d("HasImage.Util", "SET: remainings: " + activity.getClass().getSimpleName());
        }

        /**
         * {@link #imageViewsOfActivity} のこの Activity に紐づいている ImageView を削除します。
         *
         * @see #setImageViewsOfActivity(Activity)
         */
        private static void clearImageViewsOfActivity(Activity activity) {
            clearImageViewsOfActivity(activity.hashCode());
Log.d("HasImage.Util", "CLEAR: remainings: " + activity.getClass().getSimpleName());
        }

        /** */
        private static void setImageViewsOfActivity(ViewGroup root, int hashCode) {
            Set<ImageView> imageViews = new HashSet<ImageView>();
            getChildImageViews(root, imageViews);
            imageViewsOfActivity.put(hashCode, imageViews);
Log.d("HasImage.Util", "SET: remainings: " + getImageViewsOfActivities().size());
        }

        /** */
        private static void clearImageViewsOfActivity(int hashCode) {
            try {
                imageViewsOfActivity.remove(hashCode);
            } catch (Exception e) { // for ConcurrentModificationException
                Log.e("HasImage.Util", e.toString());
            }
Log.d("HasImage.Util", "CLEAR: remainings: " + getImageViewsOfActivities().size());
        }

        /** 0 (Object#hashCode()) is really unique? */
        private static final int CURRENT_ACTIVITY_ID = 0;

        /** */
        private static void setImageViewsOfCurrentActivity(ViewGroup root) {
            setImageViewsOfActivity(root, CURRENT_ACTIVITY_ID);
        }

        /** */
        private static void clearImageViewsOfCurrentActivity() {
            clearImageViewsOfActivity(CURRENT_ACTIVITY_ID);
        }

        /**
         * {@link HasImage} もしくは
         * {@link SimpleImageDownloadTask#downloadImage(Context, String, ImageView)}
         * 等を 使用しているアクティビティで以下の様に実装します。
         *
         * <pre>
         *     &#064;Override
         *     protected void onResume() {
         *         super.onResume();
         *
         *         HasImage.Util.onResume(this);
         *     }
         *
         *     &#064;Override
         *     protected void onPause() {
         *         super.onPause();
         *
         *         HasImage.Util.onPause(this);
         *     }
         *
         *     &#064;Override
         *     protected void onDestroy() {
         *         super.onDestroy();
         *
         *         HasImage.Util.onDestroy(this);
         *     }
         * </pre>
         *
         * TBD 超面倒
         */
        public static void onResume(Activity activity) {
            clearImageViewsOfActivity(activity);
        }

        /**
         * {@link HasImage} もしくは
         * {@link SimpleImageDownloadTask#downloadImage(Context, String, ImageView)}
         * 等を 使用しているアクティビティで以下の様に実装します。
         *
         * <pre>
         *     &#064;Override
         *     protected void onResume() {
         *         super.onResume();
         *
         *         HasImage.Util.onResume(this);
         *     }
         *
         *     &#064;Override
         *     protected void onPause() {
         *         super.onPause();
         *
         *         HasImage.Util.onPause(this);
         *     }
         *
         *     &#064;Override
         *     protected void onDestroy() {
         *         super.onDestroy();
         *
         *         HasImage.Util.onDestroy(this);
         *     }
         * </pre>
         *
         * TBD 超面倒
         */
        public static void onPause(Activity activity) {
            clearImageViewsOfActivity(activity);
            setImageViewsOfActivity(activity);
            recycleImages();
        }

        /**
         * {@link HasImage} もしくは
         * {@link SimpleImageDownloadTask#downloadImage(Context, String, ImageView)}
         * 等を 使用しているアクティビティで以下の様に実装します。
         *
         * <pre>
         *     &#064;Override
         *     protected void onResume() {
         *         super.onResume();
         *
         *         HasImage.Util.onResume(this);
         *     }
         *
         *     &#064;Override
         *     protected void onPause() {
         *         super.onPause();
         *
         *         HasImage.Util.onPause(this);
         *     }
         *
         *     &#064;Override
         *     protected void onDestroy() {
         *         super.onDestroy();
         *
         *         HasImage.Util.onDestroy(this);
         *     }
         * </pre>
         *
         * TBD 超面倒
         */
        public static void onDestroy(Activity activity) {
            clearImageViewsOfActivity(activity);
        }
    }
}

/* */
