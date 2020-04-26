package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class PraatExtractPitchmarks extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputFile
    final RegularFileProperty scriptFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty wavDir = project.objects.directoryProperty()

    @InputDirectory
    final DirectoryProperty pitchDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @Inject
    PraatExtractPitchmarks(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void extract() {
        def workQueue = workerExecutor.processIsolation()
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def wavFile = wavDir.file("${basename}.wav").get().asFile
            def pitchFile = pitchDir.file("${basename}.Pitch").get().asFile
            def destFile = destDir.file("${basename}.PointProcess").get().asFile
            workQueue.submit(RunnableExec.class) { parameters ->
                parameters.commandLine = [
                        project.praat.binary,
                        '--run', scriptFile.get().asFile,
                        wavFile,
                        pitchFile,
                        destFile
                ].collect { it.toString() }
            }
        }
    }
}
