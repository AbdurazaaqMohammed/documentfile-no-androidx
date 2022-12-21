/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.window.demo.embedding

import android.content.Context
import androidx.startup.Initializer
import androidx.window.demo.R
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.SUFFIX_AND_FULLSCREEN_IN_BOOK_MODE
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.SUFFIX_REVERSED
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_SHOW_FULLSCREEN_IN_PORTRAIT
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_SHOW_LAYOUT_FOLLOWING_HINGE_WHEN_SEPARATING
import androidx.window.demo.embedding.SplitDeviceStateActivityBase.Companion.TAG_USE_DEFAULT_SPLIT_ATTRIBUTES
import androidx.window.embedding.RuleController
import androidx.window.embedding.SplitAttributes
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.BOTTOM_TO_TOP
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.LEFT_TO_RIGHT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.RIGHT_TO_LEFT
import androidx.window.embedding.SplitAttributes.LayoutDirection.Companion.TOP_TO_BOTTOM
import androidx.window.embedding.SplitAttributesCalculatorParams
import androidx.window.embedding.SplitController
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowLayoutInfo
import androidx.window.layout.WindowMetrics

/**
 * Initializes SplitController with a set of statically defined rules.
 */
class ExampleWindowInitializer : Initializer<RuleController> {
    private val mDemoActivityEmbeddingController = DemoActivityEmbeddingController.getInstance()

    override fun create(context: Context): RuleController {
        SplitController.getInstance(context).apply {
            if (isSplitAttributesCalculatorSupported()) {
                setSplitAttributesCalculator(::sampleSplitAttributesCalculator)
            }
        }
        return RuleController.getInstance(context).apply {
            if (SplitController.getInstance(context).isSplitSupported()) {
                setRules(RuleController.parseRules(context, R.xml.main_split_config))
            }
        }
    }

    /**
     * A sample callback set in [SplitController.setSplitAttributesCalculator] to demonstrate how to
     * change the [SplitAttributes] with the current device and window state and
     * [SplitAttributesCalculatorParams.splitRuleTag].
     */
    private fun sampleSplitAttributesCalculator(
        params: SplitAttributesCalculatorParams
    ): SplitAttributes {
        val isPortrait = params.parentWindowMetrics.isPortrait()
        val windowLayoutInfo = params.parentWindowLayoutInfo
        val isTabletop = windowLayoutInfo.isTabletop()
        val isBookMode = windowLayoutInfo.isBookMode()
        val config = params.parentConfiguration
        // The SplitAttributes to occupy the whole task bounds
        val expandContainersAttrs = SplitAttributes.Builder()
            .setSplitType(SplitAttributes.SplitType.expandContainers())
            .build()
        val tag = params.splitRuleTag
        val shouldReversed = tag?.contains(SUFFIX_REVERSED) ?: false
        // Make a copy of the default splitAttributes, but replace the animation background
        // color to what is configured in the Demo app.
        val backgroundColor = mDemoActivityEmbeddingController.animationBackgroundColor
        val defaultSplitAttributes = SplitAttributes.Builder()
            .setLayoutDirection(params.defaultSplitAttributes.layoutDirection)
            .setSplitType(params.defaultSplitAttributes.splitType)
            .setAnimationBackgroundColor(backgroundColor)
            .build()
        when (tag?.substringBefore(SUFFIX_REVERSED)) {
            TAG_USE_DEFAULT_SPLIT_ATTRIBUTES, null -> {
                return if (params.areDefaultConstraintsSatisfied) {
                    defaultSplitAttributes
                } else {
                    expandContainersAttrs
                }
            }
            TAG_SHOW_FULLSCREEN_IN_PORTRAIT -> {
                if (isPortrait) {
                    return expandContainersAttrs
                }
            }
            TAG_SHOW_FULLSCREEN_IN_PORTRAIT + SUFFIX_AND_HORIZONTAL_LAYOUT_IN_TABLETOP -> {
                if (isTabletop) {
                    return SplitAttributes.Builder()
                        .setSplitType(SplitAttributes.SplitType.splitByHinge())
                        .setLayoutDirection(
                            if (shouldReversed) {
                                BOTTOM_TO_TOP
                            } else {
                                TOP_TO_BOTTOM
                            }
                        )
                        .setAnimationBackgroundColor(backgroundColor)
                        .build()
                } else if (isPortrait) {
                    return expandContainersAttrs
                }
            }
            TAG_SHOW_HORIZONTAL_LAYOUT_IN_TABLETOP -> {
                if (isTabletop) {
                    return SplitAttributes.Builder()
                        .setSplitType(SplitAttributes.SplitType.splitByHinge())
                        .setLayoutDirection(
                            if (shouldReversed) {
                                BOTTOM_TO_TOP
                            } else {
                                TOP_TO_BOTTOM
                            }
                        )
                        .setAnimationBackgroundColor(backgroundColor)
                        .build()
                }
            }
            TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE -> {
                return SplitAttributes.Builder()
                    .setSplitType(SplitAttributes.SplitType.splitByHinge())
                    .setLayoutDirection(
                        if (shouldReversed) {
                            BOTTOM_TO_TOP
                        } else {
                            TOP_TO_BOTTOM
                        }
                    ).build()
            }
            TAG_SHOW_DIFFERENT_LAYOUT_WITH_SIZE + SUFFIX_AND_FULLSCREEN_IN_BOOK_MODE -> {
                return if (isBookMode) {
                    expandContainersAttrs
                } else if (config.screenWidthDp <= 600) {
                    SplitAttributes.Builder()
                        .setSplitType(SplitAttributes.SplitType.splitEqually())
                        .setLayoutDirection(
                            if (shouldReversed) {
                                BOTTOM_TO_TOP
                            } else {
                                TOP_TO_BOTTOM
                            }
                        )
                        .setAnimationBackgroundColor(backgroundColor)
                        .build()
                } else {
                    SplitAttributes.Builder()
                        .setSplitType(SplitAttributes.SplitType.splitEqually())
                        .setLayoutDirection(
                            if (shouldReversed) {
                                RIGHT_TO_LEFT
                            } else {
                                LEFT_TO_RIGHT
                            }
                        )
                        .setAnimationBackgroundColor(backgroundColor)
                        .build()
                }
            }
            TAG_SHOW_LAYOUT_FOLLOWING_HINGE_WHEN_SEPARATING -> {
                val foldingState = windowLayoutInfo.getFoldingFeature()
                if (foldingState != null) {
                    return SplitAttributes.Builder()
                        .setSplitType(
                            if (foldingState.isSeparating) {
                                SplitAttributes.SplitType.splitByHinge()
                            } else {
                                SplitAttributes.SplitType.ratio(0.3f)
                            }
                        ).setLayoutDirection(
                            if (
                                foldingState.orientation
                                    == FoldingFeature.Orientation.HORIZONTAL
                            ) {
                                if (shouldReversed) BOTTOM_TO_TOP else TOP_TO_BOTTOM
                            } else {
                                if (shouldReversed) RIGHT_TO_LEFT else LEFT_TO_RIGHT
                            }
                        )
                        .setAnimationBackgroundColor(backgroundColor)
                        .build()
                }
            }
        }
        return defaultSplitAttributes
    }

    private fun WindowMetrics.isPortrait(): Boolean =
        bounds.height() > bounds.width()

    private fun WindowLayoutInfo.isTabletop(): Boolean {
        val foldingFeature = getFoldingFeature()
        return foldingFeature?.state == FoldingFeature.State.HALF_OPENED &&
            foldingFeature.orientation == FoldingFeature.Orientation.HORIZONTAL
    }

    private fun WindowLayoutInfo.isBookMode(): Boolean {
        val foldingFeature = getFoldingFeature()
        return foldingFeature?.state == FoldingFeature.State.HALF_OPENED &&
            foldingFeature.orientation == FoldingFeature.Orientation.VERTICAL
    }

    /**
     * Returns the [FoldingFeature] if it is exactly the only [FoldingFeature] in
     * [WindowLayoutInfo]. Otherwise, returns `null`.
     */
    private fun WindowLayoutInfo.getFoldingFeature(): FoldingFeature? {
        val foldingFeatures = displayFeatures.filterIsInstance<FoldingFeature>()
        return if (foldingFeatures.size == 1) foldingFeatures[0] else null
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}