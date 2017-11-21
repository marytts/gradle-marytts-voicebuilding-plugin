package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.*

import javax.inject.Inject

class PraatExtractPitchmarks extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputFile
    File scriptFile

    @InputFiles
    FileCollection wavFiles

    @InputFiles
    FileCollection pitchFiles

    @OutputDirectory
    File destDir

    @Inject
    PraatExtractPitchmarks(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void extract() {
        def cmd = [project.praat.binary, '--run', scriptFile]
        wavFiles.each { wavFile ->
            def basename = wavFile.name - '.wav'
            def pitchFile = project.file("$project.praatPitchExtractor.destDir/${basename}.Pitch")
            def destFile = project.file("$destDir/${basename}.PointProcess")
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
