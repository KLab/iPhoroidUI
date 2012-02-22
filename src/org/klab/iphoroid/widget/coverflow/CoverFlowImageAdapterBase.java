/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.widget.coverflow;

import java.util.List;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PorterDuff.Mode;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.Shader.TileMode;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;


/**
 * CoverFlow と画像を関連付けるためのアダプターです。
 *
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/06/23 sano-n initial version <br>
 */
public abstract class CoverFlowImageAdapterBase<T> extends BaseAdapter {
    /**
     * 元画像と反射エフェクト間の距離。
     */
    protected static int REFLECTION_GAP = 4;

    /**
     * コンテキスト。
     */
    protected Context mContext;

    /**
     * 反射エフェクトを使用することを示す値。
     */
    protected boolean mIsUserEffect;

    /**
     * レイアウト情報。
     */
    protected Gallery.LayoutParams mLayoutParams;

    /**
     * 画像のリソース ID コレクション。
     */
    protected List<T> items;

    /**
     * アイテムの幅と高さを指定して、インスタンスを初期化します。
     *
     * @param context コンテキスト。
     * @param items 画像のリソース ID コレクション。
     * @param width アイテムの幅。
     * @param height アイテムの高さ。
     * @param isUserEffect 反射エフェクトを使用する場合は true。それ以外は false。
     */
    public CoverFlowImageAdapterBase(Context context, List<T> items, int width, int height, boolean isUserEffect) {
        this.mContext = context;
        this.items = items;
        this.mIsUserEffect = isUserEffect;
        this.mLayoutParams = new Gallery.LayoutParams(width, height);
    }

    public int getCount() {
        return items.size();
    }

    public Object getItem(int position) {
        return items.get(position);
    }

    public long getItemId(int position) {
        return position;
    }

    /**
     * ビューの内容を取得します。
     *
     * @param position ビュー内における、アイテムの表示位置を示すインデックス。
     * @param convertView 表示領域となるビュー。
     * @param parent 親となるビュー。
     */
    public abstract View getView(int position, View convertView, ViewGroup parent);

    /**
     * 画像の下部に反射エフェクトを付けた Bitmap を生成します。
     *
     * @param src 元となる画像。
     * @param gap 元画像と反射エフェクト間の距離。
     * @return 成功時は Bitmap インスタンス。それ以外は null 参照。
     */
    protected Bitmap makeReflectedImage(Bitmap src, int gap) {
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        int width = src.getWidth();
        int height = src.getHeight();
        int destHeight = height + height / 2;
        Bitmap effect = Bitmap.createBitmap(src, 0, height / 2, width, height / 2, matrix, false);
//        Bitmap dest = Bitmap.createBitmap(width, destHeight, Config.ARGB_4444);
        Bitmap dest = Bitmap.createBitmap(width, destHeight, Config.ARGB_8888);
        Canvas canvas = new Canvas(dest);

        canvas.drawBitmap(src, 0, 0, null);
        canvas.drawRect(0, height, width, height + gap, new Paint());
        canvas.drawBitmap(effect, 0, height + gap, null);

        Paint paint = new Paint();
        paint.setShader(new LinearGradient(0, height, 0, destHeight + gap, 0x70ffffff, 0x00ffffff, TileMode.CLAMP));
        paint.setXfermode(new PorterDuffXfermode(Mode.DST_IN));
        canvas.drawRect(0, height, width, destHeight + gap, paint);

        if (effect != null && !effect.isRecycled()) {
        	effect.recycle();
        }

        return dest;
    }

    /** */
    protected Bitmap makeResizedImage(Bitmap src, int width, int height) {
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

//        Bitmap dest = Bitmap.createBitmap(width, height, Config.ARGB_8888);
        Bitmap dest = Bitmap.createBitmap(width, height, Config.RGB_565);
        Canvas canvas = new Canvas(dest);
        canvas.drawBitmap(src, new Rect(0, 0, src.getWidth(), src.getHeight()), new Rect(0, 0, width, height), null);

        return dest;
    }

    /**
     * リソース ID から画像を取得します。
     *
     * @param id リソース ID。
     * @return 成功時は Bitmap インスタンス。それ以外は null 参照。
     */
    protected Bitmap decodeBitmap(int id) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(mContext.getResources(), id, options);

        int width = (options.outWidth / mLayoutParams.width) + 1;
        int height = (options.outHeight / mLayoutParams.height) + 1;
        int scale = Math.max(width, height);

        options.inJustDecodeBounds = false;
        options.inSampleSize = scale;

        return BitmapFactory.decodeResource(mContext.getResources(), id, options);
    }
}
