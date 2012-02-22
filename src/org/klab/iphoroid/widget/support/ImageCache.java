/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by iphoroid team
 */

package org.klab.iphoroid.widget.support;

import java.util.HashMap;
import java.util.Map;

import android.graphics.Bitmap;
import android.util.Log;

import org.klab.iphoroid.util.Cache;


/**
 * 画像データキャッシュ。
 *
 * <li>WeakHashMap は value を WeakReference でラップしないと GC しない</li>
 * <li>GC では結構消されるので使用に耐えない</li>
 * <li>WeakHashMap の監視は ReferenceQueue</li>
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 */
public abstract class ImageCache {

    /** */
    private static Map<String, Bitmap> cache;

    /** */
    private static Map<String, Bitmap> expired = new HashMap<String, Bitmap>();

static int hitCount;
static int unhitCount;

    /**
     * キャッシュしておく最大数
     */
    private static int maxSize = 70;

    /**
     * キャッシュしておく時間[msec]
     */
    private static int leftTime = 30 * 1000;

    /** */
    public static void setMaxSize(int maxSize) {
        ImageCache.maxSize = maxSize;
    }

    /** */
    public static void setLeftTime(int leftTime) {
        ImageCache.leftTime = leftTime;
    }

    static {
//        cache = new HashMap<String, Bitmap>();
        cache = new Cache<String, Bitmap>(maxSize, leftTime);
        ((Cache<String, Bitmap>) cache).setExpiredListener(new Cache.OnExpiredListener<Bitmap>() {
            @Override
            public void onExpired(Object key, Bitmap bitmap) {
                // ここだはまだ使用されている可能性があるので recycle() できない
Log.w("ImageCache", "Expired: " + bitmap + ", " + key);
                expired.put((String) key, bitmap);
            }
        });
    }

    /**
     * thread unsafe
     * @return null when no cache
     */
    public static Bitmap getImage(String key) {
try {
        if (cache.containsKey(key)) {
            Bitmap bitmap = cache.get(key);
++hitCount;
            return bitmap;
        } else {
++unhitCount;
            return null;
        }
} finally {
 Log.d("ImageCache", "cache: hit: " + hitCount + ", fail: " + unhitCount + ", size: " + cache.size() + ", key: " + key);
 Log.d("ImageCache", "mem: " + Runtime.getRuntime().freeMemory() + "/" + Runtime.getRuntime().totalMemory());
}
    }

    /** thread unsafe */
    public static void setImage(String key, Bitmap image) {
        try {
            cache.put(key, image);
        } catch (Exception e) {
Log.e("ImageCache", e.getMessage(), e);
        }
    }

    /** 
     * キャッシュをクリアします
     */
    public static void clear() {
        for (Bitmap bitmap : cache.values()) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
        }
        cache.clear();
    }

    /**
     * 
     * @return
     */
    public static Map<String, Bitmap> getExpired() {
        return expired;
    }
}
