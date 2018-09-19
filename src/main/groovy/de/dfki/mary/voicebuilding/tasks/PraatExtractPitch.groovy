package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.workers.IsolationMode
import org.gradle.workers.WorkerConfiguration
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class PraatExtractPitch extends DefaultTask {

    final WorkerExecutor workerExecutor

    @InputFile
    final RegularFileProperty scriptFile = newInputFile()

    @InputDirectory
    final DirectoryProperty srcDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @Inject
    PraatExtractPitch(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void extract() {
        def cmd = [project.praat.binary, '--run', scriptFile.get().asFile]
        project.fileTree(srcDir).include('**/*.wav').each { wavFile ->
            def basename = wavFile.name - '.wav'
            def destFile = destDir.file("${basename}.Pitch").get().asFile
            workerExecutor.submit(RunnableExec.class) { WorkerConfiguration config ->
                def args = [wavFile,
                            destFile,
                            (project.marytts.voice.gender == 'female') ? 100 : 75,
                            (project.marytts.voice.gender == 'female') ? 500 : 300]
                def commandLine = [commandLine: cmd + args]
                config.params commandLine
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
