/*
 * Copyright 2020 The Android Open Source Project
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

package android.graphics

class Path {
    val skijaPath = org.jetbrains.skija.Path()

    enum class Direction(val skija: org.jetbrains.skija.PathDirection) {
        @JvmStatic
        CW(org.jetbrains.skija.PathDirection.CLOCKWISE),
        @JvmStatic
        CCW(org.jetbrains.skija.PathDirection.COUNTER_CLOCKWISE)
    }

    fun addRoundRect(
        left: Float,
        top: Float,
        right: Float,
        bottom: Float,
        rx: Float,
        ry: Float,
        dir: Direction
    ) {
        skijaPath.addRRect(org.jetbrains.skija.RRect.makeLTRB(
            left, top, right, bottom, rx, ry), dir.skija)
    }

    fun reset() {
        skijaPath.reset()
    }

    fun addRoundRect(
        rect: RectF,
        radii: FloatArray,
        dir: Direction
    ) {
        skijaPath.addRRect(org.jetbrains.skija.RRect.makeComplexLTRB(
            rect.left, rect.top, rect.right, rect.bottom, radii), dir.skija)
    }

    fun isConvex(): Boolean = skijaPath.isConvex
    fun addPath(src: Path, dx: Float, dy: Float) {
        skijaPath.addPath(src.skijaPath, dx, dy)
    }

    fun transform(matrix: Matrix) {
    }
}
