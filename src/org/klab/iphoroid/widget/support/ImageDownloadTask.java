/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by iphoroid team
 */

package org.klab.iphoroid.widget.support;

import java.io.IOException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;


/**
 * ImageDownloadTask. 
 * 
 * @author <a href="mailto:kodama-t@klab.jp">Takuya KODAMA</a> (kodamta-t)
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 */
public final class ImageDownloadTask extends AsyncTask<String, Void, Bitmap> {

    /** */
    public static interface ImageDownloadHelper<T> {
        /**
         * イメージを読み込む前に imageView に設定する操作を書いてください。
         * @param imageView
         */
        void onPreDownload(ImageView imageView);
        /**
         * 実際に Bitmap を取得する処理を書いてください。
         * @param param
         */
        Bitmap doDownload(T param) throws IOException;
        /**
         * イメージが上手く取得できた場合 imageView に Bitmap が設定される前に呼び出されます。
         * @param imageView
         */
        void onDownloadSuccess(ImageView imageView);
        /**
         * エラーが起こった場合など imageView に no image 状態をセットする場合に呼び出されます。
         * @param imageView
         */
        void onDownloadFailure(ImageView imageView);
    }

    /** */
    public static class DefaultImageDownloadHelper implements ImageDownloadHelper<String> {
        /** イメージがロード中に設定する画像 */
        private Drawable loadingDrawable;
        /** イメージがロード出来なかった場合に設定する画像 */
        private Drawable noImageDrawable;
        /**
         * @param loadingDrawable イメージがロード中に設定する画像
         * @param noImageDrawable イメージがロード出来なかった場合に設定する画像
         */
        public DefaultImageDownloadHelper(Drawable loadingDrawable, Drawable noImageDrawable) {
            this.loadingDrawable = loadingDrawable;
            this.noImageDrawable = noImageDrawable;
        }
        /* ロード中のイメージを imageView に設定します。 */
        public void onPreDownload(ImageView imageView) {
            imageView.setImageDrawable(loadingDrawable);
        }
        /* 何もしません。 */
        public void onDownloadSuccess(ImageView imageView) {
        }
        public void onDownloadFailure(ImageView imageView) {
            imageView.setImageDrawable(noImageDrawable);
        }
        /* ファイル名で Bitmap を読み込みます。 */
        public Bitmap doDownload(String param) throws IOException {
            return BitmapFactory.decodeFile(param);
        }
    }

    private ImageView imageView;
    /** 重複チェックに使用 (kodama-t オリジナルすばらしい！) */
    private Integer tag;

static int queueInCount;
static int executeCount;
static int cancelCount;
static int doneCount;
static int errorCount;
static int runningCount;

    /** */
    private ImageDownloadHelper<String> imageDownloadHelper;

    /**
     * @param imageView to be set after download, should be set position to tag
     */
    public ImageDownloadTask(ImageView imageView, ImageDownloadHelper<String> imageDownloadHelper) {
        this.imageView = imageView;
        this.tag = (Integer) (imageView.getTag()); // これ以降 tag の値が変わるということやんね
        this.imageDownloadHelper = imageDownloadHelper;
++queueInCount;
++runningCount;
    }

    @Override
    protected void onPreExecute() {
        imageDownloadHelper.onPreDownload(imageView);
        super.onPreExecute();
    }

    /**
     * 取得した画像は自動的にキャッシュします。
     *  
     * @param urls [0] image URL
     * @return null when download failed or canceled
     * @see DetailImageDownloadTask
     */
    @Override
    protected Bitmap doInBackground(String... urls) {
        try {
            if (tag != null && tag.equals(imageView.getTag())) {
                // HasImage での Cache チェックとダブルけど
                // このタスク自体遅延してかぶる場合があるので
                Bitmap image = ImageCache.getImage(urls[0]);
                if (image == null) {
Log.i("ImageDownloadTask", "loading: " + tag + ", " + urls[0]);
                    image = imageDownloadHelper.doDownload(urls[0]);
++executeCount;
                    if (image != null) {
                        ImageCache.setImage(urls[0], image);
                    } else {
Log.w("ImageDownloadTask", "canceled : " + urls[0]);
                    }
                }
                return image;
            } else {
Log.w("ImageDownloadTask", "tag not match 1: " + tag);
                return null;
            }
} catch (java.io.FileNotFoundException e) {
 ++errorCount;
 Log.e("ImageDownloadTask", urls[0] + ": " + e.toString());
 return null;
        } catch (NullPointerException e) { // for at org.klab.iphoroid.net.HTTPClient.getByteArrayFromURL(HTTPClient.java:62)
 Log.w("ImageDownloadTask", "may be canceled?: " + urls[0]);
            return null;
        } catch (Exception e) {
++errorCount;
            Log.e("ImageDownloadTask", urls[0] + ": " + e.getMessage(), e);
            return null;
        }
    }

    @Override
    protected void onCancelled() {
        super.onCancelled();
        imageDownloadHelper.onDownloadSuccess(imageView);
Log.w("ImageDownloadTask", "canceled: " + tag);
++cancelCount;
--runningCount;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        if (result != null) {
            if (tag != null && tag.equals(imageView.getTag())) { // view 使いまわされ対策
                imageView.setImageBitmap(result);
Log.i("ImageDownloadTask", "done: " + tag);
++doneCount;
            } else {
Log.w("ImageDownloadTask", "tag not match 2: " + tag);
            }
        } else {
            if (tag != null && tag.equals(imageView.getTag())) {
                imageDownloadHelper.onDownloadFailure(imageView);
            }
        }
        imageDownloadHelper.onDownloadSuccess(imageView);
--runningCount;
Log.w("ImageDownloadTask", "in: " + queueInCount + ", exec: " + executeCount + ", error: " + errorCount + ", cancel: " + cancelCount + ", done: " + doneCount + ", run: " + runningCount);
    }
}
