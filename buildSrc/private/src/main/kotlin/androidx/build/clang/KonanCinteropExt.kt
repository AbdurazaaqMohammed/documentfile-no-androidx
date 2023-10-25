/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.clang

import com.android.utils.appendCapitalized
import org.gradle.kotlin.dsl.get
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * Configures a CInterop for the given [kotlinNativeTarget].
 * The cinterop will be based on the [cinteropName] in the project sources but will additionally
 * include the references to the library archive from the [ClangArchiveTask] so that it can be
 * embedded in the generated klib of the cinterop.
 */
internal fun MultiTargetNativeCompilation.configureCinterop(
    kotlinNativeTarget: KotlinNativeTarget,
    cinteropName: String = archiveName,
) {
    if (!canCompileOnCurrentHost(kotlinNativeTarget.konanTarget)) {
        return
    }
    val konanTarget = kotlinNativeTarget.konanTarget
    val nativeTargetCompilation = targetProvider(konanTarget)
    val taskNamePrefix = "androidXCinterop".appendCapitalized(
        kotlinNativeTarget.name,
        archiveName
    )
    val createDefFileTask = project.tasks.register(
        taskNamePrefix.appendCapitalized(
            "createDefFileFor", konanTarget.name
        ), CreateDefFileWithLibraryPathTask::class.java
    ) { task ->
        task.objectFile.set(
            nativeTargetCompilation.flatMap {
                it.archiveTask
            }.flatMap {
                it.llvmArchiveParameters.outputFile
            }
        )
        task.target.set(
            project.layout.buildDirectory.file(
                "cinteropDefFiles/$taskNamePrefix/${konanTarget.name}/$cinteropName.def"
            )
        )
        task.original.set(
            project.layout.projectDirectory.file(
                "src/nativeInterop/cinterop/$cinteropName.def"
            )
        )
        task.projectDir.set(
            project.layout.projectDirectory
        )
    }
    (kotlinNativeTarget.compilations[
        KotlinCompilation.MAIN_COMPILATION_NAME
    ] as KotlinNativeCompilation).cinterops.register(
        cinteropName
    ) { cInteropSettings ->

        cInteropSettings.defFileProperty.set(createDefFileTask.flatMap { it.target.asFile })
        cInteropSettings.includeDirs(nativeTargetCompilation.flatMap {
            it.compileTask
        }.map {
            it.clangParameters.includes
        })
        // TODO KT-62795 We shouldn't need this dependency once that issue is fixed.
        project.tasks.named(cInteropSettings.interopProcessingTaskName).configure {
            it.dependsOn(createDefFileTask)
        }
    }
}
