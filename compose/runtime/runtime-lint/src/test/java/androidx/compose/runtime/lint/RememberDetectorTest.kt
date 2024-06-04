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

@file:Suppress("UnstableApiUsage")

package androidx.compose.runtime.lint

import androidx.compose.lint.test.Stubs
import com.android.tools.lint.checks.infrastructure.LintDetectorTest
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Issue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)

/** Test for [RememberDetector]. */
class RememberDetectorTest : LintDetectorTest() {
    override fun getDetector(): Detector = RememberDetector()

    override fun getIssues(): MutableList<Issue> =
        mutableListOf(RememberDetector.RememberReturnType)

    @Test
    fun returnsUnit() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                class FooState {
                    fun update(new: Int) {}
                }

                @Composable
                fun Test() {
                    val state = remember { FooState() }
                    remember {
                        state.update(5)
                    }
                    val unit = remember {
                        state.update(5)
                    }
                    val unitLambda: () -> Unit = {}
                    remember(unitLambda)
                    val unit2 = remember(unitLambda)
                }

                @Composable
                fun Test(number: Int) {
                    val state = remember { FooState() }
                    remember(number) {
                        state.update(number)
                    }
                    val unit = remember(number) {
                        state.update(number)
                    }
                    val unitLambda: () -> Unit = {}
                    remember(number, unitLambda)
                    val unit2 = remember(number, unitLambda)
                }

                @Composable
                fun Test(number1: Int, number2: Int) {
                    val state = remember { FooState() }
                    remember(number1, number2) {
                        state.update(number1)
                        state.update(number2)
                    }
                    val unit = remember(number1, number2) {
                        state.update(number1)
                        state.update(number2)
                    }
                    val unitLambda: () -> Unit = {}
                    remember(number1, number2, unitLambda)
                    val unit2 = remember(number1, number2, unitLambda)
                }

                @Composable
                fun Test(number1: Int, number2: Int, number3: Int) {
                    val state = remember { FooState() }
                    remember(number1, number2, number3) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                    val unit = remember(number1, number2, number3) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                    val unitLambda: () -> Unit = {}
                    remember(number1, number2, number3, unitLambda)
                    val unit2 = remember(number1, number2, number3, unitLambda)
                }

                @Composable
                fun Test(number1: Int, number2: Int, number3: Int, flag: Boolean) {
                    val state = remember { FooState() }
                    remember(number1, number2, number3, flag) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                    val unit = remember(number1, number2, number3, flag) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                    val unitLambda: () -> Unit = {}
                    remember(number1, number2, number3, flag, calculation = unitLambda)
                    val unit2 = remember(number1, number2, number3, flag, calculation = unitLambda)
                }
            """
                ),
                Stubs.Composable,
                Stubs.Remember
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/FooState.kt:14: Error: remember calls must not return Unit [RememberReturnType]
                    remember {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:17: Error: remember calls must not return Unit [RememberReturnType]
                    val unit = remember {
                               ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:21: Error: remember calls must not return Unit [RememberReturnType]
                    remember(unitLambda)
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:22: Error: remember calls must not return Unit [RememberReturnType]
                    val unit2 = remember(unitLambda)
                                ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:28: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:31: Error: remember calls must not return Unit [RememberReturnType]
                    val unit = remember(number) {
                               ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:35: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number, unitLambda)
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:36: Error: remember calls must not return Unit [RememberReturnType]
                    val unit2 = remember(number, unitLambda)
                                ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:42: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number1, number2) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:46: Error: remember calls must not return Unit [RememberReturnType]
                    val unit = remember(number1, number2) {
                               ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:51: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number1, number2, unitLambda)
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:52: Error: remember calls must not return Unit [RememberReturnType]
                    val unit2 = remember(number1, number2, unitLambda)
                                ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:58: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number1, number2, number3) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:63: Error: remember calls must not return Unit [RememberReturnType]
                    val unit = remember(number1, number2, number3) {
                               ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:69: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number1, number2, number3, unitLambda)
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:70: Error: remember calls must not return Unit [RememberReturnType]
                    val unit2 = remember(number1, number2, number3, unitLambda)
                                ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:76: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number1, number2, number3, flag) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:81: Error: remember calls must not return Unit [RememberReturnType]
                    val unit = remember(number1, number2, number3, flag) {
                               ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:87: Error: remember calls must not return Unit [RememberReturnType]
                    remember(number1, number2, number3, flag, calculation = unitLambda)
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:88: Error: remember calls must not return Unit [RememberReturnType]
                    val unit2 = remember(number1, number2, number3, flag, calculation = unitLambda)
                                ~~~~~~~~
20 errors, 0 warnings
            """
            )
    }

    @Test
    fun returnsUnit_dueToTypeError() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                @Composable
                fun Test() {
                    val shouldBeError = remember { Unknown() }
                    val stillError = remember {
                        val local = Unknown()
                        local
                    }
                    val shouldBeInt = remember { 42 }
                    val stillInt = remember {
                        val local = Unknown()
                        42
                    }
                }
                """
                ),
                Stubs.Composable,
                Stubs.Remember
            )
            .run()
            .expectClean()
    }

    @Test
    fun returnsValue_explicitUnitType() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                class FooState {
                    fun update(new: Int): Boolean = true
                }

                @Composable
                fun Test() {
                    val state = remember { FooState() }
                    remember<Unit> {
                        state.update(5)
                    }
                    val result = remember<Unit> {
                        state.update(5)
                    }
                }

                @Composable
                fun Test(number: Int) {
                    val state = remember { FooState() }
                    remember<Unit>(number) {
                        state.update(number)
                    }
                    val result = remember<Unit>(number) {
                        state.update(number)
                    }
                }

                @Composable
                fun Test(number1: Int, number2: Int) {
                    val state = remember { FooState() }
                    remember<Unit>(number1, number2) {
                        state.update(number1)
                        state.update(number2)
                    }
                    val result = remember<Unit>(number1, number2) {
                        state.update(number1)
                        state.update(number2)
                    }
                }

                @Composable
                fun Test(number1: Int, number2: Int, number3: Int) {
                    val state = remember { FooState() }
                    remember<Unit>(number1, number2, number3) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                    val result = remember<Unit>(number1, number2, number3) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                }

                @Composable
                fun Test(number1: Int, number2: Int, number3: Int, flag: Boolean) {
                    val state = remember { FooState() }
                    remember<Unit>(number1, number2, number3, flag) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                    val result = remember<Unit>(number1, number2, number3, flag) {
                        state.update(number1)
                        state.update(number2)
                        state.update(number3)
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.Remember
            )
            .run()
            .expect(
                """
src/androidx/compose/runtime/foo/FooState.kt:14: Error: remember calls must not return Unit [RememberReturnType]
                    remember<Unit> {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:17: Error: remember calls must not return Unit [RememberReturnType]
                    val result = remember<Unit> {
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:25: Error: remember calls must not return Unit [RememberReturnType]
                    remember<Unit>(number) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:28: Error: remember calls must not return Unit [RememberReturnType]
                    val result = remember<Unit>(number) {
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:36: Error: remember calls must not return Unit [RememberReturnType]
                    remember<Unit>(number1, number2) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:40: Error: remember calls must not return Unit [RememberReturnType]
                    val result = remember<Unit>(number1, number2) {
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:49: Error: remember calls must not return Unit [RememberReturnType]
                    remember<Unit>(number1, number2, number3) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:54: Error: remember calls must not return Unit [RememberReturnType]
                    val result = remember<Unit>(number1, number2, number3) {
                                 ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:64: Error: remember calls must not return Unit [RememberReturnType]
                    remember<Unit>(number1, number2, number3, flag) {
                    ~~~~~~~~
src/androidx/compose/runtime/foo/FooState.kt:69: Error: remember calls must not return Unit [RememberReturnType]
                    val result = remember<Unit>(number1, number2, number3, flag) {
                                 ~~~~~~~~
10 errors, 0 warnings
            """
            )
    }

    @Test
    fun noErrors() {
        lint()
            .files(
                kotlin(
                    """
                package androidx.compose.runtime.foo

                import androidx.compose.runtime.Composable
                import androidx.compose.runtime.remember

                class FooState {
                    fun update(new: Int): Boolean = true
                }

                @Composable
                fun Test() {
                    val state = remember { FooState() }
                    remember {
                        state.update(5)
                    }
                    val result = remember {
                        state.update(5)
                    }
                }

                @Composable
                fun Test(number: Int) {
                    val state = remember { FooState() }
                    remember(number) {
                        state.update(number)
                    }
                    val result = remember(number) {
                        state.update(number)
                    }
                }

                @Composable
                fun Test(number1: Int, number2: Int) {
                    val state = remember { FooState() }
                    remember(number1, number2) {
                        state.update(number)
                    }
                    val result = remember(number1, number2) {
                        state.update(number)
                    }
                }

                @Composable
                fun Test(number1: Int, number2: Int, number3: Int) {
                    val state = remember { FooState() }
                    remember(number1, number2, number3) {
                        state.update(number)
                    }
                    val result = remember(number1, number2, number3) {
                        state.update(number)
                    }
                }

                @Composable
                fun Test(number1: Int, number2: Int, number3: Int, flag: Boolean) {
                    val state = remember { FooState() }
                    remember(number1, number2, number3, flag) {
                        state.update(number)
                    }
                    val result = remember(number1, number2, number3, flag) {
                        state.update(number)
                    }
                }
            """
                ),
                Stubs.Composable,
                Stubs.Remember
            )
            .run()
            .expectClean()
    }
}
