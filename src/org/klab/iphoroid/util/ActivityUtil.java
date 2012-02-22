/*
 * Copyright (c) 2011 by KLab Inc., All rights reserved.
 *
 * Programmed by Naohide Sano
 */

package org.klab.iphoroid.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import android.app.Activity;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.util.Log;
import android.view.Display;


/**
 * ActivityUtil.
 * 
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version 0.00 2011/07/06 sano-n initial version <br>
 */
public abstract class ActivityUtil {

    private static final String TAG = "ActivityUtil";

    /**
     * ランタイムに縦か横かを取得します。
     * 
     * @see "http://stackoverflow.com/questions/2795833/check-orientation-on-android-phone"
     */
    public static int getScreenOrientation(Activity activity) {
        Display getOrient = activity.getWindowManager().getDefaultDisplay();
        int orientation = Configuration.ORIENTATION_UNDEFINED;
        if (getOrient.getWidth() == getOrient.getHeight()) {
            orientation = Configuration.ORIENTATION_SQUARE;
        } else {
            if (getOrient.getWidth() < getOrient.getHeight()) {
                orientation = Configuration.ORIENTATION_PORTRAIT;
            } else {
                orientation = Configuration.ORIENTATION_LANDSCAPE;
            }
        }
        return orientation;
    }

    /**
     * 指定されたファイルの内容を文字列として返します.
     * 
     * @param activity
     * @param fileName ファイル名
     * @param charset ファイルのエンコーディング
     * @return ファイルの内容
     */
    public static String loadAssetsText(Activity activity, String fileName, String charset) {
        return loadAssetsText(activity.getResources().getAssets(), fileName, charset);
    }

    /**
     * 指定されたファイルの内容を文字列として返します.
     * 
     * @param assetManager
     * @param fileName ファイル名
     * @param charset ファイルのエンコーディング
     * @return ファイルの内容
     */
    public static String loadAssetsText(AssetManager assetManager, String fileName, String charset) {
        String text = null;
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(assetManager.open(fileName), charset));
            StringBuilder sb = new StringBuilder();
            while ((text = br.readLine()) != null) {
                sb.append(text);
            }
            text = sb.toString();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage(), e);
        }
        return text;
    }
}

/* */
