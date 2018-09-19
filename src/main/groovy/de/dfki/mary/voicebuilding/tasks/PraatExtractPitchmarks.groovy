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

class PraatExtractPitchmarks extends DefaultTask {

    final WorkerExecutor workerExecutor

    @InputFile
    final RegularFileProperty scriptFile = newInputFile()

    @InputDirectory
    final DirectoryProperty wavDir = newInputDirectory()

    @InputDirectory
    final DirectoryProperty pitchDir = newInputDirectory()

    @OutputDirectory
    final DirectoryProperty destDir = newOutputDirectory()

    @Inject
    PraatExtractPitchmarks(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void extract() {
        def cmd = [project.praat.binary, '--run', scriptFile.get().asFile]
        project.fileTree(wavDir).include('*.wav').each { wavFile ->
            def basename = wavFile.name - '.wav'
            def pitchFile = pitchDir.file("${basename}.Pitch").get().asFile
            def destFile = destDir.file("${basename}.PointProcess").get().asFile
            workerExecutor.submit(RunnableExec.class) { WorkerConfiguration config ->
                def args = [wavFile,
                            pitchFile,
                            destFile]
                def commandLine = [commandLine: cmd + args]
                config.params commandLine
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
