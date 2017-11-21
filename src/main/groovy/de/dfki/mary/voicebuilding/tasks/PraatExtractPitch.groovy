package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import org.gradle.workers.*

import javax.inject.Inject

class PraatExtractPitch extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputFile
    File scriptFile

    @InputFiles
    FileCollection srcFiles

    @OutputDirectory
    File destDir

    @Inject
    PraatExtractPitch(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void extract() {
        def cmd = [project.praat.binary, scriptFile]
        srcFiles.each { wavFile ->
            def basename = wavFile.name - '.wav'
            def destFile = project.file("$destDir/${basename}.Pitch")
            workerExecutor.submit(RunnableExec.class) { WorkerConfiguration config ->
                def args = [wavFile,
                            destFile,
                            (project.voice.gender == 'female') ? 100 : 75,
                            (project.voice.gender == 'female') ? 500 : 300]
                def commandLine = [commandLine: cmd + args]
                config.params commandLine
                config.isolationMode = IsolationMode.PROCESS
            }
        }
    }
}
