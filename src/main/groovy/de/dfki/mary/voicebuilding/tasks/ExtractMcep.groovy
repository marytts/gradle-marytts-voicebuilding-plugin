package de.dfki.mary.voicebuilding.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.workers.WorkerExecutor

import javax.inject.Inject

class ExtractMcep extends DefaultTask {

    @Internal
    final WorkerExecutor workerExecutor

    @InputFile
    final RegularFileProperty basenamesFile = project.objects.fileProperty()

    @InputDirectory
    final DirectoryProperty wavDir = project.objects.directoryProperty()

    @InputDirectory
    final DirectoryProperty pmDir = project.objects.directoryProperty()

    @OutputDirectory
    final DirectoryProperty destDir = project.objects.directoryProperty()

    @Inject
    ExtractMcep(WorkerExecutor workerExecutor) {
        this.workerExecutor = workerExecutor
    }

    @TaskAction
    void process() {
        def sig2FvPath = System.env['PATH'].split(':').collect { dir ->
            new File(dir, 'sig2fv')
        }.find { it.exists() }
        assert sig2FvPath
        def workQueue = workerExecutor.processIsolation()
        basenamesFile.get().asFile.eachLine('UTF-8') { basename ->
            def wavFile = wavDir.file("${basename}.wav").get().asFile
            def pmFile = pmDir.file("${basename}.pm").get().asFile
            def destFile = destDir.file("${basename}.mcep").get().asFile
            workQueue.submit(RunnableExec.class) { parameters ->
                parameters.commandLine = [
                        sig2FvPath,
                        '-window_type', 'hamming',
                        '-factor', 2.5,
                        '-otype', 'est_binary',
                        '-coefs', 'melcep',
                        '-melcep_order', 12,
                        '-fbank_order', 24,
                        '-shift', 0.01,
                        '-preemph', 0.97,
                        '-pm', pmFile,
                        '-o', destFile,
                        wavFile
                ].collect { it.toString() }
            }
        }
    }
}
