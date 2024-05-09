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

@file:Suppress("UnstableApiUsage")

package androidx.compose.animation.lint

import androidx.compose.lint.Name
import androidx.compose.lint.Names
import androidx.compose.lint.UnreferencedParameter
import androidx.compose.lint.findUnreferencedParameters
import androidx.compose.lint.inheritsFrom
import androidx.compose.lint.isComposable
import androidx.compose.lint.isInPackageName
import androidx.compose.lint.returnsUnit
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.UastLintUtils.Companion.tryResolveUDeclaration
import com.intellij.psi.PsiMethod
import java.util.EnumSet
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UBlockExpression
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.ULambdaExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UReturnExpression
import org.jetbrains.uast.getParameterForArgument

private const val MODIFIER_PARAMETER_NAME = "modifier"

/** Detector to highlight unused provided Modifier from SharedTransitionContent. */
class SharedTransitionScopeDetector : Detector(), SourceCodeScanner {
    override fun getApplicableMethodNames(): List<String> = listOf(SharedTransitionScope.shortName)

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        if (method.isInPackageName(Names.Animation.PackageName)) {
            // Only one argument expected for `SharedTransitionScope(...)`, the content lambda.
            val lambdaArgument = node.getArgumentForParameter(0) as? ULambdaExpression ?: return

            lambdaArgument.findUnreferencedParameters().forEach { unreferencedParameter ->
                val location =
                    unreferencedParameter.parameter?.let { context.getLocation(it) }
                        ?: context.getLocation(lambdaArgument)

                // Find a Composable call proper for a quickfix
                val fixCandidate =
                    (lambdaArgument.body as? UBlockExpression)?.let {
                        findCompatibleComposableCall(it)
                    }

                val quickFix =
                    fixCandidate?.let {
                        buildQuickFix(
                            context = context,
                            node = fixCandidate,
                            unusedModifierParameter = unreferencedParameter
                        )
                    }

                context.report(
                    issue = UnusedSharedTransitionModifierParameter,
                    scope = node,
                    location = location,
                    message =
                        "Supplied Modifier parameter should be used on the top most " +
                            "Composable. Otherwise, consider using `SharedTransitionLayout`.",
                    quickfixData = quickFix
                )
            }
        }
    }

    /**
     * Looks for a compatible Composable call that can be used to provide a quick fix.
     *
     * Here we look for a Composable function call that it's most likely to be a Layout. Meaning,
     * that it has to be an emitter Composable: first letter is uppercase (by convention), has Unit
     * return type, and takes in a Modifier parameter. To stay on the safer side of assumptions, we
     * explicitly expect the modifier parameter to be called `modifier`.
     *
     * We also only return a result if there's only one such Composable.
     */
    private fun findCompatibleComposableCall(block: UBlockExpression): UCallExpression? {
        // TODO: Suggest SharedTransitionLayout when there's multiple Layout Composables at
        //  the top level
        var composableEmitterCount = 0
        var composableLayoutCount = 0
        var lastComposableLayoutCall: UCallExpression? = null

        // Look for Call expressions within the content lambda block (not recursive, only looks at
        // the top level)
        block.expressions
            .asSequence()
            .mapNotNull { node ->
                when (node) {
                    is UCallExpression -> {
                        node
                    }
                    is UReturnExpression -> {
                        // Lambda block content is sometimes wrapped in a `UReturnExpression` when
                        // there's only one expression.
                        node.returnExpression as? UCallExpression
                    }
                    else -> {
                        null
                    }
                }
            }
            .forEach { node ->
                // Note that resolving here is not fool-proof. Due to overloading, it's possible
                // that
                // a method may exist with a modifier parameter but not being able to resolve to it.
                val resolvedUMethod = node.tryResolveUDeclaration() as? UMethod
                val isFirstUpperCaseName = resolvedUMethod?.name?.first()?.isUpperCase() == true
                val isReturnsUnit = resolvedUMethod?.returnsUnit == true
                val isComposable = resolvedUMethod?.isComposable == true
                if (isComposable && isReturnsUnit && isFirstUpperCaseName) {
                    composableEmitterCount++
                    val modifierParameter =
                        resolvedUMethod?.uastParameters?.firstOrNull { parameter ->
                            val isModifierType =
                                parameter.sourcePsi is KtParameter &&
                                    parameter.type.inheritsFrom(Names.Ui.Modifier)
                            val hasModifierName = parameter.name == MODIFIER_PARAMETER_NAME

                            return@firstOrNull isModifierType && hasModifierName
                        }
                    if (modifierParameter != null) {
                        composableLayoutCount++
                        lastComposableLayoutCall = node
                    }
                }
            }

        // Stay risk-averse, only return the expression reference if it's also the only Emitter
        // Composable.
        if (composableLayoutCount == 1 && composableEmitterCount == 1) {
            return lastComposableLayoutCall
        } else {
            return null
        }
    }

    /**
     * Returns a quickfix, that changes the compatible Composable call expression to take in the
     * unused Modifier parameter from `SharedTransitionScope`.
     *
     * Note that at this point, [node] is expected to resolve to a Composable method that takes in a
     * `modifier: Modifier` parameter.
     *
     * So, if the original call expression doesn't have any modifier expression, we know that we can
     * simply assign the unused modifier parameter to it. E.g.: `MyComposable(modifier = it)`. If it
     * did already have a modifier expression on it. We can just wrap around it with `.then()`.
     */
    private fun buildQuickFix(
        context: JavaContext,
        node: UCallExpression,
        unusedModifierParameter: UnreferencedParameter
    ): LintFix? {
        val callName = node.methodName ?: return null

        // We'll  use this to rebuild the entire call expression, using named parameters to make
        // sure it can still resolve to the expected method.
        val callBuilder = StringBuilder().append(callName).append('(')

        val callArgs = node.valueArguments
        var callContainsModifier = false

        if (callArgs.isEmpty()) {
            callBuilder.append("$MODIFIER_PARAMETER_NAME = ${unusedModifierParameter.name})")
        } else {
            // Rebuild call using named parameters
            callArgs.forEach { argumentExpression ->
                val parameter = node.getParameterForArgument(argumentExpression)
                val expressionText = argumentExpression.sourcePsi?.text
                val parameterName = parameter?.name

                if (parameterName == MODIFIER_PARAMETER_NAME) {
                    callContainsModifier = true
                    if (expressionText != null) {
                        callBuilder.append(
                            "$parameterName = ${unusedModifierParameter.name}" +
                                ".then($expressionText)"
                        )
                    }
                } else {
                    if (parameterName != null && expressionText != null) {
                        callBuilder.append("$parameterName = $expressionText,\n")
                    }
                }
            }

            if (!callContainsModifier) {
                callBuilder.append("$MODIFIER_PARAMETER_NAME = ${unusedModifierParameter.name}\n")
            }
            callBuilder.append(')')
        }

        val fixText = callBuilder.toString()

        return LintFix.create()
            .replace()
            .name("Apply `SharedTransitionScope`'s Modifier to top-most Layout Composable.")
            .range(context.getLocation(node))
            .all()
            .with(fixText)
            .autoFix()
            .reformat(true)
            .build()
    }

    companion object {
        val UnusedSharedTransitionModifierParameter =
            Issue.create(
                id = "UnusedSharedTransitionModifierParameter",
                briefDescription =
                    "SharedTransitionScope calls should use the provided Modifier " + "parameter.",
                explanation =
                    "When using `SharedTransitionScope` the provided `Modifier` should " +
                        "always be used on the top-most child, as the `Modifier` both obtains " +
                        "the root coordinates and creates an overlay. Otherwise, consider using " +
                        "`SharedTransitionLayout`.",
                category = Category.CORRECTNESS,
                priority = 3,
                severity = Severity.ERROR,
                implementation =
                    Implementation(
                        SharedTransitionScopeDetector::class.java,
                        EnumSet.of(Scope.JAVA_FILE, Scope.TEST_SOURCES)
                    )
            )
    }
}

private val SharedTransitionScope = Name(Names.Animation.PackageName, "SharedTransitionScope")
