/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.buildtools.api.tests.compilation.scenario

import org.jetbrains.kotlin.buildtools.api.CompilerExecutionStrategyConfiguration
import org.jetbrains.kotlin.buildtools.api.SourcesChanges
import org.jetbrains.kotlin.buildtools.api.jvm.IncrementalJvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.jvm.JvmCompilationConfiguration
import org.jetbrains.kotlin.buildtools.api.tests.compilation.assertions.assertOutputs
import org.jetbrains.kotlin.buildtools.api.tests.compilation.model.*
import java.nio.file.Files
import java.nio.file.Path
import java.util.*
import kotlin.io.path.*

internal class ScenarioModuleImpl(
    internal val module: Module,
    internal val outputs: MutableSet<String>,
    private val strategyConfig: CompilerExecutionStrategyConfiguration,
    private val compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
    private val incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
) : ScenarioModule {
    override fun changeFile(
        fileName: String,
        transform: (String) -> String,
    ) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(transform(file.readText()))
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles + file.toFile(),
            removedFiles = sourcesChanges.removedFiles,
        )
    }

    override fun deleteFile(fileName: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.deleteExisting()
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles,
            removedFiles = sourcesChanges.removedFiles + file.toFile(),
        )
    }

    override fun createFile(fileName: String, content: String) {
        val file = module.sourcesDirectory.resolve(fileName)
        file.writeText(content)
        sourcesChanges = SourcesChanges.Known(
            modifiedFiles = sourcesChanges.modifiedFiles + file.toFile(),
            removedFiles = sourcesChanges.removedFiles,
        )
    }

    private var sourcesChanges = SourcesChanges.Known(emptyList(), emptyList())

    override fun compile(
        forceOutput: LogLevel?,
        assertions: context(Module, ScenarioModule) CompilationOutcome.() -> Unit,
    ) {
        module.compileIncrementally(
            sourcesChanges,
            strategyConfig,
            forceOutput,
            compilationConfigAction = { compilationOptionsModifier?.invoke(it) },
            incrementalCompilationConfigAction = { incrementalCompilationOptionsModifier?.invoke(it) },
            assertions = {
                assertions(module, this@ScenarioModuleImpl, this)
            })
    }
}

private class ScenarioDsl(
    private val project: Project,
    private val strategyConfig: CompilerExecutionStrategyConfiguration,
) : Scenario {
    @Synchronized
    override fun module(
        moduleName: String,
        dependencies: List<ScenarioModule>,
        additionalCompilationArguments: List<String>,
        compilationOptionsModifier: ((JvmCompilationConfiguration) -> Unit)?,
        incrementalCompilationOptionsModifier: ((IncrementalJvmCompilationConfiguration<*>) -> Unit)?,
    ): ScenarioModule {
        val transformedDependencies = dependencies.map { (it as ScenarioModuleImpl).module }
        val module =
            project.module(moduleName, transformedDependencies, additionalCompilationArguments)
        return GlobalCompiledProjectsCache.getProjectFromCache(module, strategyConfig, compilationOptionsModifier, incrementalCompilationOptionsModifier)
            ?: GlobalCompiledProjectsCache.putProjectIntoCache(module, strategyConfig, compilationOptionsModifier, incrementalCompilationOptionsModifier)
    }
}

fun BaseCompilationTest.scenario(strategyConfig: CompilerExecutionStrategyConfiguration, action: Scenario.() -> Unit) {
    action(ScenarioDsl(Project(strategyConfig, workingDirectory), strategyConfig))
}