/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by iphoroid team
 */

package org.klab.iphoroid.widget.support;

import java.io.IOException;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;


/**
 * SimpleImageDownloadTask. 
 *
 * @author <a href="mailto:kodama-t@klab.jp">Takuya KODAMA</a> (kodamta-t)
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 */
public abstract class SimpleImageDownloadTask extends DownloadTask<String, Bitmap> {

    /**
     * @param context for dialog 
     */
    public SimpleImageDownloadTask(Context context) {
        super(context, null);
    }

    /**
     * @param context for dialog 
     */
    public SimpleImageDownloadTask(Context context, String message) {
        super(context, message);
    }

    /**
     * 取得した画像は自動的にキャッシュします。
     * キャッシュにあるかどうか判定します。
     *  
     * @param urls [0] image URL
     * @return null when error occurs
     * @see ImageDownloadTask
     */
    @Override
    public final Bitmap download(String... urls) {
        try {
            Bitmap image = ImageCache.getImage(urls[0]);
            if (image == null) {
Log.d("SimpleImageDownloadTask", "loading URL: " + urls[0]);
                image = getBitmap(urls[0]);
                ImageCache.setImage(urls[0], image);
            }
            return image;
        } catch (Exception e) {
Log.e("SimpleImageDownloadTask", urls[0] + ": " + e.toString());
            return null;
        }
    }
    
    public final Bitmap downloadSetResult(String... urls) {
        try {
            Bitmap image = ImageCache.getImage(urls[0]);
            if (image == null) {
Log.d("SimpleImageDownloadTask", "loading URL: " + urls[0]);
                image = getBitmap(urls[0]);
                ImageCache.setImage(urls[0], image);
            }
            return image;
        } catch (Exception e) {
Log.e("SimpleImageDownloadTask", urls[0] + ": " + e.toString());
            return null;
        }
    }

    /** */
    public abstract Bitmap getBitmap(String url) throws IOException;
}
