/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * This code is base on the Android Gallery widget and was modify
 * by Neil Davies neild001 'at' gmail dot com to be a Coverflow widget
 */

package org.klab.iphoroid.widget.coverflow;

import android.content.Context;
import android.graphics.Camera;
import android.graphics.Matrix;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Transformation;
import android.widget.ImageView;

import org.klab.iphoroid.widget.gallery.ScrollDetectableGallery;


/**
 * CoverFlow 風の Gallery 機能を提供します。
 *
 * @author Neil Davies
 * @author <a href="mailto:sano-n@klab.jp">Naohide Sano</a> (sano-n)
 * @version Neil Davies original
 * @version sano-n {@link ScrollDetectableGallery} を使用するようにした
 */
public class CoverFlowGallery extends ScrollDetectableGallery {
    /**
     * ImageView を変形させるためのカメラ。
     */
    private Camera mCamera = new Camera();

    /**
     * 回転時の傾きとなる角度。
     */
    private int mMaxRotationAngle = 60;

    /**
     * 拡大率。
     */
    private int mMaxZoom = -120;

    /**
     * CoverFlow の中心となる座標。
     */
    private int mCoveflowCenter;

    /**
     * インスタンスを初期化します。
     * 
     * @param context コンテキスト。
     */
    public CoverFlowGallery(Context context) {
        super(context);
        this.setStaticTransformationsEnabled(true);
    }

    /**
     * インスタンスを初期化します。
     * 
     * @param context コンテキスト。
     * @param attrs レイアウト XML などで設定されたコントロールの属性情報。
     */
    public CoverFlowGallery(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.setStaticTransformationsEnabled(true);
    }

    /**
     * インスタンスを初期化します。
     * 
     * @param context コンテキスト。
     * @param attrs レイアウト XML などで設定されたコントロールの属性情報。
     * @param defStyle 定義されたスタイルの識別子。
     */
    public CoverFlowGallery(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        this.setStaticTransformationsEnabled(true);
    }

    /**
     * CoverFlow における X 軸の中央位置を取得します。
     * 
     * @return 位置。
     */
    private int getCenterOfCoverflow() {
        int left = this.getPaddingLeft();
        return (getWidth() - left - getPaddingRight()) / 2 + left;
    }

    /**
     * View における X 軸の中央位置を取得します。
     * 
     * @param view 位置を取得する View。
     * 
     * @return 位置。
     */
    private static int getCenterOfView(View view) {
        return view.getLeft() + view.getWidth() / 2;
    }

    /**
     * CoverFlow を構成する子 View の変形を実行します。
     * 
     * @param child 変形する View。
     * @param t 変形情報。
     * 
     * @return getChildDrawingOrder に定義された順で描画する場合は true。それ以外は false。
     */
    @Override
    protected boolean getChildStaticTransformation(View child, Transformation t) {
        final int center = getCenterOfView(child);
        final int width = child.getWidth();
        int angle = 0;

        t.clear();
        t.setTransformationType(Transformation.TYPE_MATRIX);

        if (center == mCoveflowCenter) {
            this.transformImageBitmap((ImageView) child, t, 0);

        } else {
            angle = (int) (((float) (this.mCoveflowCenter - center) / width) * this.mMaxRotationAngle);
            if (Math.abs(angle) > this.mMaxRotationAngle) {
                angle = (angle < 0) ? -this.mMaxRotationAngle : this.mMaxRotationAngle;
            }
//Log.d("CoverFlowGallery", "angle: " + angle + ", mCoveflowCenter: " + mCoveflowCenter + ", center: " + center + ", width: " + width + ", mMaxRotationAngle: " + mMaxRotationAngle);

            this.transformImageBitmap((ImageView) child, t, angle);
        }

        return true;
    }

    /**
     * 回転時の最大角度を取得します。
     * 
     * @return 角度。
     */
    public int getMaxRotationAngle() {
        return this.mMaxRotationAngle;
    }

    /**
     * 回転時の最大角度を設定します。
     * 
     * @param angle 角度。
     */
    public void setMaxRotationAngle(int angle) {
        this.mMaxRotationAngle = angle;
    }

    /**
     * 拡大率を取得します。
     * 
     * @return 拡大率。
     */
    public int getMaxZoom() {
        return this.mMaxZoom;
    }

    /**
     * 拡大率を設定します。
     * 
     * @param zoom 拡大率。
     */
    public void setMaxZoom(int zoom) {
        this.mMaxZoom = zoom;
    }

    /**
     * コントロールのサイズが変更された時に発生します。
     * 
     * @param w 現在の幅。
     * @param h 現在の高さ。
     * @param oldw サイズが変更される前の幅。
     * @param oldh サイズが変更される前の高さ。
     */
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        this.mCoveflowCenter = this.getCenterOfCoverflow();
        super.onSizeChanged(w, h, oldw, oldh);
    }

    /**
     * 画像を変形させます。
     * 
     * @param child 画像の描画領域となる View。
     * @param t 変形情報。
     * @param angle 変形させる角度。
     */
    private void transformImageBitmap(ImageView child, Transformation t, int angle) {
        this.mCamera.save();

        final Matrix matrix = t.getMatrix();
        final int imageHeight = child.getLayoutParams().height;
        final int imageWidth = child.getLayoutParams().width;
        final int rotation = Math.abs(angle);

        this.mCamera.translate(0.0f, 0.0f, 100.0f);

        // 拡大率の算出
        if (rotation < this.mMaxRotationAngle) {
            float zoom = (float) (this.mMaxZoom + (rotation * 1.5));
            this.mCamera.translate(0.0f, 0.0f, zoom);
        }

        this.mCamera.rotateY(angle);
        this.mCamera.getMatrix(matrix);
        matrix.preTranslate(-(imageWidth / 2), -(imageHeight / 2));
        matrix.postTranslate((imageWidth / 2), (imageHeight / 2));
        this.mCamera.restore();
    }

    @Override
    protected int getChildDrawingOrder(int childCount, int i) {
        int selectedIndex = getSelectedItemPosition() - getFirstVisiblePosition();

        // Just to be safe
        if (selectedIndex < 0) {
            return i;
        }

        if (i == childCount - 1) {
            // Draw the selected child last
            return selectedIndex;
        } else if (i >= selectedIndex) {
            // Move the children to the right of the selected child earlier one
            int index = childCount - 1 - (i - selectedIndex);

            return index;
        } else {
            // Keep the children to the left of the selected child the same
            return i;
        }
    }
}
