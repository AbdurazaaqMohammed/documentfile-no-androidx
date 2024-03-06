/*
 * Copyright 2024 The Android Open Source Project
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
 */

package androidx.pdf.widget;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Point;
import android.graphics.Rect;
import android.view.View;

import androidx.annotation.RestrictTo;
import androidx.pdf.aidl.Dimensions;
import androidx.pdf.util.ErrorLog;
import androidx.pdf.util.Preconditions;
import androidx.pdf.util.TileBoard.TileInfo;

import java.util.Objects;

/**
 * A basic image view that holds one tile (a bitmap that is a part of a larger image).
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@SuppressWarnings("UnusedVariable")
public class TileView extends View {

    private static final Point ZERO = new Point();
    private static final Matrix IDENTITY = new Matrix();

    private final Dimensions mSize;
    private final Rect mBounds;

    final TileInfo mTileInfo;
    private Bitmap mBitmap;

    public TileView(Context context, TileInfo tileInfo) {
        super(context);
        this.mSize = tileInfo.getExactSize();
        mBounds = new Rect(0, 0, mSize.getWidth(), mSize.getHeight());
        this.mTileInfo = tileInfo;
    }

    void setBitmap(TileInfo tileId, Bitmap bitmap) {
        Preconditions.checkArgument(Objects.equals(tileId, this.mTileInfo),
                String.format("Got wrong tileId %s : %s", this.mTileInfo, tileId));
        if (this.mBitmap != null) {
            ErrorLog.log(getLogTag(), "Used tile receiving new bitmap " + tileId);
        }
        this.mBitmap = bitmap;
        // This View is already properly laid out, but without this requestLayout, it doesn't draw.
        requestLayout();
        invalidate();
    }

    void reset() {
        mBitmap = null;
    }

    public Point getOffset() {
        return mTileInfo != null ? mTileInfo.getOffset() : ZERO;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (mBitmap != null) {
            canvas.drawBitmap(mBitmap, IDENTITY, null);
        }
    }

    @Override
    public boolean isOpaque() {
        return true;
    }

    String getLogTag() {
        return mTileInfo != null ? mTileInfo.toString() : "TileView - empty";
    }
}

